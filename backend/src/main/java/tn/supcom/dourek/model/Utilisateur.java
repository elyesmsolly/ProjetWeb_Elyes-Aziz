package tn.supcom.dourek.model;

import jakarta.persistence.*;

/** Compte Dourek : client, ou membre de l'espace pro (gérant, employé) rattaché à un établissement. */
@Entity
@Table(name = "profils")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Haché avec BCrypt, jamais en clair. */
    @Column(nullable = false)
    private String motDePasse;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Vide pour un client. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Etablissement etablissement;

    /** Téléphone du client (alertes). */
    private String telephone;

    /** Nom du poste de l'employé, ex. "Poste 2". */
    private String poste;

    public Utilisateur() {}

    public Utilisateur(String email, String motDePasse, String nom, Role role, Etablissement etablissement, String poste) {
        this.email = email;
        this.motDePasse = motDePasse;
        this.nom = nom;
        this.role = role;
        this.etablissement = etablissement;
        this.poste = poste;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getMotDePasse() { return motDePasse; }
    public String getNom() { return nom; }
    public Role getRole() { return role; }
    public Etablissement getEtablissement() { return etablissement; }
    public String getPoste() { return poste; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}
