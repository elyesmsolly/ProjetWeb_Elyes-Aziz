package tn.supcom.dourek.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Appel à l'API Google Gemini (offre gratuite), utilisé quand il n'y a pas de clé Claude.
 * La clé vient de GEMINI_API_KEY (variable d'environnement ou fichier cle-ia.properties) : elle n'est jamais
 * écrite dans le code et reste sur le serveur (les visiteurs ne la voient pas).
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    /** Erreur renvoyée par l'API, avec son code HTTP. */
    static class ErreurGemini extends IllegalStateException {
        final int code;

        ErreurGemini(int code, String message) {
            super(message);
            this.code = code;
        }

        /** Modèle introuvable, quota atteint, surcharge ou délai dépassé : un autre modèle peut répondre. */
        boolean autreModelePossible() {
            return code == 404 || code == 408 || code == 429 || code >= 500;
        }
    }

    /** Modèles essayés d'office après celui de la configuration (alias officiels, toujours à jour). */
    private static final List<String> ALIAS = List.of("gemini-flash-lite-latest", "gemini-flash-latest");
    private static final int ESSAIS_MAX = 7;
    private static final Duration DELAI_PAR_ESSAI = Duration.ofSeconds(10);
    private static final Duration DELAI_TOTAL = Duration.ofSeconds(25);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json;
    private final String cle;
    private final String url;
    /** Modèle utilisé : celui de la configuration, remplacé par le premier qui répond s'il est indisponible. */
    private volatile String modele;

    public GeminiClient(ObjectMapper json,
                        @Value("${GEMINI_API_KEY:}") String cle,
                        @Value("${dourek.gemini.modele}") String modele,
                        @Value("${dourek.gemini.url}") String url) {
        this.json = json;
        this.cle = cle.trim();
        this.modele = modele.trim();
        this.url = url.trim();
    }

    public boolean actif() {
        return !cle.isBlank();
    }

    public String modele() {
        return modele;
    }

    /** Réponse JSON qui respecte le schéma donné (sortie structurée de Gemini). */
    public JsonNode json(String consigne, String message, Map<String, Object> schema) {
        String texte;
        try {
            texte = generer(consigne, message, Map.of("responseMimeType", "application/json", "responseSchema", schema));
        } catch (ErreurGemini ex) {
            // Une clé invalide donne aussi un code 400 : inutile de réessayer dans ce cas.
            if (ex.code != 400 || ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("api key")) throw ex;
            // Schéma refusé par ce modèle : on demande du JSON simple, le schéma étant décrit dans la consigne.
            log.warn("Gemini : schéma refusé ({}), nouvel essai en JSON simple", ex.getMessage());
            texte = generer(consigne + "\n\nRéponds uniquement par un objet JSON conforme à ce schéma :\n"
                    + json.writeValueAsString(schema), message, Map.of("responseMimeType", "application/json"));
        }
        return json.readTree(texte);
    }

    /** Réponse en texte libre. */
    public String texte(String consigne, String message) {
        return generer(consigne, message, Map.of()).trim();
    }

    /**
     * Les modèles gratuits changent de nom, saturent (503) ou épuisent leur quota (429) : on essaie le modèle
     * courant, puis les alias officiels, puis les modèles « flash » listés par l'API, et on garde le premier
     * qui répond pour les appels suivants.
     */
    private String generer(String consigne, String message, Map<String, Object> config) {
        Map<String, Object> corps = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", consigne))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", message)))),
                "generationConfig", config);
        String charge = json.writeValueAsString(corps);
        String courant = modele;
        List<String> essais = new ArrayList<>(List.of(courant));
        ALIAS.stream().filter(a -> !essais.contains(a)).forEach(essais::add);
        boolean listeLue = false;
        long debut = System.nanoTime();
        ErreurGemini derniere = null;
        for (int i = 0; ; i++) {
            if (i >= essais.size()) {
                if (listeLue) break;
                listeLue = true;
                modelesDisponibles().stream().filter(m -> !essais.contains(m)).limit(Math.max(0, ESSAIS_MAX - essais.size())).forEach(essais::add);
                if (i >= essais.size()) break;
            }
            if (derniere != null && Duration.ofNanos(System.nanoTime() - debut).compareTo(DELAI_TOTAL) > 0) break;
            String essai = essais.get(i);
            try {
                String texte = lire(appeler(essai, charge));
                if (!essai.equals(courant)) {
                    log.warn("Gemini : modèle « {} » indisponible ({}), utilisation de « {} »", courant,
                            derniere == null ? "" : derniere.getMessage(), essai);
                    modele = essai;
                }
                return texte;
            } catch (ErreurGemini ex) {
                if (!ex.autreModelePossible()) throw ex;
                derniere = ex;
            }
        }
        throw derniere == null ? new ErreurGemini(404, "aucun modèle disponible") : derniere;
    }

    private static String lire(JsonNode reponse) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode p : reponse.path("candidates").path(0).path("content").path("parts")) {
            // Les éventuelles parties « réflexion » du modèle ne font pas partie de la réponse.
            if (!p.path("thought").asBoolean(false)) sb.append(p.path("text").asString(""));
        }
        if (sb.isEmpty()) throw new IllegalStateException("réponse vide");
        return sb.toString();
    }

    private JsonNode appeler(String nomModele, String charge) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/v1beta/models/" + nomModele + ":generateContent"))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", cle)
                .timeout(DELAI_PAR_ESSAI)
                .POST(HttpRequest.BodyPublishers.ofString(charge))
                .build();
        return envoyer(req);
    }

    private JsonNode envoyer(HttpRequest req) {
        try {
            HttpResponse<String> rep = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (rep.statusCode() != 200) {
                String detail = rep.body() == null ? "" : rep.body();
                try {
                    String m = json.readTree(detail).path("error").path("message").asString("");
                    if (!m.isBlank()) detail = m;
                } catch (RuntimeException ignore) {
                    // corps non JSON : on garde le texte brut
                }
                throw new ErreurGemini(rep.statusCode(), "code " + rep.statusCode() + " : "
                        + (detail.length() > 200 ? detail.substring(0, 200) : detail));
            }
            return json.readTree(rep.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("appel interrompu");
        } catch (java.net.http.HttpTimeoutException ex) {
            throw new ErreurGemini(408, "délai dépassé");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Gemini injoignable : " + ex.getMessage());
        }
    }

    private static final Pattern VERSION = Pattern.compile("gemini-(\\d+(?:\\.\\d+)?)");
    private static final List<String> HORS_SUJET = List.of("image", "tts", "audio", "live", "embedding", "robotics",
            "computer", "learnlm", "aqa", "nano", "veo", "imagen", "omni");

    /** Modèles de texte accessibles avec cette clé, du préféré au moins bon (liste vide si l'API ne répond pas). */
    List<String> modelesDisponibles() {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/v1beta/models?pageSize=1000"))
                .header("x-goog-api-key", cle)
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        JsonNode modeles;
        try {
            modeles = envoyer(req).path("models");
        } catch (IllegalStateException ex) {
            log.warn("Gemini : liste des modèles indisponible ({})", ex.getMessage());
            return List.of();
        }
        List<String> noms = new ArrayList<>();
        for (JsonNode m : modeles) {
            String nom = m.path("name").asString("").replaceFirst("^models/", "");
            boolean genere = StreamSupport.stream(m.path("supportedGenerationMethods").spliterator(), false)
                    .anyMatch(x -> "generateContent".equals(x.asString("")));
            if (genere && nom.startsWith("gemini") && HORS_SUJET.stream().noneMatch(nom::contains)) noms.add(nom);
        }
        noms.sort(Comparator.comparingDouble(GeminiClient::note).reversed());
        return noms;
    }

    /**
     * Préférence : flash-lite (réponse en une seconde, quota gratuit le plus large) > flash > autres ;
     * à égalité, version stable puis numéro de version le plus élevé.
     */
    static double note(String nom) {
        double n = nom.contains("flash-lite") ? 300 : nom.contains("flash") ? 200 : 100;
        if (nom.contains("preview") || nom.contains("exp")) n -= 50;
        if (nom.endsWith("-latest")) n += 20;
        Matcher v = VERSION.matcher(nom);
        if (v.find()) n += Math.min(19.9, Double.parseDouble(v.group(1)));
        return n;
    }
}
