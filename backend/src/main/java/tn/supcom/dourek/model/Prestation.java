package tn.supcom.dourek.model;

import jakarta.persistence.*;

/** Service proposé par un établissement (ex. Coupe homme, 20 min, 15 DT). */
@Entity
@Table(name = "services")
public class Prestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Etablissement etablissement;

    @Column(nullable = false)
    private String nom;
    private String description;
    private int dureeMinutes;
    private double prix;

    public Prestation() {}

    public Prestation(String nom, String description, int dureeMinutes, double prix) {
        this.nom = nom;
        this.description = description;
        this.dureeMinutes = dureeMinutes;
        this.prix = prix;
    }

    public Long getId() { return id; }
    public Etablissement getEtablissement() { return etablissement; }
    public void setEtablissement(Etablissement etablissement) { this.etablissement = etablissement; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(int dureeMinutes) { this.dureeMinutes = dureeMinutes; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }
}
