package tn.supcom.dourek.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "etablissements")
public class Etablissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categorie categorie;

    /** Type d'activité saisi par le gérant quand il choisit « Autres » (ex. « Pressing »). */
    private String categorieLibre;

    private String telephone;
    private String adresse;
    private String quartier;
    private String description;
    private double latitude;
    private double longitude;

    /** Note moyenne des avis (0 à 5) et nombre d'avis. */
    private double note;
    private int nombreAvis;

    /** Horaires lisibles, ex. "Lun – Sam : 9 h – 20 h". */
    private String horaires;
    private int heureOuverture = 9;
    private int heureFermeture = 20;

    /** false = file en pause : aucun nouveau ticket. */
    private boolean fileOuverte = true;
    private int postesOuverts = 1;

    /** Compteurs des numéros de ticket (normal / prioritaire). */
    private int compteurNormal;
    private int compteurPrioritaire;
    /** Règle anti-famine : nombre de clients normaux appelés depuis le dernier prioritaire. */
    private int normauxDepuisPrioritaire;

    @OneToMany(mappedBy = "etablissement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Prestation> prestations = new ArrayList<>();

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }
    public String getCategorieLibre() { return categorieLibre; }
    public void setCategorieLibre(String categorieLibre) { this.categorieLibre = categorieLibre; }
    /** Libellé affiché : le texte libre pour « Autres », sinon le libellé de la catégorie. */
    public String libelleCategorie() {
        return categorie == Categorie.AUTRE && categorieLibre != null && !categorieLibre.isBlank() ? categorieLibre : categorie.getLibelle();
    }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getQuartier() { return quartier; }
    public void setQuartier(String quartier) { this.quartier = quartier; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public double getNote() { return note; }
    public void setNote(double note) { this.note = note; }
    public int getNombreAvis() { return nombreAvis; }
    public void setNombreAvis(int nombreAvis) { this.nombreAvis = nombreAvis; }
    public String getHoraires() { return horaires; }
    public void setHoraires(String horaires) { this.horaires = horaires; }
    public int getHeureOuverture() { return heureOuverture; }
    public void setHeureOuverture(int heureOuverture) { this.heureOuverture = heureOuverture; }
    public int getHeureFermeture() { return heureFermeture; }
    public void setHeureFermeture(int heureFermeture) { this.heureFermeture = heureFermeture; }
    public boolean isFileOuverte() { return fileOuverte; }
    public void setFileOuverte(boolean fileOuverte) { this.fileOuverte = fileOuverte; }
    public int getPostesOuverts() { return postesOuverts; }
    public void setPostesOuverts(int postesOuverts) { this.postesOuverts = postesOuverts; }
    public int getCompteurNormal() { return compteurNormal; }
    public void setCompteurNormal(int compteurNormal) { this.compteurNormal = compteurNormal; }
    public int getCompteurPrioritaire() { return compteurPrioritaire; }
    public void setCompteurPrioritaire(int compteurPrioritaire) { this.compteurPrioritaire = compteurPrioritaire; }
    public int getNormauxDepuisPrioritaire() { return normauxDepuisPrioritaire; }
    public void setNormauxDepuisPrioritaire(int n) { this.normauxDepuisPrioritaire = n; }
    public List<Prestation> getPrestations() { return prestations; }

    public void ajouterPrestation(Prestation p) {
        p.setEtablissement(this);
        prestations.add(p);
    }
}
