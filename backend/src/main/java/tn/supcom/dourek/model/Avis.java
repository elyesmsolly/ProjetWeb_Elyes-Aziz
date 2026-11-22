package tn.supcom.dourek.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Avis laissé par un client après avoir été servi : un seul par ticket. */
@Entity
@Table(name = "avis")
public class Avis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(unique = true)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Etablissement etablissement;

    private int note;

    @Column(length = 500)
    private String commentaire;

    private boolean signale;
    private Instant creeLe = Instant.now();

    public Avis() {}

    public Avis(Ticket ticket, int note, String commentaire) {
        this.ticket = ticket;
        this.etablissement = ticket.getEtablissement();
        this.note = note;
        this.commentaire = commentaire;
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public Etablissement getEtablissement() { return etablissement; }
    public int getNote() { return note; }
    public String getCommentaire() { return commentaire; }
    public boolean isSignale() { return signale; }
    public void setSignale(boolean signale) { this.signale = signale; }
    public Instant getCreeLe() { return creeLe; }
}
