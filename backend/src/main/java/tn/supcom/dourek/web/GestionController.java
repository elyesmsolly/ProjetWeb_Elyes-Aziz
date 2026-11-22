package tn.supcom.dourek.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.supcom.dourek.dto.Dtos.PrestationDto;
import tn.supcom.dourek.dto.GestionDtos.*;
import tn.supcom.dourek.service.GestionService;

/** Réservé au gérant ; l'établissement vient toujours du jeton, jamais de l'adresse. */
@RestController
@RequestMapping("/api/pro/gerant")
public class GestionController {

    private final GestionService gestion;

    public GestionController(GestionService gestion) {
        this.gestion = gestion;
    }

    private static Long etab(Jwt jwt) {
        return Long.valueOf(jwt.getClaimAsString("etab"));
    }

    @GetMapping("/services")
    public List<PrestationDto> services(@AuthenticationPrincipal Jwt jwt) {
        return gestion.services(etab(jwt));
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public PrestationDto ajouter(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PrestationDto req) {
        return gestion.ajouterService(etab(jwt), req);
    }

    @PutMapping("/services/{id}")
    public PrestationDto modifier(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody PrestationDto req) {
        return gestion.modifierService(etab(jwt), id, req);
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirer(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        gestion.retirerService(etab(jwt), id);
    }

    @GetMapping("/equipe")
    public Equipe equipe(@AuthenticationPrincipal Jwt jwt) {
        return gestion.equipe(etab(jwt));
    }

    @PutMapping("/equipe/postes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void postes(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PostesRequest req) {
        gestion.changerPostes(etab(jwt), req);
    }

    @PostMapping("/equipe")
    @ResponseStatus(HttpStatus.CREATED)
    public Employe ajouterEmploye(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EmployeRequest req) {
        return gestion.ajouterEmploye(etab(jwt), req);
    }

    @DeleteMapping("/equipe/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirerEmploye(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        gestion.retirerEmploye(etab(jwt), id);
    }

    @GetMapping("/parametres")
    public Parametres parametres(@AuthenticationPrincipal Jwt jwt) {
        return gestion.parametres(etab(jwt));
    }

    @PutMapping("/parametres")
    public Parametres modifierParametres(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Parametres req) {
        return gestion.modifierParametres(etab(jwt), req);
    }
}
