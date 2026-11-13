package tn.supcom.dourek.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Objets échangés en JSON avec le front Angular (records Java = immuables). */
public final class Dtos {
    private Dtos() {}

    /** Un champ du formulaire client, défini par le gérant dans le constructeur. */
    public record ChampFormulaire(
            @NotBlank String cle,
            @NotBlank String type,   // TEXTE, TEXTE_LONG, TELEPHONE, NOMBRE, LISTE, CHOIX, CASE, DATE
            @NotBlank String label,
            boolean obligatoire,
            List<String> options,
            String aide) {}

    public record FormulaireDto(int version, List<ChampFormulaire> champs, Instant publieLe) {}

    public record PublierFormulaireRequest(@NotNull @Size(min = 1, max = 30) List<@Valid ChampFormulaire> champs) {}

    public record PrestationDto(Long id, @NotBlank String nom, String description, int dureeMinutes, double prix) {}

    public record EtablissementResume(
            Long id, String slug, String nom, String categorie, String categorieLibelle,
            String quartier, String adresse, double latitude, double longitude,
            double distanceKm, double note, int nombreAvis,
            boolean fileOuverte, int heureFermeture,
            int personnesEnAttente, int attenteMinutes, int postesOuverts,
            List<PrestationDto> prestations, double score, boolean meilleurChoix) {}

    public record EtablissementDetail(
            EtablissementResume resume, String description, String horaires, String telephone,
            FormulaireDto formulaire) {}

    public record PriseTicketRequest(
            @NotNull Long prestationId,
            Map<String, Object> reponses,
            Boolean prioritaire) {}

    public record TicketCree(String code, String numero) {}

    /** Pastille de la file affichée sur la page de suivi. */
    public record PastilleFile(String numero, String etat) {} // SERVI_EN_COURS, ATTENTE, MOI

    public record TicketVue(
            String code, String numero, String statut,
            Long etablissementId, String etablissementSlug, String etablissementNom, String adresse,
            double latitude, double longitude,
            String prestation, int personnesAvant, int attenteMinutes, Instant passageEstime,
            Instant creeLe, Instant appeleLe, Instant termineLe, String poste,
            int formulaireVersion, boolean prioritaire, List<PastilleFile> file) {}

    /** Une ligne de la file vue par le gérant ou l'employé. */
    public record TicketLigne(
            String code, String numero, String nomClient, String prestation, String statut,
            boolean prioritaire, boolean surPlace, Instant creeLe, Instant appeleLe,
            int attenteEstimeeMinutes, String poste, Map<String, Object> reponses,
            List<ChampFormulaire> champs) {}

    public record EtatFile(
            Long etablissementId, String etablissementNom, boolean fileOuverte, int postesOuverts,
            int dureeMoyenneMinutes, List<TicketLigne> enCours, List<TicketLigne> enAttente,
            List<PrestationDto> prestations) {}

    public record AffluenceHeure(int heure, int nombre) {}

    public record PosteEtat(String poste, String employe, String ticket, String prestation, Integer depuisMinutes) {}

    public record Statistiques(
            int servis, int enAttente, int attenteMoyenneMinutes, int tauxAbsence,
            int absents, int ecartEstimeReelMinutes, List<AffluenceHeure> affluence, List<PosteEtat> postes,
            List<TicketLigne> derniers) {}

    public record AjoutSurPlaceRequest(@NotBlank String nom, @NotNull Long prestationId, boolean prioritaire) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String motDePasse) {}

    public record AuthReponse(String token, String nom, String role, Long etablissementId,
                              String etablissementNom, String slug, String poste, String email, String telephone) {}

    public record InscriptionClientRequest(
            @NotBlank String nom,
            @NotBlank @Email String email,
            @NotBlank String telephone,
            @NotBlank @Size(min = 6) String motDePasse) {}

    public record InscriptionRequest(
            @NotBlank String nom,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String motDePasse,
            @NotBlank String nomEtablissement,
            String telephone,
            @NotBlank String categorie,
            String categorieLibre,
            @NotBlank String adresse,
            String quartier,
            double latitude,
            double longitude,
            int heureOuverture,
            int heureFermeture,
            String horaires,
            int postes,
            @NotNull @Size(min = 1) List<@Valid PrestationDto> prestations,
            List<@Valid ChampFormulaire> champs) {}

    public record AssistantRequest(@NotBlank @Size(max = 500) String message, Double lat, Double lng) {}

    public record Criteres(String categorie, String prestation, String quartier, Integer avantHeure,
                           Integer attenteMax, Boolean petitPrix) {}

    public record AssistantReponse(String reponse, Criteres criteres, List<EtablissementResume> resultats,
                                   String source) {}

    // ---- formulaire proposé par l'IA

    public record GenerationFormulaireRequest(@NotBlank @Size(min = 3, max = 600) String description,
                                              @Size(max = 80) String etablissement,
                                              @Size(max = 60) String activite) {}

    /** source : « claude », « gemini » ou « local » (règles par mots-clés, sans IA). */
    public record GenerationFormulaireReponse(List<ChampFormulaire> champs, String source) {}
}
