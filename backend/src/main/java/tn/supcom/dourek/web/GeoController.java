package tn.supcom.dourek.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Adresse d'un point cliqué sur la carte (géocodage inverse).
 * Service gratuit Nominatim d'OpenStreetMap, sans clé. On passe par le serveur pour envoyer
 * un User-Agent identifiable (règle d'usage de Nominatim) et garder les réponses en cache.
 */
@RestController
@RequestMapping("/api/geo")
public class GeoController {

    public record Adresse(String adresse, String quartier) {}

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String, Adresse> cache = new ConcurrentHashMap<>();
    private final ObjectMapper json;
    private final String url;

    public GeoController(ObjectMapper json, @Value("${dourek.geo.url:https://nominatim.openstreetmap.org}") String url) {
        this.json = json;
        this.url = url;
    }

    @GetMapping("/adresse")
    public Adresse adresse(@RequestParam double lat, @RequestParam double lng) {
        String cle = String.format(Locale.ROOT, "%.5f,%.5f", lat, lng);
        Adresse connue = cache.get(cle);
        if (connue != null) return connue;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(String.format(Locale.ROOT,
                            "%s/reverse?format=jsonv2&lat=%.6f&lon=%.6f&zoom=18&addressdetails=1&accept-language=fr", url, lat, lng)))
                    .header("User-Agent", "Dourek/1.0 (projet etudiant SUP'COM)")
                    .timeout(Duration.ofSeconds(8))
                    .GET().build();
            HttpResponse<String> rep = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (rep.statusCode() != 200) throw new IllegalStateException("code " + rep.statusCode());
            Adresse a = lire(json.readTree(rep.body()));
            cache.put(cle, a);
            return a;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw indisponible();
        } catch (Exception ex) {
            throw indisponible();
        }
    }

    /** « 12 Rue Mustapha Mohsen, Ariana » et le quartier (« Ennasr », « Ariana Ville »…). */
    static Adresse lire(JsonNode r) {
        JsonNode a = r.path("address");
        String rue = premier(a, "road", "pedestrian", "street", "residential", "path", "square");
        String numero = a.path("house_number").asString("");
        String quartier = premier(a, "suburb", "neighbourhood", "quarter", "city_district", "village", "town", "city");
        String ville = premier(a, "city", "town", "village", "municipality", "county", "state");
        StringBuilder sb = new StringBuilder();
        if (!rue.isBlank()) sb.append(numero.isBlank() ? "" : numero + " ").append(rue);
        if (!ville.isBlank() && !ville.equals(rue)) sb.append(sb.isEmpty() ? "" : ", ").append(ville);
        String adresse = sb.isEmpty() ? r.path("display_name").asString("") : sb.toString();
        return new Adresse(adresse, quartier.isBlank() ? ville : quartier);
    }

    private static String premier(JsonNode a, String... cles) {
        for (String c : cles) {
            String v = a.path(c).asString("");
            if (!v.isBlank()) return v;
        }
        return "";
    }

    private static ResponseStatusException indisponible() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Adresse introuvable pour ce point (service de cartes injoignable). Saisissez-la à la main.");
    }
}
