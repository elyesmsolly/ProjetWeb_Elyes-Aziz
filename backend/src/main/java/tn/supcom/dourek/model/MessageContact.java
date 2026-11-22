package tn.supcom.dourek.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Message envoyé depuis la page Contact, lu par l'administration. */
@Entity
@Table(name = "messages_contact")
public class MessageContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String email;
    private String sujet;

    @Column(length = 2000)
    private String message;

    private Instant creeLe = Instant.now();

    public MessageContact() {}

    public MessageContact(String nom, String email, String sujet, String message) {
        this.nom = nom;
        this.email = email;
        this.sujet = sujet;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public String getSujet() { return sujet; }
    public String getMessage() { return message; }
    public Instant getCreeLe() { return creeLe; }
}
