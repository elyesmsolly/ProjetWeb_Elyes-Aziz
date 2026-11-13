package tn.supcom.dourek.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Appel à l'API Google Gemini (offre gratuite), utilisé par l'assistant quand il n'y a pas de clé Claude.
 * La clé vient de la variable d'environnement GEMINI_API_KEY : elle n'est jamais écrite dans le code
 * et reste sur le serveur (les visiteurs ne la voient pas).
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json;
    private final String cle;
    private final String modele;
    private final String url;

    public GeminiClient(ObjectMapper json,
                        @Value("${GEMINI_API_KEY:}") String cle,
                        @Value("${dourek.gemini.modele}") String modele,
                        @Value("${dourek.gemini.url}") String url) {
        this.json = json;
        this.cle = cle.trim();
        this.modele = modele;
        this.url = url;
    }

    public boolean actif() {
        return !cle.isBlank();
    }

    public String modele() {
        return modele;
    }

    /** Réponse JSON qui respecte le schéma donné (sortie structurée de Gemini). */
    public JsonNode json(String consigne, String message, Map<String, Object> schema) {
        String texte = generer(consigne, message, Map.of("responseMimeType", "application/json", "responseSchema", schema));
        return json.readTree(texte);
    }

    /** Réponse en texte libre. */
    public String texte(String consigne, String message) {
        return generer(consigne, message, Map.of()).trim();
    }

    private String generer(String consigne, String message, Map<String, Object> config) {
        Map<String, Object> corps = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", consigne))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", message)))),
                "generationConfig", config);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/v1beta/models/" + modele + ":generateContent"))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", cle)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corps)))
                    .build();
            HttpResponse<String> rep = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (rep.statusCode() != 200) {
                log.warn("Gemini : code {} : {}", rep.statusCode(), rep.body().length() > 300 ? rep.body().substring(0, 300) : rep.body());
                throw new IllegalStateException("Gemini a répondu " + rep.statusCode());
            }
            JsonNode parts = json.readTree(rep.body()).path("candidates").path(0).path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) sb.append(p.path("text").asString(""));
            if (sb.isEmpty()) throw new IllegalStateException("réponse vide");
            return sb.toString();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("appel interrompu");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Gemini injoignable : " + ex.getMessage());
        }
    }
}
