package tn.supcom.dourek.web;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.supcom.dourek.dto.Dtos.*;
import tn.supcom.dourek.model.Categorie;
import tn.supcom.dourek.service.AssistantService;
import tn.supcom.dourek.service.FileService;
import tn.supcom.dourek.service.RechercheService;

/** API publique utilisée par les clients (sans compte). */
@RestController
@RequestMapping("/api")
public class PublicController {

    private final RechercheService recherche;
    private final FileService file;
    private final AssistantService assistant;

    public PublicController(RechercheService recherche, FileService file, AssistantService assistant) {
        this.recherche = recherche;
        this.file = file;
        this.assistant = assistant;
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        List<EtablissementResume> tous = recherche.rechercher(null, null, null, null);
        return Arrays.stream(Categorie.values()).map(c -> Map.<String, Object>of(
                "code", c.name(), "libelle", c.getLibelle(),
                "nombre", tous.stream().filter(e -> e.categorie().equals(c.name())).count())).toList();
    }

    @GetMapping("/etablissements")
    public List<EtablissementResume> rechercher(@RequestParam(required = false) String categorie,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(required = false) Double lat,
                                                @RequestParam(required = false) Double lng) {
        return recherche.rechercher(categorie, q, lat, lng);
    }

    @GetMapping("/etablissements/{slug}")
    public EtablissementDetail detail(@PathVariable String slug,
                                      @RequestParam(required = false) Double lat,
                                      @RequestParam(required = false) Double lng) {
        return recherche.detail(slug, lat, lng);
    }

    @PostMapping("/etablissements/{slug}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCree prendreTicket(@PathVariable String slug, @Valid @RequestBody PriseTicketRequest req) {
        return file.prendreTicket(slug, req);
    }

    @GetMapping("/tickets/{code}")
    public TicketVue ticket(@PathVariable String code) {
        return file.vue(code);
    }

    @PostMapping("/tickets/{code}/annuler")
    public TicketVue annuler(@PathVariable String code) {
        return file.annuler(code);
    }

    @PostMapping("/tickets/{code}/laisser-passer")
    public TicketVue laisserPasser(@PathVariable String code) {
        return file.laisserPasser(code);
    }

    @PostMapping("/assistant")
    public AssistantReponse assistant(@Valid @RequestBody AssistantRequest req) {
        return assistant.repondre(req);
    }

    @GetMapping("/assistant/statut")
    public Map<String, Object> statutAssistant() {
        return Map.of("ia", assistant.iaDisponible());
    }
}
