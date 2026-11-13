package tn.supcom.dourek.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Formulaire client versionné. Chaque publication crée une nouvelle ligne
 * (version + 1) : les tickets restent liés à la version remplie par le client.
 */
@Entity
@Table(name = "formulaires", uniqueConstraints = @UniqueConstraint(columnNames = {"etablissement_id", "version"}))
public class Formulaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Etablissement etablissement;

    private int version;

    /** Liste des champs, en JSON (voir ChampFormulaire). */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String champsJson;

    private Instant publieLe = Instant.now();

    public Formulaire() {}

    public Formulaire(Etablissement etablissement, int version, String champsJson) {
        this.etablissement = etablissement;
        this.version = version;
        this.champsJson = champsJson;
    }

    public Long getId() { return id; }
    public Etablissement getEtablissement() { return etablissement; }
    public int getVersion() { return version; }
    public String getChampsJson() { return champsJson; }
    public Instant getPublieLe() { return publieLe; }
}
