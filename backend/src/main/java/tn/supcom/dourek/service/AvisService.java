package tn.supcom.dourek.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.supcom.dourek.dto.GestionDtos.*;
import tn.supcom.dourek.model.*;
import tn.supcom.dourek.repository.*;

/** Avis des clients : seulement après un service, un seul par ticket. */
@Service
public class AvisService {

    private final AvisRepository avis;
    private final TicketRepository tickets;
    private final EtablissementRepository etablissements;

    public AvisService(AvisRepository avis, TicketRepository tickets, EtablissementRepository etablissements) {
        this.avis = avis;
        this.tickets = tickets;
        this.etablissements = etablissements;
    }

    @Transactional
    public AvisVue donner(String code, AvisRequest req) {
        Ticket t = tickets.findByCode(code).orElseThrow(() -> Introuvable.de("Ticket"));
        // Le verrou passe avant le contrôle : deux envois simultanés ne comptent qu'une fois.
        Etablissement e = etablissements.verrouiller(t.getEtablissement().getId()).orElseThrow();
        if (t.getStatut() != StatutTicket.SERVI) throw Introuvable.conflit("Vous pourrez donner votre avis une fois servi");
        if (avis.findByTicketCode(code).isPresent()) throw Introuvable.conflit("Vous avez déjà donné votre avis pour ce ticket");
        String texte = req.commentaire() == null || req.commentaire().isBlank() ? null : req.commentaire().trim();
        Avis a = avis.save(new Avis(t, req.note(), texte));
        e.setNote(moyenneAvec(e.getNote(), e.getNombreAvis(), req.note()));
        e.setNombreAvis(e.getNombreAvis() + 1);
        return vue(a);
    }

    @Transactional(readOnly = true)
    public Optional<AvisVue> duTicket(String code) {
        return avis.findByTicketCode(code).map(AvisService::vue);
    }

    @Transactional(readOnly = true)
    public List<AvisVue> deLEtablissement(String slug) {
        return avis.findTop20ByEtablissementSlugOrderByCreeLeDesc(slug).stream().map(AvisService::vue).toList();
    }

    @Transactional
    public void signaler(Long id) {
        avis.findById(id).orElseThrow(() -> Introuvable.de("Avis")).setSignale(true);
    }

    static AvisVue vue(Avis a) {
        Utilisateur auteur = a.getTicket().getClient();
        return new AvisVue(a.getId(), a.getNote(), a.getCommentaire(), nomPublic(auteur == null ? null : auteur.getNom()),
                a.getCreeLe(), a.isSignale(), a.getEtablissement().getNom());
    }

    /** « Yassine Trabelsi » devient « Yassine T. » : le nom complet n'est jamais public. */
    static String nomPublic(String nom) {
        if (nom == null || nom.isBlank()) return "Client";
        String[] mots = nom.trim().split("\\s+");
        return mots.length == 1 ? mots[0] : mots[0] + " " + mots[mots.length - 1].charAt(0) + ".";
    }

    static double moyenneAvec(double moyenne, int nombre, int note) {
        return (moyenne * nombre + note) / (nombre + 1);
    }

    static double moyenneSans(double moyenne, int nombre, int note) {
        return nombre <= 1 ? 0 : Math.max(0, Math.min(5, (moyenne * nombre - note) / (nombre - 1)));
    }
}
