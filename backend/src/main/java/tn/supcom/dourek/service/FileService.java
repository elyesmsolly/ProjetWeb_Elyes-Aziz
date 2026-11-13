package tn.supcom.dourek.service;

import tn.supcom.dourek.model.Utilisateur;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.supcom.dourek.dto.Dtos.*;
import tn.supcom.dourek.model.*;
import tn.supcom.dourek.repository.*;

/**
 * Cœur de Dourek : prise de ticket, ordre de passage, appel du suivant, estimation de l'attente.
 * Toutes les opérations qui modifient une file verrouillent d'abord la ligne de l'établissement.
 */
@Service
public class FileService {

    public static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");
    /** Règle anti-famine : un prioritaire au plus tous les 2 clients normaux. */
    public static final int NORMAUX_PAR_PRIORITAIRE = 2;

    private final EtablissementRepository etablissements;
    private final TicketRepository tickets;
    private final PrestationRepository prestations;
    private final FormulaireRepository formulaires;
    private final UtilisateurRepository utilisateurs;
    private final FormulaireService formulaireService;
    private final TempsReel tempsReel;

    public FileService(EtablissementRepository etablissements, TicketRepository tickets,
                       PrestationRepository prestations, FormulaireRepository formulaires,
                       UtilisateurRepository utilisateurs, FormulaireService formulaireService, TempsReel tempsReel) {
        this.etablissements = etablissements;
        this.tickets = tickets;
        this.prestations = prestations;
        this.formulaires = formulaires;
        this.utilisateurs = utilisateurs;
        this.formulaireService = formulaireService;
        this.tempsReel = tempsReel;
    }

    // ------------------------------------------------------------------ ordre et estimation

    /**
     * Ordre de passage prévu : on simule la règle « 1 prioritaire pour 2 normaux »
     * à partir de l'état actuel du compteur. C'est aussi l'ordre utilisé par « Appeler le suivant ».
     */
    public static List<Ticket> ordreDePassage(Etablissement e, List<Ticket> enAttente) {
        Deque<Ticket> prio = new ArrayDeque<>();
        Deque<Ticket> normaux = new ArrayDeque<>();
        for (Ticket t : enAttente) (t.isPrioritaire() ? prio : normaux).add(t);
        int depuis = e.getNormauxDepuisPrioritaire();
        List<Ticket> ordre = new ArrayList<>(enAttente.size());
        while (!prio.isEmpty() || !normaux.isEmpty()) {
            if (!prio.isEmpty() && (normaux.isEmpty() || depuis >= NORMAUX_PAR_PRIORITAIRE)) {
                ordre.add(prio.poll());
                depuis = 0;
            } else {
                ordre.add(normaux.poll());
                depuis++;
            }
        }
        return ordre;
    }

    /** Durée moyenne d'un passage : moyenne mobile des 20 derniers services, sinon durée des prestations. */
    public double dureeMoyenne(Etablissement e) {
        List<Ticket> derniers = tickets.findTop20ByEtablissementIdAndStatutAndTermineLeNotNullOrderByTermineLeDesc(
                e.getId(), StatutTicket.SERVI);
        List<Double> durees = derniers.stream()
                .filter(t -> t.getAppeleLe() != null)
                .map(t -> Duration.between(t.getAppeleLe(), t.getTermineLe()).toSeconds() / 60.0)
                .filter(d -> d >= 1 && d <= 180)
                .toList();
        if (durees.size() >= 3) return durees.stream().mapToDouble(Double::doubleValue).average().orElse(15);
        return e.getPrestations().stream().mapToInt(Prestation::getDureeMinutes).average().orElse(15);
    }

    /** Version 1 de l'estimation : (personnes devant × durée moyenne) / postes ouverts. */
    public static int estimerAttente(int personnesDevant, double dureeMoyenne, int postes) {
        if (personnesDevant <= 0) return 0;
        return (int) Math.ceil(personnesDevant * dureeMoyenne / Math.max(1, postes));
    }

    public List<Ticket> enAttente(Long etablissementId) {
        return tickets.findByEtablissementIdAndStatutOrderByOrdreAsc(etablissementId, StatutTicket.EN_ATTENTE);
    }

    // ------------------------------------------------------------------ client

    @Transactional
    public TicketCree prendreTicket(String slug, PriseTicketRequest req, String emailClient) {
        Utilisateur client = utilisateurs.findByEmailIgnoreCase(emailClient)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connectez-vous pour prendre un ticket"));
        Long id = etablissements.findBySlug(slug).orElseThrow(() -> Introuvable.de("Établissement")).getId();
        Etablissement e = etablissements.verrouiller(id).orElseThrow();
        if (!e.isFileOuverte()) throw Introuvable.conflit("La file de cet établissement est en pause");

        Prestation p = prestations.findById(req.prestationId())
                .filter(x -> x.getEtablissement().getId().equals(e.getId()))
                .orElseThrow(() -> Introuvable.requete("Prestation inconnue"));

        Formulaire f = formulaires.findFirstByEtablissementIdOrderByVersionDesc(e.getId()).orElse(null);
        List<ChampFormulaire> champs = f == null ? FormulaireService.champsParDefaut() : formulaireService.lireChamps(f.getChampsJson());
        Map<String, Object> reponses = req.reponses() == null ? Map.of() : req.reponses();
        formulaireService.validerReponses(champs, reponses);

        Ticket t = nouveauTicket(e, p, Boolean.TRUE.equals(req.prioritaire()));
        t.setClient(client);
        t.setNomClient(texte(reponses.get("nom"), client.getNom()));
        t.setTelephone(texte(reponses.get("telephone"), client.getTelephone()));
        t.setReponsesJson(formulaireService.ecrire(reponses));
        t.setFormulaireVersion(f == null ? 0 : f.getVersion());
        tickets.save(t);
        tempsReel.fileModifiee(e.getId());
        return new TicketCree(t.getCode(), t.getNumero());
    }

    private Ticket nouveauTicket(Etablissement e, Prestation p, boolean prioritaire) {
        Ticket t = new Ticket();
        t.setCode(UUID.randomUUID().toString());
        t.setEtablissement(e);
        t.setPrestation(p);
        t.setPrioritaire(prioritaire);
        if (prioritaire) {
            e.setCompteurPrioritaire(e.getCompteurPrioritaire() + 1);
            t.setNumero(String.format("P-%02d", e.getCompteurPrioritaire()));
        } else {
            e.setCompteurNormal(e.getCompteurNormal() + 1);
            t.setNumero(String.format("%s-%02d", e.getCategorie().getPrefixe(), e.getCompteurNormal()));
        }
        t.setOrdre(tickets.ordreMax(e.getId()) + 1);
        return t;
    }

    private static String texte(Object v, String parDefaut) {
        return v == null || v.toString().isBlank() ? parDefaut : v.toString().trim();
    }

    /** Un client ne voit et ne modifie que ses propres tickets. */
    @Transactional(readOnly = true)
    public void verifierProprietaire(String code, String emailClient) {
        Ticket t = tickets.findByCode(code).orElseThrow(() -> Introuvable.de("Ticket"));
        if (t.getClient() == null || !t.getClient().getEmail().equalsIgnoreCase(emailClient))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce ticket appartient à un autre compte");
    }

    /** Tickets du client connecté, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<TicketVue> mesTickets(String emailClient) {
        return tickets.findTop20ByClientEmailOrderByCreeLeDesc(emailClient).stream().map(t -> vue(t.getCode())).toList();
    }

    @Transactional(readOnly = true)
    public TicketVue vue(String code) {
        Ticket t = tickets.findByCode(code).orElseThrow(() -> Introuvable.de("Ticket"));
        Etablissement e = t.getEtablissement();
        List<Ticket> ordre = ordreDePassage(e, enAttente(e.getId()));
        int position = ordre.indexOf(t);
        int devant = Math.max(position, 0);
        double duree = dureeMoyenne(e);
        int attente = t.getStatut() == StatutTicket.EN_ATTENTE ? estimerAttente(devant, duree, e.getPostesOuverts()) : 0;

        List<PastilleFile> pastilles = new ArrayList<>();
        List<Ticket> enCours = tickets.findByEtablissementIdAndStatutOrderByAppeleLeDesc(e.getId(), StatutTicket.APPELE);
        enCours.stream().limit(2).toList().reversed()
                .forEach(c -> pastilles.add(new PastilleFile(c.getNumero(), c == t || c.getId().equals(t.getId()) ? "MOI" : "SERVI_EN_COURS")));
        int debut = Math.max(0, position - 4);
        int fin = Math.min(ordre.size(), Math.max(position, 0) + 3);
        for (int i = debut; i < fin; i++) {
            Ticket o = ordre.get(i);
            pastilles.add(new PastilleFile(o.getNumero(), o.getId().equals(t.getId()) ? "MOI" : "ATTENTE"));
        }

        return new TicketVue(t.getCode(), t.getNumero(), t.getStatut().name(),
                e.getId(), e.getSlug(), e.getNom(), e.getAdresse(), e.getLatitude(), e.getLongitude(),
                t.getPrestation() == null ? null : t.getPrestation().getNom(),
                devant, attente, Instant.now().plus(Duration.ofMinutes(attente)),
                t.getCreeLe(), t.getAppeleLe(), t.getTermineLe(), t.getPoste(),
                t.getFormulaireVersion(), t.isPrioritaire(), pastilles);
    }

    @Transactional
    public TicketVue annuler(String code) {
        Ticket t = tickets.findByCode(code).orElseThrow(() -> Introuvable.de("Ticket"));
        etablissements.verrouiller(t.getEtablissement().getId());
        if (t.getStatut() != StatutTicket.EN_ATTENTE) throw Introuvable.conflit("Ce ticket n'est plus en attente");
        t.setStatut(StatutTicket.ANNULE);
        t.setTermineLe(Instant.now());
        tempsReel.fileModifiee(t.getEtablissement().getId());
        return vue(code);
    }

    /** « Laisser passer 1 personne » : le client recule d'une place sans perdre son ticket. */
    @Transactional
    public TicketVue laisserPasser(String code) {
        Ticket t = tickets.findByCode(code).orElseThrow(() -> Introuvable.de("Ticket"));
        Etablissement e = etablissements.verrouiller(t.getEtablissement().getId()).orElseThrow();
        if (t.getStatut() != StatutTicket.EN_ATTENTE) throw Introuvable.conflit("Ce ticket n'est plus en attente");
        List<Ticket> memeType = enAttente(e.getId()).stream().filter(x -> x.isPrioritaire() == t.isPrioritaire()).toList();
        int i = memeType.indexOf(t);
        if (i < 0 || i == memeType.size() - 1) throw Introuvable.conflit("Personne derrière vous : vous êtes déjà le dernier");
        Ticket suivant = memeType.get(i + 1);
        long o = t.getOrdre();
        t.setOrdre(suivant.getOrdre());
        suivant.setOrdre(o);
        tempsReel.fileModifiee(e.getId());
        return vue(code);
    }

    // ------------------------------------------------------------------ employé

    /**
     * « Appeler le suivant » : termine le client en cours du poste (servi) puis appelle
     * le prochain selon l'ordre de passage. Le verrou garantit qu'un client n'est jamais
     * appelé par deux postes à la fois.
     */
    @Transactional
    public EtatFile appelerSuivant(Long etablissementId, String poste) {
        Etablissement e = etablissements.verrouiller(etablissementId).orElseThrow();
        tickets.findFirstByEtablissementIdAndPosteAndStatut(e.getId(), poste, StatutTicket.APPELE)
                .ifPresent(c -> terminerTicket(c, StatutTicket.SERVI));
        List<Ticket> ordre = ordreDePassage(e, enAttente(e.getId()));
        if (!ordre.isEmpty()) {
            Ticket suivant = ordre.getFirst();
            suivant.setStatut(StatutTicket.APPELE);
            suivant.setAppeleLe(Instant.now());
            suivant.setPoste(poste);
            e.setNormauxDepuisPrioritaire(suivant.isPrioritaire() ? 0 : e.getNormauxDepuisPrioritaire() + 1);
        }
        tempsReel.fileModifiee(e.getId());
        return etat(e.getId());
    }

    @Transactional
    public EtatFile terminer(Long etablissementId, String poste, StatutTicket statut) {
        etablissements.verrouiller(etablissementId).orElseThrow();
        Ticket c = tickets.findFirstByEtablissementIdAndPosteAndStatut(etablissementId, poste, StatutTicket.APPELE)
                .orElseThrow(() -> Introuvable.conflit("Aucun client en cours à ce poste"));
        terminerTicket(c, statut);
        tempsReel.fileModifiee(etablissementId);
        return etat(etablissementId);
    }

    private void terminerTicket(Ticket t, StatutTicket statut) {
        t.setStatut(statut);
        t.setTermineLe(Instant.now());
    }

    @Transactional
    public EtatFile ajouterSurPlace(Long etablissementId, AjoutSurPlaceRequest req) {
        Etablissement e = etablissements.verrouiller(etablissementId).orElseThrow();
        Prestation p = prestations.findById(req.prestationId())
                .filter(x -> x.getEtablissement().getId().equals(e.getId()))
                .orElseThrow(() -> Introuvable.requete("Prestation inconnue"));
        Ticket t = nouveauTicket(e, p, Boolean.TRUE.equals(req.prioritaire()));
        t.setNomClient(req.nom().trim());
        t.setSurPlace(true);
        t.setReponsesJson("{}");
        tickets.save(t);
        tempsReel.fileModifiee(e.getId());
        return etat(e.getId());
    }

    @Transactional
    public EtatFile changerOuverture(Long etablissementId, boolean ouverte) {
        Etablissement e = etablissements.verrouiller(etablissementId).orElseThrow();
        e.setFileOuverte(ouverte);
        tempsReel.fileModifiee(e.getId());
        return etat(e.getId());
    }

    // ------------------------------------------------------------------ vues pro

    @Transactional(readOnly = true)
    public EtatFile etat(Long etablissementId) {
        Etablissement e = etablissements.findById(etablissementId).orElseThrow(() -> Introuvable.de("Établissement"));
        double duree = dureeMoyenne(e);
        List<Ticket> ordre = ordreDePassage(e, enAttente(e.getId()));
        Map<Integer, List<ChampFormulaire>> cache = new HashMap<>();
        List<TicketLigne> attente = new ArrayList<>();
        for (int i = 0; i < ordre.size(); i++) {
            attente.add(ligne(ordre.get(i), estimerAttente(i, duree, e.getPostesOuverts()), cache));
        }
        List<TicketLigne> enCours = tickets.findByEtablissementIdAndStatutOrderByAppeleLeDesc(e.getId(), StatutTicket.APPELE)
                .stream().map(t -> ligne(t, 0, cache)).toList();
        List<PrestationDto> prest = e.getPrestations().stream()
                .map(p -> new PrestationDto(p.getId(), p.getNom(), p.getDescription(), p.getDureeMinutes(), p.getPrix())).toList();
        return new EtatFile(e.getId(), e.getNom(), e.isFileOuverte(), e.getPostesOuverts(),
                (int) Math.round(duree), enCours, attente, prest);
    }

    TicketLigne ligne(Ticket t, int attenteEstimee, Map<Integer, List<ChampFormulaire>> cache) {
        List<ChampFormulaire> champs = cache.computeIfAbsent(t.getFormulaireVersion(), v -> formulaires
                .findByEtablissementIdAndVersion(t.getEtablissement().getId(), v)
                .map(f -> formulaireService.lireChamps(f.getChampsJson()))
                .orElse(FormulaireService.champsParDefaut()));
        return new TicketLigne(t.getCode(), t.getNumero(), t.getNomClient(),
                t.getPrestation() == null ? null : t.getPrestation().getNom(), t.getStatut().name(),
                t.isPrioritaire(), t.isSurPlace(), t.getCreeLe(), t.getAppeleLe(), attenteEstimee, t.getPoste(),
                formulaireService.lireReponses(t.getReponsesJson()), champs);
    }

    @Transactional(readOnly = true)
    public Statistiques statistiques(Long etablissementId) {
        Etablissement e = etablissements.findById(etablissementId).orElseThrow(() -> Introuvable.de("Établissement"));
        Instant minuit = LocalDate.now(TUNIS).atStartOfDay(TUNIS).toInstant();
        List<Ticket> duJour = tickets.findByEtablissementIdAndCreeLeAfterOrderByCreeLeAsc(e.getId(), minuit);

        int servis = (int) duJour.stream().filter(t -> t.getStatut() == StatutTicket.SERVI).count();
        int absents = (int) duJour.stream().filter(t -> t.getStatut() == StatutTicket.ABSENT).count();
        int enAttente = (int) duJour.stream().filter(t -> t.getStatut() == StatutTicket.EN_ATTENTE).count();
        int attenteMoy = (int) Math.round(duJour.stream().filter(t -> t.getAppeleLe() != null)
                .mapToLong(t -> Duration.between(t.getCreeLe(), t.getAppeleLe()).toMinutes()).average().orElse(0));
        int tauxAbsence = servis + absents == 0 ? 0 : Math.round(100f * absents / (servis + absents));

        int[] parHeure = new int[24];
        duJour.forEach(t -> parHeure[ZonedDateTime.ofInstant(t.getCreeLe(), TUNIS).getHour()]++);
        List<AffluenceHeure> affluence = new ArrayList<>();
        for (int h = e.getHeureOuverture(); h < e.getHeureFermeture(); h++) affluence.add(new AffluenceHeure(h, parHeure[h]));

        List<Ticket> appeles = tickets.findByEtablissementIdAndStatutOrderByAppeleLeDesc(e.getId(), StatutTicket.APPELE);
        List<PosteEtat> postes = utilisateurs.findByEtablissementIdOrderByPosteAsc(e.getId()).stream()
                .filter(u -> u.getPoste() != null)
                .map(u -> appeles.stream().filter(t -> t.getPoste() != null && t.getPoste().startsWith(u.getPoste() + " ·")).findFirst()
                        .map(t -> new PosteEtat(u.getPoste(), u.getNom(), t.getNumero(),
                                t.getPrestation() == null ? null : t.getPrestation().getNom(),
                                (int) Duration.between(t.getAppeleLe(), Instant.now()).toMinutes()))
                        .orElse(new PosteEtat(u.getPoste(), u.getNom(), null, null, null)))
                .toList();

        Map<Integer, List<ChampFormulaire>> cache = new HashMap<>();
        List<TicketLigne> derniers = duJour.stream()
                .filter(t -> t.getStatut() == StatutTicket.ABSENT || t.getStatut() == StatutTicket.SERVI)
                .sorted((a, b) -> b.getTermineLe().compareTo(a.getTermineLe()))
                .limit(5).map(t -> ligne(t, 0, cache)).toList();

        return new Statistiques(servis, enAttente, attenteMoy, tauxAbsence, absents, 0, affluence, postes, derniers);
    }
}
