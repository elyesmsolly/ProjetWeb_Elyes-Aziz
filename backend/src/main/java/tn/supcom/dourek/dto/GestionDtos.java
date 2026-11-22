package tn.supcom.dourek.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

/** Objets JSON des pages ajoutées à l'Objectif 4. */
public final class GestionDtos {
    private GestionDtos() {}

    public record Profil(String nom, String email, String telephone) {}

    public record ProfilRequest(
            @NotBlank @Size(max = 120) String nom,
            @NotBlank @Size(max = 25) String telephone,
            String motDePasseActuel,
            @Size(min = 6, max = 72) String nouveauMotDePasse) {}

    public record AvisRequest(@Min(1) @Max(5) int note, @Size(max = 500) String commentaire) {}

    public record AvisVue(Long id, int note, String commentaire, String auteur, Instant creeLe,
                          boolean signale, String etablissement) {}

    public record ContactRequest(
            @NotBlank @Size(max = 120) String nom,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(max = 120) String sujet,
            @NotBlank @Size(min = 10, max = 2000) String message) {}

    public record MessageVue(Long id, String nom, String email, String sujet, String message, Instant creeLe) {}

    public record Parametres(
            @NotBlank @Size(max = 100) String nom,
            @Size(max = 25) String telephone,
            @NotBlank @Size(max = 200) String adresse,
            @Size(max = 80) String quartier,
            @Size(max = 250) String description,
            @Size(max = 250) String horaires,
            @Min(0) @Max(23) int heureOuverture,
            @Min(1) @Max(24) int heureFermeture) {}

    public record Employe(Long id, String nom, String email, String role, String poste) {}

    public record Equipe(int postesOuverts, List<Employe> membres) {}

    public record EmployeRequest(
            @NotBlank @Size(max = 60) String nom,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 6, max = 72) String motDePasse,
            @NotBlank @Size(max = 30) @Pattern(regexp = "[^·]+") String poste) {}

    public record PostesRequest(@Min(1) @Max(20) int postesOuverts) {}

    public record EtablissementAdmin(Long id, String slug, String nom, String categorie, String quartier,
                                     boolean fileOuverte, boolean suspendu) {}

    public record UtilisateurAdmin(Long id, String nom, String email, String role, String etablissement,
                                   boolean suspendu) {}

    public record SuspensionRequest(boolean suspendu) {}

    public record RoleRequest(@NotBlank String role) {}

    public record CategorieAdmin(String libelle, long nombre, boolean libre) {}

    public record RenommageRequest(@NotBlank @Size(max = 40) String ancien, @NotBlank @Size(max = 40) String nouveau) {}
}
