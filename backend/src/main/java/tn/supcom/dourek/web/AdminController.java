package tn.supcom.dourek.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.supcom.dourek.dto.GestionDtos.*;
import tn.supcom.dourek.service.AdminService;

/** Réservé au rôle ADMIN (voir SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/etablissements")
    public List<EtablissementAdmin> etablissements() {
        return admin.etablissements();
    }

    @PutMapping("/etablissements/{id}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendreEtablissement(@PathVariable Long id, @Valid @RequestBody SuspensionRequest req) {
        admin.suspendreEtablissement(id, req.suspendu());
    }

    @GetMapping("/utilisateurs")
    public List<UtilisateurAdmin> utilisateurs() {
        return admin.utilisateurs();
    }

    @PutMapping("/utilisateurs/{id}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendreUtilisateur(@PathVariable Long id, @Valid @RequestBody SuspensionRequest req) {
        admin.suspendreUtilisateur(id, req.suspendu());
    }

    @PutMapping("/utilisateurs/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void role(@PathVariable Long id, @Valid @RequestBody RoleRequest req) {
        admin.changerRole(id, req.role());
    }

    @GetMapping("/avis")
    public List<AvisVue> avis() {
        return admin.avisSignales();
    }

    @PutMapping("/avis/{id}/conserver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void conserver(@PathVariable Long id) {
        admin.conserverAvis(id);
    }

    @DeleteMapping("/avis/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerAvis(@PathVariable Long id) {
        admin.supprimerAvis(id);
    }

    @GetMapping("/categories")
    public List<CategorieAdmin> categories() {
        return admin.categories();
    }

    @PutMapping("/categories")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void renommer(@Valid @RequestBody RenommageRequest req) {
        admin.renommerCategorie(req);
    }

    @GetMapping("/messages")
    public List<MessageVue> messages() {
        return admin.messages();
    }

    @DeleteMapping("/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerMessage(@PathVariable Long id) {
        admin.supprimerMessage(id);
    }
}
