package tn.supcom.dourek.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.supcom.dourek.dto.GestionDtos.*;
import tn.supcom.dourek.model.*;
import tn.supcom.dourek.repository.*;

/** Branche « Administration » du sitemap. */
@Service
public class AdminService {

    private final EtablissementRepository etablissements;
    private final UtilisateurRepository utilisateurs;
    private final AvisRepository avis;
    private final MessageContactRepository messages;
    private final TempsReel tempsReel;

    public AdminService(EtablissementRepository etablissements, UtilisateurRepository utilisateurs,
                        AvisRepository avis, MessageContactRepository messages, TempsReel tempsReel) {
        this.etablissements = etablissements;
        this.utilisateurs = utilisateurs;
        this.avis = avis;
        this.messages = messages;
        this.tempsReel = tempsReel;
    }

    @Transactional(readOnly = true)
    public List<EtablissementAdmin> etablissements() {
        return etablissements.findAll().stream().sorted(Comparator.comparing(Etablissement::getNom))
                .map(e -> new EtablissementAdmin(e.getId(), e.getSlug(), e.getNom(), e.libelleCategorie(),
                        e.getQuartier(), e.isFileOuverte(), e.isSuspendu()))
                .toList();
    }

    @Transactional
    public void suspendreEtablissement(Long id, boolean suspendu) {
        etablissements.verrouiller(id).orElseThrow(() -> Introuvable.de("Établissement")).setSuspendu(suspendu);
        tempsReel.fileModifiee(id);
    }

    @Transactional(readOnly = true)
    public List<UtilisateurAdmin> utilisateurs() {
        return utilisateurs.findAll().stream().sorted(Comparator.comparing(Utilisateur::getNom))
                .map(u -> new UtilisateurAdmin(u.getId(), u.getNom(), u.getEmail(), u.getRole().name(),
                        u.getEtablissement() == null ? null : u.getEtablissement().getNom(), u.isSuspendu()))
                .toList();
    }

    @Transactional
    public void suspendreUtilisateur(Long id, boolean suspendu) {
        Utilisateur u = utilisateur(id);
        if (u.getRole() == Role.ADMIN) throw Introuvable.conflit("Un administrateur ne peut pas être suspendu");
        u.setSuspendu(suspendu);
    }

    /** Seuls gérant et employé s'échangent, et un établissement garde toujours un gérant. */
    @Transactional
    public void changerRole(Long id, String role) {
        Utilisateur u = utilisateur(id);
        if (!role.equals("GERANT") && !role.equals("EMPLOYE")) throw Introuvable.requete("Rôle non autorisé");
        if (u.getEtablissement() == null) throw Introuvable.conflit("Ce compte n'est rattaché à aucun établissement");
        Long etab = u.getEtablissement().getId();
        etablissements.verrouiller(etab).orElseThrow();
        if (u.getRole() == Role.GERANT && role.equals("EMPLOYE")
                && utilisateurs.findByEtablissementIdOrderByPosteAsc(etab).stream().filter(x -> x.getRole() == Role.GERANT).count() <= 1)
            throw Introuvable.conflit("Un établissement doit garder au moins un gérant");
        u.setRole(Role.valueOf(role));
    }

    @Transactional(readOnly = true)
    public List<AvisVue> avisSignales() {
        return avis.findBySignaleTrueOrderByCreeLeDesc().stream().map(AvisService::vue).toList();
    }

    @Transactional
    public void conserverAvis(Long id) {
        avis.findById(id).orElseThrow(() -> Introuvable.de("Avis")).setSignale(false);
    }

    @Transactional
    public void supprimerAvis(Long id) {
        Avis a = avis.findById(id).orElseThrow(() -> Introuvable.de("Avis"));
        Etablissement e = etablissements.verrouiller(a.getEtablissement().getId()).orElseThrow();
        e.setNote(AvisService.moyenneSans(e.getNote(), e.getNombreAvis(), a.getNote()));
        e.setNombreAvis(Math.max(0, e.getNombreAvis() - 1));
        avis.delete(a);
    }

    @Transactional(readOnly = true)
    public List<CategorieAdmin> categories() {
        List<Etablissement> tous = etablissements.findAll();
        List<CategorieAdmin> res = new ArrayList<>(Arrays.stream(Categorie.values())
                .map(c -> new CategorieAdmin(c.getLibelle(), tous.stream().filter(e -> e.getCategorie() == c).count(), false))
                .toList());
        tous.stream().filter(e -> e.getCategorie() == Categorie.AUTRE && e.getCategorieLibre() != null)
                .collect(Collectors.groupingBy(e -> RechercheService.normaliser(e.getCategorieLibre())))
                .values().forEach(g -> res.add(new CategorieAdmin(g.getFirst().getCategorieLibre(), g.size(), true)));
        return res;
    }

    /** Renommer un type libre ; lui donner le nom d'un autre les fusionne. */
    @Transactional
    public void renommerCategorie(RenommageRequest req) {
        String cle = RechercheService.normaliser(req.ancien());
        List<Etablissement> cibles = etablissements.findAll().stream()
                .filter(e -> e.getCategorie() == Categorie.AUTRE && e.getCategorieLibre() != null)
                .filter(e -> RechercheService.normaliser(e.getCategorieLibre()).equals(cle))
                .toList();
        if (cibles.isEmpty()) throw Introuvable.de("Catégorie");
        cibles.forEach(e -> e.setCategorieLibre(req.nouveau().trim()));
    }

    @Transactional(readOnly = true)
    public List<MessageVue> messages() {
        return messages.findTop100ByOrderByCreeLeDesc().stream()
                .map(m -> new MessageVue(m.getId(), m.getNom(), m.getEmail(), m.getSujet(), m.getMessage(), m.getCreeLe()))
                .toList();
    }

    @Transactional
    public void supprimerMessage(Long id) {
        messages.delete(messages.findById(id).orElseThrow(() -> Introuvable.de("Message")));
    }

    private Utilisateur utilisateur(Long id) {
        return utilisateurs.findById(id).orElseThrow(() -> Introuvable.de("Utilisateur"));
    }
}
