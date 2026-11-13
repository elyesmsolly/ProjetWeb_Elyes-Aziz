package tn.supcom.dourek.service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.supcom.dourek.dto.Dtos.*;
import tn.supcom.dourek.model.*;
import tn.supcom.dourek.repository.EtablissementRepository;
import tn.supcom.dourek.repository.UtilisateurRepository;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurs;
    private final EtablissementRepository etablissements;
    private final FormulaireService formulaires;
    private final PasswordEncoder encodeur;
    private final JwtEncoder jwt;

    public AuthService(UtilisateurRepository utilisateurs, EtablissementRepository etablissements,
                       FormulaireService formulaires, PasswordEncoder encodeur, JwtEncoder jwt) {
        this.utilisateurs = utilisateurs;
        this.etablissements = etablissements;
        this.formulaires = formulaires;
        this.encodeur = encodeur;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public AuthReponse connexion(LoginRequest req) {
        Utilisateur u = utilisateurs.findByEmailIgnoreCase(req.email().trim())
                .filter(x -> encodeur.matches(req.motDePasse(), x.getMotDePasse()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou mot de passe incorrect"));
        return reponse(u);
    }

    /** Création de l'espace pro (assistant en 5 étapes) : compte gérant + établissement + services + formulaire. */
    @Transactional
    public AuthReponse inscription(InscriptionRequest req) {
        if (utilisateurs.existsByEmailIgnoreCase(req.email()))
            throw Introuvable.conflit("Un compte existe déjà avec cet e-mail");
        Categorie cat;
        try {
            cat = Categorie.valueOf(req.categorie());
        } catch (IllegalArgumentException ex) {
            throw Introuvable.requete("Catégorie inconnue");
        }
        Etablissement e = new Etablissement();
        e.setNom(req.nomEtablissement().trim());
        e.setSlug(slugUnique(req.nomEtablissement()));
        e.setCategorie(cat);
        e.setTelephone(req.telephone());
        e.setAdresse(req.adresse());
        e.setQuartier(req.quartier() == null || req.quartier().isBlank() ? "Ariana" : req.quartier());
        e.setLatitude(req.latitude() == 0 ? RechercheService.LAT_ARIANA : req.latitude());
        e.setLongitude(req.longitude() == 0 ? RechercheService.LNG_ARIANA : req.longitude());
        e.setHeureOuverture(req.heureOuverture() == 0 ? 9 : req.heureOuverture());
        e.setHeureFermeture(req.heureFermeture() == 0 ? 20 : req.heureFermeture());
        e.setHoraires(req.horaires());
        e.setPostesOuverts(Math.max(1, req.postes()));
        e.setNote(0);
        req.prestations().forEach(p -> e.ajouterPrestation(new Prestation(p.nom(), p.description(), Math.max(5, p.dureeMinutes()), p.prix())));
        etablissements.save(e);

        Utilisateur gerant = utilisateurs.save(new Utilisateur(req.email().trim().toLowerCase(Locale.ROOT),
                encodeur.encode(req.motDePasse()), req.nom().trim(), Role.GERANT, e, "Poste 1"));
        List<ChampFormulaire> champs = req.champs() == null || req.champs().isEmpty() ? FormulaireService.champsParDefaut() : req.champs();
        formulaires.publier(e.getId(), champs);
        return reponse(gerant);
    }

    private AuthReponse reponse(Utilisateur u) {
        Instant maintenant = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("dourek")
                .subject(u.getEmail())
                .issuedAt(maintenant)
                .expiresAt(maintenant.plus(Duration.ofHours(12)))
                .claim("role", u.getRole().name())
                .claim("etab", u.getEtablissement().getId())
                .claim("poste", u.getPoste() == null ? "" : u.getPoste())
                .claim("nom", u.getNom())
                .build();
        String token = jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        Etablissement e = u.getEtablissement();
        return new AuthReponse(token, u.getNom(), u.getRole().name(), e.getId(), e.getNom(), e.getSlug(), u.getPoste());
    }

    private String slugUnique(String nom) {
        String base = Normalizer.normalize(nom, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "etablissement";
        String slug = base;
        for (int i = 2; etablissements.existsBySlug(slug); i++) slug = base + "-" + i;
        return slug;
    }
}
