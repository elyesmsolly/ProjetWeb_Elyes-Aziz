package tn.supcom.dourek.web;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.supcom.dourek.dto.Dtos.*;
import tn.supcom.dourek.model.StatutTicket;
import tn.supcom.dourek.service.FileService;
import tn.supcom.dourek.service.FormulaireService;

/**
 * Espace pro. L'établissement et le poste viennent du jeton JWT : un employé ne peut agir
 * que sur la file de son établissement.
 */
@RestController
@RequestMapping("/api/pro")
public class ProController {

    private final FileService file;
    private final FormulaireService formulaires;

    public ProController(FileService file, FormulaireService formulaires) {
        this.file = file;
        this.formulaires = formulaires;
    }

    private static Long etab(Jwt jwt) {
        return Long.valueOf(jwt.getClaimAsString("etab"));
    }

    private static String poste(Jwt jwt) {
        String p = jwt.getClaimAsString("poste");
        return p == null || p.isBlank() ? "Poste 1" : p + " · " + jwt.getClaimAsString("nom");
    }

    // ---- écran employé (gérant ou employé)

    @GetMapping("/file")
    public EtatFile etat(@AuthenticationPrincipal Jwt jwt) {
        return file.etat(etab(jwt));
    }

    @PostMapping("/file/appeler-suivant")
    public EtatFile appelerSuivant(@AuthenticationPrincipal Jwt jwt) {
        return file.appelerSuivant(etab(jwt), poste(jwt));
    }

    @PostMapping("/file/servi")
    public EtatFile servi(@AuthenticationPrincipal Jwt jwt) {
        return file.terminer(etab(jwt), poste(jwt), StatutTicket.SERVI);
    }

    @PostMapping("/file/absent")
    public EtatFile absent(@AuthenticationPrincipal Jwt jwt) {
        return file.terminer(etab(jwt), poste(jwt), StatutTicket.ABSENT);
    }

    @PostMapping("/file/sur-place")
    public EtatFile surPlace(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AjoutSurPlaceRequest req) {
        return file.ajouterSurPlace(etab(jwt), req);
    }

    @GetMapping("/moi")
    public Map<String, Object> moi(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("poste", poste(jwt), "role", jwt.getClaimAsString("role"), "etablissementId", etab(jwt));
    }

    // ---- gérant uniquement

    @GetMapping("/gerant/statistiques")
    public Statistiques statistiques(@AuthenticationPrincipal Jwt jwt) {
        return file.statistiques(etab(jwt));
    }

    @PostMapping("/gerant/file/ouverture")
    public EtatFile ouverture(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, Boolean> corps) {
        return file.changerOuverture(etab(jwt), Boolean.TRUE.equals(corps.get("ouverte")));
    }

    @GetMapping("/gerant/formulaire")
    public FormulaireDto formulaire(@AuthenticationPrincipal Jwt jwt) {
        return formulaires.actif(etab(jwt));
    }

    @PostMapping("/gerant/formulaire")
    public FormulaireDto publier(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PublierFormulaireRequest req) {
        return formulaires.publier(etab(jwt), req.champs());
    }
}
