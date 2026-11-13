package tn.supcom.dourek.web;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.supcom.dourek.dto.Dtos.*;
import tn.supcom.dourek.model.Categorie;
import tn.supcom.dourek.service.AssistantService;
import tn.supcom.dourek.service.FormulaireIaService;
import tn.supcom.dourek.service.FileService;
import tn.supcom.dourek.service.RechercheService;

/** API publique utilisée par les clients (sans compte). */
@RestController
@RequestMapping("/api")
public class PublicController {

    private final RechercheService recherche;
    private final FileService file;
    private final AssistantService assistant;
    private final FormulaireIaService formulaireIa;

    public PublicController(RechercheService recherche, FileService file, AssistantService assistant,
                            FormulaireIaService formulaireIa) {
        this.recherche = recherche;
        this.file = file;
        this.assistant = assistant;
        this.formulaireIa = formulaireIa;
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        List<EtablissementResume> tous = recherche.rechercher(null, null, null, null);
        List<Map<String, Object>> res = new java.util.ArrayList<>(Arrays.stream(Categorie.values()).map(c -> Map.<String, Object>of(
                "code", c.name(), "libelle", c.getLibelle(),
                "nombre", tous.stream().filter(e -> e.categorie().equals(c.name())).count())).toList());
        // Types créés par les gérants via « Autres » : enregistrés en base, ils deviennent des filtres.
        tous.stream().filter(e -> e.categorie().equals("AUTRE") && !e.categorieLibelle().equals(Categorie.AUTRE.getLibelle()))
                .collect(java.util.stream.Collectors.groupingBy(EtablissementResume::categorieLibelle, java.util.TreeMap::new, java.util.stream.Collectors.counting()))
                .forEach((libelle, n) -> res.add(Map.of("code", libelle, "libelle", libelle, "nombre", n, "libre", true)));
        return res;
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
    public TicketCree prendreTicket(@PathVariable String slug, @Valid @RequestBody PriseTicketRequest req,
                                    @AuthenticationPrincipal Jwt jwt) {
        return file.prendreTicket(slug, req, jwt.getSubject());
    }

    @GetMapping("/client/tickets")
    public List<TicketVue> mesTickets(@AuthenticationPrincipal Jwt jwt) {
        return file.mesTickets(jwt.getSubject());
    }

    @GetMapping("/tickets/{code}")
    public TicketVue ticket(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
        file.verifierProprietaire(code, jwt.getSubject());
        return file.vue(code);
    }

    @PostMapping("/tickets/{code}/annuler")
    public TicketVue annuler(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
        file.verifierProprietaire(code, jwt.getSubject());
        return file.annuler(code);
    }

    @PostMapping("/tickets/{code}/laisser-passer")
    public TicketVue laisserPasser(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
        file.verifierProprietaire(code, jwt.getSubject());
        return file.laisserPasser(code);
    }

    @PostMapping("/assistant")
    public AssistantReponse assistant(@Valid @RequestBody AssistantRequest req) {
        return assistant.repondre(req);
    }

    /**
     * Propose un formulaire client à partir d'une phrase du gérant. Route publique : elle sert aussi pendant
     * l'inscription, avant que le compte existe. Elle ne modifie rien : le gérant publie lui-même le formulaire.
     */
    @PostMapping("/assistant/formulaire")
    public GenerationFormulaireReponse formulaire(@Valid @RequestBody GenerationFormulaireRequest req) {
        return formulaireIa.generer(req);
    }

    @GetMapping("/assistant/statut")
    public Map<String, Object> statutAssistant() {
        return Map.of("ia", assistant.iaDisponible());
    }
}
