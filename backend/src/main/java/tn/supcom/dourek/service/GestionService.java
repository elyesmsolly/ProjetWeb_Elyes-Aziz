package tn.supcom.dourek.service;

import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.supcom.dourek.dto.Dtos.PrestationDto;
import tn.supcom.dourek.dto.GestionDtos.*;
import tn.supcom.dourek.model.*;
import tn.supcom.dourek.repository.*;

/** Pages du gérant : services, postes et employés, paramètres. Tout est limité à son établissement. */
@Service
public class GestionService {

    private final EtablissementRepository etablissements;
    private final PrestationRepository prestations;
    private final UtilisateurRepository utilisateurs;
    private final TicketRepository tickets;
    private final PasswordEncoder encodeur;
    private final TempsReel tempsReel;

    public GestionService(EtablissementRepository etablissements, PrestationRepository prestations,
                          UtilisateurRepository utilisateurs, TicketRepository tickets,
                          PasswordEncoder encodeur, TempsReel tempsReel) {
        this.etablissements = etablissements;
        this.prestations = prestations;
        this.utilisateurs = utilisateurs;
        this.tickets = tickets;
        this.encodeur = encodeur;
        this.tempsReel = tempsReel;
    }

    // ---- services

    @Transactional(readOnly = true)
    public List<PrestationDto> services(Long etab) {
        return etablissement(etab).getPrestations().stream().map(GestionService::vue).toList();
    }

    @Transactional
    public PrestationDto ajouterService(Long etab, PrestationDto req) {
        Prestation p = new Prestation(req.nom().trim(), req.description(), req.dureeMinutes(), req.prix());
        p.setEtablissement(verrouiller(etab));
        prestations.save(p);
        tempsReel.fileModifiee(etab);
        return vue(p);
    }

    @Transactional
    public PrestationDto modifierService(Long etab, Long id, PrestationDto req) {
        verrouiller(etab);
        Prestation p = service(etab, id);
        p.setNom(req.nom().trim());
        p.setDescription(req.description());
        p.setDureeMinutes(req.dureeMinutes());
        p.setPrix(req.prix());
        tempsReel.fileModifiee(etab);
        return vue(p);
    }

    @Transactional
    public void retirerService(Long etab, Long id) {
        Etablissement e = verrouiller(etab);
        Prestation p = service(etab, id);
        if (e.getPrestations().size() <= 1) throw Introuvable.conflit("Gardez au moins un service");
        p.setRetiree(true);
        tempsReel.fileModifiee(etab);
    }

    // ---- postes et employés

    @Transactional(readOnly = true)
    public Equipe equipe(Long etab) {
        return new Equipe(etablissement(etab).getPostesOuverts(),
                utilisateurs.findByEtablissementIdOrderByPosteAsc(etab).stream().map(GestionService::vue).toList());
    }

    @Transactional
    public void changerPostes(Long etab, PostesRequest req) {
        verrouiller(etab).setPostesOuverts(req.postesOuverts());
        tempsReel.fileModifiee(etab);
    }

    @Transactional
    public Employe ajouterEmploye(Long etab, EmployeRequest req) {
        Etablissement e = verrouiller(etab);
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (utilisateurs.existsByEmailIgnoreCase(email)) throw Introuvable.conflit("Un compte existe déjà avec cet e-mail");
        // Le poste identifie qui a appelé un client : deux personnes ne peuvent pas partager le même.
        String poste = req.poste().trim();
        if (utilisateurs.findByEtablissementIdOrderByPosteAsc(etab).stream().anyMatch(u -> poste.equalsIgnoreCase(u.getPoste())))
            throw Introuvable.conflit("Ce poste est déjà attribué");
        Utilisateur u = utilisateurs.save(new Utilisateur(email, encodeur.encode(req.motDePasse()), req.nom().trim(), Role.EMPLOYE, e, poste));
        tempsReel.fileModifiee(etab);
        return vue(u);
    }

    @Transactional
    public void retirerEmploye(Long etab, Long id) {
        verrouiller(etab);
        Utilisateur u = utilisateurs.findById(id)
                .filter(x -> x.getEtablissement() != null && x.getEtablissement().getId().equals(etab))
                .orElseThrow(() -> Introuvable.de("Employé"));
        if (u.getRole() != Role.EMPLOYE) throw Introuvable.conflit("Le compte d'un gérant ne peut pas être retiré ici");
        if (tickets.findFirstByEtablissementIdAndPosteAndStatut(etab, u.getPoste() + " · " + u.getNom(), StatutTicket.APPELE).isPresent())
            throw Introuvable.conflit("Cet employé sert un client : terminez d'abord ce ticket");
        utilisateurs.delete(u);
        tempsReel.fileModifiee(etab);
    }

    // ---- paramètres

    @Transactional(readOnly = true)
    public Parametres parametres(Long etab) {
        return vue(etablissement(etab));
    }

    @Transactional
    public Parametres modifierParametres(Long etab, Parametres req) {
        if (req.heureFermeture() <= req.heureOuverture()) throw Introuvable.requete("La fermeture doit être après l'ouverture");
        Etablissement e = verrouiller(etab);
        e.setNom(req.nom().trim());
        e.setTelephone(req.telephone());
        e.setAdresse(req.adresse().trim());
        e.setQuartier(req.quartier());
        e.setDescription(req.description());
        e.setHoraires(req.horaires());
        e.setHeureOuverture(req.heureOuverture());
        e.setHeureFermeture(req.heureFermeture());
        tempsReel.fileModifiee(etab);
        return vue(e);
    }

    private Etablissement etablissement(Long id) {
        return etablissements.findById(id).orElseThrow(() -> Introuvable.de("Établissement"));
    }

    private Etablissement verrouiller(Long id) {
        return etablissements.verrouiller(id).orElseThrow(() -> Introuvable.de("Établissement"));
    }

    private Prestation service(Long etab, Long id) {
        return prestations.findById(id).filter(p -> !p.isRetiree() && p.getEtablissement().getId().equals(etab))
                .orElseThrow(() -> Introuvable.de("Service"));
    }

    private static PrestationDto vue(Prestation p) {
        return new PrestationDto(p.getId(), p.getNom(), p.getDescription(), p.getDureeMinutes(), p.getPrix());
    }

    private static Employe vue(Utilisateur u) {
        return new Employe(u.getId(), u.getNom(), u.getEmail(), u.getRole().name(), u.getPoste());
    }

    private static Parametres vue(Etablissement e) {
        return new Parametres(e.getNom(), e.getTelephone(), e.getAdresse(), e.getQuartier(), e.getDescription(),
                e.getHoraires(), e.getHeureOuverture(), e.getHeureFermeture());
    }
}
