package tn.supcom.dourek.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.supcom.dourek.dto.GestionDtos.*;
import tn.supcom.dourek.service.*;

/** Profil du client, avis et page Contact. */
@RestController
@RequestMapping("/api")
public class CompteController {

    private final CompteService compte;
    private final AvisService avis;
    private final FileService file;

    public CompteController(CompteService compte, AvisService avis, FileService file) {
        this.compte = compte;
        this.avis = avis;
        this.file = file;
    }

    @GetMapping("/client/profil")
    public Profil profil(@AuthenticationPrincipal Jwt jwt) {
        return compte.profil(jwt.getSubject());
    }

    @PutMapping("/client/profil")
    public Profil modifier(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfilRequest req) {
        return compte.modifier(jwt.getSubject(), req);
    }

    @GetMapping("/tickets/{code}/avis")
    public ResponseEntity<AvisVue> avisDuTicket(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
        file.verifierProprietaire(code, jwt.getSubject());
        return avis.duTicket(code).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/tickets/{code}/avis")
    @ResponseStatus(HttpStatus.CREATED)
    public AvisVue donner(@PathVariable String code, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AvisRequest req) {
        file.verifierProprietaire(code, jwt.getSubject());
        return avis.donner(code, req);
    }

    @GetMapping("/etablissements/{slug}/avis")
    public List<AvisVue> avis(@PathVariable String slug) {
        return avis.deLEtablissement(slug);
    }

    @PostMapping("/avis/{id}/signaler")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void signaler(@PathVariable Long id) {
        avis.signaler(id);
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public void contacter(@Valid @RequestBody ContactRequest req) {
        compte.contacter(req);
    }
}
