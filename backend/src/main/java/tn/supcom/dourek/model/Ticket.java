package tn.supcom.dourek.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tickets", indexes = {
        @Index(columnList = "etablissement_id, statut"),
        @Index(columnList = "code", unique = true)
})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant secret dans l'URL de suivi (/ticket/{code}). */
    @Column(nullable = false, unique = true, length = 36)
    private String code;

    /** Numéro affiché, ex. B-24 ou P-03. */
    @Column(nullable = false)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY)
    private Prestation prestation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTicket statut = StatutTicket.EN_ATTENTE;

    private boolean prioritaire;
    /** Ajouté au guichet par un employé (client sans smartphone). */
    private boolean surPlace;

    private String nomClient;
    private String telephone;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reponsesJson;
    private int formulaireVersion;

    /** Ordre dans la file (permet « laisser passer » sans changer l'heure d'arrivée). */
    private long ordre;

    private Instant creeLe = Instant.now();
    private Instant appeleLe;
    private Instant termineLe;

    /** Poste qui a appelé le client, ex. "Poste 2 · Sami". */
    private String poste;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Etablissement getEtablissement() { return etablissement; }
    public void setEtablissement(Etablissement etablissement) { this.etablissement = etablissement; }
    public Prestation getPrestation() { return prestation; }
    public void setPrestation(Prestation prestation) { this.prestation = prestation; }
    public StatutTicket getStatut() { return statut; }
    public void setStatut(StatutTicket statut) { this.statut = statut; }
    public boolean isPrioritaire() { return prioritaire; }
    public void setPrioritaire(boolean prioritaire) { this.prioritaire = prioritaire; }
    public boolean isSurPlace() { return surPlace; }
    public void setSurPlace(boolean surPlace) { this.surPlace = surPlace; }
    public String getNomClient() { return nomClient; }
    public void setNomClient(String nomClient) { this.nomClient = nomClient; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getReponsesJson() { return reponsesJson; }
    public void setReponsesJson(String reponsesJson) { this.reponsesJson = reponsesJson; }
    public int getFormulaireVersion() { return formulaireVersion; }
    public void setFormulaireVersion(int formulaireVersion) { this.formulaireVersion = formulaireVersion; }
    public long getOrdre() { return ordre; }
    public void setOrdre(long ordre) { this.ordre = ordre; }
    public Instant getCreeLe() { return creeLe; }
    public void setCreeLe(Instant creeLe) { this.creeLe = creeLe; }
    public Instant getAppeleLe() { return appeleLe; }
    public void setAppeleLe(Instant appeleLe) { this.appeleLe = appeleLe; }
    public Instant getTermineLe() { return termineLe; }
    public void setTermineLe(Instant termineLe) { this.termineLe = termineLe; }
    public String getPoste() { return poste; }
    public void setPoste(String poste) { this.poste = poste; }
}
