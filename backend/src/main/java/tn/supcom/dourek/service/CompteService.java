package tn.supcom.dourek.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.supcom.dourek.dto.GestionDtos.*;
import tn.supcom.dourek.model.*;
import tn.supcom.dourek.repository.*;

/** Profil du client et page Contact. */
@Service
public class CompteService {

    private final UtilisateurRepository utilisateurs;
    private final MessageContactRepository messages;
    private final PasswordEncoder encodeur;

    public CompteService(UtilisateurRepository utilisateurs, MessageContactRepository messages, PasswordEncoder encodeur) {
        this.utilisateurs = utilisateurs;
        this.messages = messages;
        this.encodeur = encodeur;
    }

    @Transactional(readOnly = true)
    public Profil profil(String email) {
        return vue(client(email));
    }

    @Transactional
    public Profil modifier(String email, ProfilRequest req) {
        Utilisateur u = client(email);
        if (!req.telephone().replaceAll("[\\s.+-]", "").matches("\\d{8,12}"))
            throw Introuvable.requete("Numéro de téléphone invalide");
        if (req.nouveauMotDePasse() != null) {
            if (req.motDePasseActuel() == null || !encodeur.matches(req.motDePasseActuel(), u.getMotDePasse()))
                throw Introuvable.requete("Mot de passe actuel incorrect");
            u.setMotDePasse(encodeur.encode(req.nouveauMotDePasse()));
        }
        u.setNom(req.nom().trim());
        u.setTelephone(req.telephone().trim());
        return vue(u);
    }

    @Transactional
    public void contacter(ContactRequest req) {
        messages.save(new MessageContact(req.nom().trim(), req.email().trim(), req.sujet().trim(), req.message().trim()));
    }

    private Utilisateur client(String email) {
        return utilisateurs.findByEmailIgnoreCase(email).orElseThrow(() -> Introuvable.de("Compte"));
    }

    private static Profil vue(Utilisateur u) {
        return new Profil(u.getNom(), u.getEmail(), u.getTelephone());
    }
}
