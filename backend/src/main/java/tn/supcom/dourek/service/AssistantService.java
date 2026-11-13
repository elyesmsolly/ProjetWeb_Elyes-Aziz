package tn.supcom.dourek.service;

import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.supcom.dourek.dto.Dtos.*;
import tn.supcom.dourek.model.Categorie;
import tools.jackson.databind.JsonNode;

/**
 * Assistant de recherche en langage naturel, en trois temps :
 * 1. le modèle de chat (API Claude) comprend la demande et en extrait des critères ;
 * 2. NOTRE algorithme (RechercheService) filtre et classe les établissements ;
 * 3. le modèle explique le choix en une ou deux phrases.
 * Modèle utilisé : Claude si ANTHROPIC_API_KEY est définie, sinon Google Gemini (offre gratuite) si
 * GEMINI_API_KEY est définie, sinon un analyseur simple prend le relais.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    /** Critères extraits par le modèle (sortie structurée : le schéma JSON est dérivé de ce record). */
    public record CriteresExtraits(
            @JsonPropertyDescription("Une valeur parmi BARBIER, COIFFURE_FEMME, LABORATOIRE, AGENCE, ADMINISTRATION, AUTRE, ou chaîne vide si inconnue")
            String categorie,
            @JsonPropertyDescription("Prestation demandée en français (ex. « Coupe homme », « Barbe », « Prise de sang »), ou chaîne vide")
            String prestation,
            @JsonPropertyDescription("Quartier ou ville mentionné (ex. « Ariana », « Ennasr »), ou chaîne vide")
            String quartier,
            @JsonPropertyDescription("Heure limite de passage sur 24 h (ex. 18 pour « avant 18 h »), ou 0 si aucune")
            int avantHeure,
            @JsonPropertyDescription("Attente maximale acceptée en minutes, ou 0 si aucune")
            int attenteMax,
            @JsonPropertyDescription("true si le client veut payer peu cher")
            boolean petitPrix) {}

    private final RechercheService recherche;
    private final ClaudeClient claude;
    private final GeminiClient gemini;

    public AssistantService(RechercheService recherche, ClaudeClient claude, GeminiClient gemini) {
        this.recherche = recherche;
        this.claude = claude;
        this.gemini = gemini;
        log.info("IA de Dourek (assistant et formulaire) : {}", claude.actif() ? "API Claude, modèle " + claude.modele()
                : gemini.actif() ? "API Gemini, modèle " + gemini.modele()
                : "mode local (ni ANTHROPIC_API_KEY ni GEMINI_API_KEY)");
    }

    public boolean iaDisponible() {
        return claude.actif() || gemini.actif();
    }

    public AssistantReponse repondre(AssistantRequest req) {
        Criteres criteres = null;
        String source = "local";
        if (claude.actif()) {
            try {
                criteres = extraireAvecIA(req.message());
                source = "claude";
            } catch (RuntimeException ex) {
                log.warn("Assistant : appel à Claude impossible ({})", ex.getMessage());
            }
        }
        // Pas de clé Claude, ou Claude injoignable : on essaie Gemini s'il est configuré.
        if (criteres == null && gemini.actif()) {
            try {
                criteres = extraireAvecGemini(req.message());
                source = "gemini";
            } catch (RuntimeException ex) {
                log.warn("Assistant : appel à Gemini impossible ({}), analyse locale", ex.getMessage());
            }
        }
        if (criteres == null) criteres = extraireLocalement(req.message());

        List<EtablissementResume> resultats = classer(criteres, req.lat(), req.lng());
        String reponse = null;
        if (!"local".equals(source)) {
            try {
                reponse = "gemini".equals(source)
                        ? gemini.texte(CONSIGNE_EXPLICATION, demandeEtClassement(req.message(), resultats))
                        : expliquerAvecIA(req.message(), criteres, resultats);
                if (reponse != null && reponse.isBlank()) reponse = null;
            } catch (RuntimeException ex) {
                log.warn("Assistant : explication impossible ({})", ex.getMessage());
            }
        }
        if (reponse == null) reponse = expliquerLocalement(criteres, resultats);
        return new AssistantReponse(reponse, criteres, resultats, source);
    }

    // ------------------------------------------------------------------ classement (notre code)

    List<EtablissementResume> classer(Criteres c, Double lat, Double lng) {
        List<EtablissementResume> tous = recherche.rechercher(c.categorie(), null, lat, lng);
        String presta = RechercheService.normaliser(c.prestation());
        String quartier = RechercheService.normaliser(c.quartier());
        int maintenant = ZonedDateTime.now(FileService.TUNIS).getHour() * 60 + ZonedDateTime.now(FileService.TUNIS).getMinute();
        List<EtablissementResume> classes = tous.stream()
                .filter(EtablissementResume::fileOuverte)
                .filter(e -> presta.isBlank() || e.prestations().stream()
                        .anyMatch(p -> RechercheService.normaliser(p.nom()).contains(presta)
                                || presta.contains(RechercheService.normaliser(p.nom()))))
                .filter(e -> c.attenteMax() == null || e.attenteMinutes() <= c.attenteMax())
                .filter(e -> c.avantHeure() == null || maintenant + e.attenteMinutes() <= c.avantHeure() * 60)
                .sorted((a, b) -> Double.compare(ajuste(a, quartier, c), ajuste(b, quartier, c)))
                .toList();
        // Quartier demandé (« à Ennasr ») : s'il y a des établissements sur place, on ne garde qu'eux.
        if (!quartier.isBlank()) {
            List<EtablissementResume> surPlace = classes.stream()
                    .filter(e -> RechercheService.normaliser(e.quartier() + " " + e.adresse() + " " + e.nom()).contains(quartier)).toList();
            if (!surPlace.isEmpty()) classes = surPlace;
        }
        return classes.stream().limit(3).toList();
    }

    /** Petits bonus quand le quartier demandé ou le petit prix correspondent. */
    private static double ajuste(EtablissementResume e, String quartier, Criteres c) {
        double s = e.score();
        if (!quartier.isBlank() && RechercheService.normaliser(e.quartier() + " " + e.adresse()).contains(quartier)) s -= 8;
        if (Boolean.TRUE.equals(c.petitPrix())) s += e.prestations().stream().mapToDouble(PrestationDto::prix).min().orElse(0) / 2;
        return s;
    }

    private static final String CONSIGNE_EXTRACTION = "Tu analyses la demande d'un client de Dourek, une plateforme tunisienne de files d'attente "
            + "virtuelles (barbiers, coiffure, laboratoires, agences, administrations). "
            + "Extrais uniquement les critères présents dans la demande, sans rien inventer. "
            + "La demande peut être en français, en arabe ou en dialecte tunisien.";

    private static final String CONSIGNE_EXPLICATION = "Tu es l'assistant de Dourek. Le classement ci-dessous a été calculé par Dourek "
            + "(distance, attente, avis) : tu ne le modifies pas, tu l'expliques. Réponds en français, "
            + "en deux phrases au plus, sans liste ni titre. Recommande le premier et cite au besoin "
            + "le deuxième comme alternative. N'invente aucune information absente des données.";

    private static String demandeEtClassement(String message, List<EtablissementResume> res) {
        String donnees = res.isEmpty() ? "Aucun établissement ne correspond." : res.stream()
                .map(e -> String.format(Locale.FRANCE, "%d. %s (%s, %s) : %.1f km, attente ~%d min, %d personnes, note %.1f, ferme à %d h, prix dès %.0f DT",
                        res.indexOf(e) + 1, e.nom(), e.categorieLibelle(), e.quartier(), e.distanceKm(), e.attenteMinutes(),
                        e.personnesEnAttente(), e.note(), e.heureFermeture(),
                        e.prestations().stream().mapToDouble(PrestationDto::prix).min().orElse(0)))
                .collect(Collectors.joining("\n"));
        return "Demande du client : « " + message + " »\n\nClassement Dourek :\n" + donnees;
    }

    // ------------------------------------------------------------------ API Gemini (offre gratuite)

    /** Schéma JSON des critères pour la sortie structurée de Gemini. */
    private static final Map<String, Object> SCHEMA_GEMINI = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "categorie", Map.of("type", "STRING", "description", "Une valeur parmi BARBIER, COIFFURE_FEMME, LABORATOIRE, AGENCE, ADMINISTRATION, AUTRE, ou chaîne vide si inconnue"),
                    "prestation", Map.of("type", "STRING", "description", "Prestation demandée en français (ex. Coupe homme, Barbe, Prise de sang), ou chaîne vide"),
                    "quartier", Map.of("type", "STRING", "description", "Quartier ou ville mentionné (ex. Ariana, Ennasr), ou chaîne vide"),
                    "avantHeure", Map.of("type", "INTEGER", "description", "Heure limite sur 24 h (18 pour « avant 18 h »), ou 0"),
                    "attenteMax", Map.of("type", "INTEGER", "description", "Attente maximale en minutes, ou 0"),
                    "petitPrix", Map.of("type", "BOOLEAN", "description", "true si le client veut payer peu cher")),
            "required", List.of("categorie", "prestation", "quartier", "avantHeure", "attenteMax", "petitPrix"));

    private Criteres extraireAvecGemini(String message) {
        JsonNode x = gemini.json(CONSIGNE_EXTRACTION, message, SCHEMA_GEMINI);
        return depuisExtraits(new CriteresExtraits(x.path("categorie").asString(""), x.path("prestation").asString(""),
                x.path("quartier").asString(""), x.path("avantHeure").asInt(0), x.path("attenteMax").asInt(0),
                x.path("petitPrix").asBoolean(false)), message);
    }

    // ------------------------------------------------------------------ API Claude

    private Criteres extraireAvecIA(String message) {
        StructuredMessageCreateParams<CriteresExtraits> params = MessageCreateParams.builder()
                .model(claude.modele())
                .maxTokens(2000L)
                .system(CONSIGNE_EXTRACTION)
                .outputConfig(CriteresExtraits.class)
                .addUserMessage(message)
                .build();
        CriteresExtraits x = claude.api().messages().create(params).content().stream()
                .flatMap(b -> b.text().stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("réponse vide"))
                .text();
        return depuisExtraits(x, message);
    }

    /** Convertit les critères renvoyés par le modèle (Claude ou Gemini) en critères Dourek. */
    private static Criteres depuisExtraits(CriteresExtraits x, String message) {
        Categorie cat = null;
        try {
            if (x.categorie() != null && !x.categorie().isBlank()) cat = Categorie.valueOf(x.categorie().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignore) {
            cat = RechercheService.categorieDepuisTexte(message);
        }
        return new Criteres(cat == null ? null : cat.name(), vide(x.prestation()), vide(x.quartier()),
                x.avantHeure() > 0 ? x.avantHeure() : null, x.attenteMax() > 0 ? x.attenteMax() : null, x.petitPrix());
    }

    private String expliquerAvecIA(String message, Criteres c, List<EtablissementResume> res) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(claude.modele())
                .maxTokens(1000L)
                .system(CONSIGNE_EXPLICATION)
                .addUserMessage(demandeEtClassement(message, res))
                .build();
        Message m = claude.api().messages().create(params);
        if (m.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) return null;
        String texte = m.content().stream().flatMap(b -> b.text().stream()).map(t -> t.text())
                .collect(Collectors.joining()).trim();
        return texte.isBlank() ? null : texte;
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ------------------------------------------------------------------ mode local (sans clé)

    private static final Pattern HEURE = Pattern.compile("avant\\s*(\\d{1,2})\\s*(h|heures?)?");
    private static final Pattern MINUTES = Pattern.compile("(moins de|max(imum)?|<)\\s*(\\d{1,3})\\s*min");

    static Criteres extraireLocalement(String message) {
        String t = RechercheService.normaliser(message);
        Categorie cat = RechercheService.categorieDepuisTexte(message);
        String presta = null;
        if (t.contains("coupe") && t.contains("barbe")) presta = "Coupe + barbe";
        else if (t.contains("barbe")) presta = "Barbe";
        else if (t.contains("enfant")) presta = "Coupe enfant";
        else if (t.contains("coupe") || t.contains("degrade")) presta = cat == Categorie.COIFFURE_FEMME ? "Coupe" : "Coupe homme";
        else if (t.contains("prise de sang") || t.contains("analyse")) presta = "Prise de sang";
        String quartier = null;
        for (String q : List.of("ariana", "ennasr", "el menzah", "menzah", "la soukra", "soukra", "manar", "tunis"))
            if (t.contains(q)) { quartier = q; break; }
        Matcher h = HEURE.matcher(t);
        Integer avant = h.find() ? Integer.valueOf(h.group(1)) : null;
        Matcher m = MINUTES.matcher(t);
        Integer max = m.find() ? Integer.valueOf(m.group(3)) : null;
        boolean petitPrix = t.matches(".*(pas cher|pas trop cher|moins cher|petit prix|economique).*");
        return new Criteres(cat == null ? null : cat.name(), presta, quartier, avant, max, petitPrix);
    }

    static String expliquerLocalement(Criteres c, List<EtablissementResume> res) {
        if (res.isEmpty())
            return "Je n'ai trouvé aucun établissement ouvert qui respecte tous vos critères. Essayez d'élargir l'heure ou la zone.";
        EtablissementResume a = res.getFirst();
        StringBuilder sb = new StringBuilder(String.format(Locale.FRANCE,
                "Je vous conseille %s : à %s, environ %d min d'attente (%d personne%s) et il ferme à %d h.",
                a.nom(), distance(a.distanceKm()), a.attenteMinutes(), a.personnesEnAttente(),
                a.personnesEnAttente() > 1 ? "s" : "", a.heureFermeture()));
        if (res.size() > 1) {
            EtablissementResume b = res.get(1);
            sb.append(String.format(Locale.FRANCE, " %s est une alternative à %s avec ~%d min d'attente.",
                    b.nom(), distance(b.distanceKm()), b.attenteMinutes()));
        }
        return sb.toString();
    }

    private static String distance(double km) {
        return km < 1 ? Math.round(km * 1000 / 50) * 50 + " m" : String.format(Locale.FRANCE, "%.1f km", km);
    }
}
