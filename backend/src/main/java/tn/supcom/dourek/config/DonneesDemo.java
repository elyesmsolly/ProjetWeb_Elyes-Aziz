package tn.supcom.dourek.config;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.supcom.dourek.dto.Dtos.ChampFormulaire;
import tn.supcom.dourek.model.*;
import tn.supcom.dourek.repository.*;
import tn.supcom.dourek.service.FileService;
import tn.supcom.dourek.service.FormulaireService;

/**
 * Données de démonstration (quartier de l'Ariana), insérées au premier lancement
 * seulement, quand la base est vide.
 */
@Component
public class DonneesDemo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DonneesDemo.class);
    public static final String MOT_DE_PASSE = "dourek123";

    private final EtablissementRepository etablissements;
    private final UtilisateurRepository utilisateurs;
    private final TicketRepository tickets;
    private final FormulaireService formulaires;
    private final PasswordEncoder encodeur;

    public DonneesDemo(EtablissementRepository etablissements, UtilisateurRepository utilisateurs,
                       TicketRepository tickets, FormulaireService formulaires, PasswordEncoder encodeur) {
        this.etablissements = etablissements;
        this.utilisateurs = utilisateurs;
        this.tickets = tickets;
        this.formulaires = formulaires;
        this.encodeur = encodeur;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (etablissements.count() > 0) return;

        Etablissement karim = etab("salon-karim", "Salon Karim", Categorie.BARBIER, "Rue Mustapha Mohsen, Ariana", "Ariana",
                36.8641, 10.1949, 4.7, 128, 9, 20, "Lun – Sam : 9 h – 20 h\nDimanche : fermé", 2,
                "Barbier de quartier depuis 2012 : coupes, dégradés et barbe à l'ancienne.");
        karim.ajouterPrestation(new Prestation("Coupe homme", "Shampoing compris", 20, 15));
        karim.ajouterPrestation(new Prestation("Barbe", "Taille et contours", 10, 8));
        karim.ajouterPrestation(new Prestation("Coupe + barbe", "Formule complète", 30, 20));
        karim.ajouterPrestation(new Prestation("Coupe enfant", "Moins de 12 ans", 15, 10));

        Etablissement ennasr = etab("barber-ennasr", "Barber Ennasr", Categorie.BARBIER, "Avenue Hédi Nouira, Ennasr 2", "Ennasr",
                36.8562, 10.1712, 4.5, 86, 9, 21, "Tous les jours : 9 h – 21 h", 1, "Salon moderne, sans rendez-vous.");
        ennasr.ajouterPrestation(new Prestation("Coupe homme", "Avec coiffage", 25, 12));
        ennasr.ajouterPrestation(new Prestation("Barbe", "Rasage à la serviette chaude", 15, 7));

        Etablissement style = etab("coiff-style-ariana", "Coiff’ Style Ariana", Categorie.BARBIER, "Rue de la Mosquée, Ariana Ville", "Ariana",
                36.8705, 10.2010, 4.6, 54, 9, 19, "Lun – Sam : 9 h – 19 h", 3, "Trois fauteuils, attente courte.");
        style.ajouterPrestation(new Prestation("Coupe homme", "Coupe classique", 20, 14));
        style.ajouterPrestation(new Prestation("Coupe + barbe", "Formule complète", 30, 19));

        Etablissement b2080 = etab("barbershop-2080", "Barbershop 2080", Categorie.BARBIER, "Cité 2080, Ariana", "Ariana",
                36.8520, 10.2075, 4.3, 40, 10, 20, "Mar – Dim : 10 h – 20 h", 1, "Coupes tendance.");
        b2080.ajouterPrestation(new Prestation("Coupe homme", "", 20, 12));
        b2080.setFileOuverte(false);

        Etablissement lina = etab("salon-lina", "Salon Lina", Categorie.COIFFURE_FEMME, "Avenue de l'Indépendance, Ariana", "Ariana",
                36.8668, 10.1905, 4.8, 97, 9, 19, "Lun – Sam : 9 h – 19 h", 2, "Coiffure, brushing et couleur.");
        lina.ajouterPrestation(new Prestation("Brushing", "Cheveux mi-longs", 30, 20));
        lina.ajouterPrestation(new Prestation("Coupe femme", "Shampoing et coupe", 40, 30));
        lina.ajouterPrestation(new Prestation("Couleur", "Racines", 60, 45));

        Etablissement labo = etab("labo-el-menzah", "Labo El Menzah", Categorie.LABORATOIRE, "Avenue Othman Ibn Affane, El Menzah 6", "El Menzah",
                36.8508, 10.1850, 4.8, 211, 7, 18, "Lun – Sam : 7 h – 18 h", 2, "Analyses médicales, résultats en ligne.");
        labo.ajouterPrestation(new Prestation("Prise de sang", "À jeun de préférence", 8, 0));
        labo.ajouterPrestation(new Prestation("Dépôt d'échantillon", "", 4, 0));
        labo.ajouterPrestation(new Prestation("Retrait de résultats", "", 3, 0));

        Etablissement agence = etab("agence-ariana-centre", "Agence Ariana Centre", Categorie.AGENCE, "Avenue Habib Bourguiba, Ariana", "Ariana",
                36.8600, 10.1930, 4.1, 63, 8, 17, "Lun – Ven : 8 h – 17 h", 3, "Agence de services (factures, abonnements).");
        agence.ajouterPrestation(new Prestation("Paiement de facture", "", 5, 0));
        agence.ajouterPrestation(new Prestation("Nouvel abonnement", "", 20, 0));

        Etablissement mairie = etab("municipalite-ariana", "Municipalité de l'Ariana", Categorie.ADMINISTRATION, "Place de la Mairie, Ariana", "Ariana",
                36.8653, 10.1638, 3.9, 120, 8, 16, "Lun – Ven : 8 h – 16 h", 2, "Légalisation de signature, extraits de naissance.");
        mairie.ajouterPrestation(new Prestation("Légalisation de signature", "", 5, 0));
        mairie.ajouterPrestation(new Prestation("Extrait de naissance", "", 7, 0));

        List<Etablissement> tous = List.of(karim, ennasr, style, b2080, lina, labo, agence, mairie);
        etablissements.saveAll(tous);

        // Formulaire du Salon Karim (version 3, comme dans la maquette)
        formulaires.publier(karim.getId(), FormulaireService.champsParDefaut());
        formulaires.publier(karim.getId(), List.of(
                FormulaireService.champsParDefaut().get(0), FormulaireService.champsParDefaut().get(1),
                new ChampFormulaire("coiffeur", "CHOIX", "Coiffeur préféré", false, List.of("Peu importe", "Karim"), null)));
        formulaires.publier(karim.getId(), List.of(
                FormulaireService.champsParDefaut().get(0), FormulaireService.champsParDefaut().get(1),
                new ChampFormulaire("coiffeur", "CHOIX", "Coiffeur préféré", false, List.of("Peu importe", "Karim", "Sami"), null),
                new ChampFormulaire("remarque", "TEXTE_LONG", "Remarque pour le coiffeur", false, List.of(), "Ex. dégradé court sur les côtés")));
        for (Etablissement e : tous) if (e != karim) formulaires.publier(e.getId(), FormulaireService.champsParDefaut());

        String mdp = encodeur.encode(MOT_DE_PASSE);
        utilisateurs.save(new Utilisateur("karim@dourek.tn", mdp, "Karim", Role.GERANT, karim, "Poste 1"));
        utilisateurs.save(new Utilisateur("sami@dourek.tn", mdp, "Sami", Role.EMPLOYE, karim, "Poste 2"));
        utilisateurs.save(new Utilisateur("labo@dourek.tn", mdp, "Dr Ben Ali", Role.GERANT, labo, "Poste 1"));

        remplirFiles(tous, karim);
        log.info("Données de démonstration créées : {} établissements. Connexion pro : karim@dourek.tn / {}", tous.size(), MOT_DE_PASSE);
    }

    private Etablissement etab(String slug, String nom, Categorie cat, String adresse, String quartier, double lat, double lng,
                               double note, int avis, int ouv, int ferm, String horaires, int postes, String description) {
        Etablissement e = new Etablissement();
        e.setSlug(slug);
        e.setNom(nom);
        e.setCategorie(cat);
        e.setAdresse(adresse);
        e.setQuartier(quartier);
        e.setLatitude(lat);
        e.setLongitude(lng);
        e.setNote(note);
        e.setNombreAvis(avis);
        e.setHeureOuverture(ouv);
        e.setHeureFermeture(ferm);
        e.setHoraires(horaires);
        e.setPostesOuverts(postes);
        e.setDescription(description);
        e.setTelephone("+216 71 000 000");
        return e;
    }

    /** Une journée déjà entamée : clients servis, absents, en cours et en attente. */
    private void remplirFiles(List<Etablissement> tous, Etablissement karim) {
        Random r = new Random(7);
        Instant maintenant = Instant.now();
        Instant ouverture = LocalDate.now(FileService.TUNIS).atStartOfDay(FileService.TUNIS).plusHours(9).toInstant();
        String[] prenoms = {"Youssef M.", "Hedi B.", "Mme Trabelsi", "Walid S.", "Omar K.", "Nour H.", "Mehdi G.", "Rania T.", "Fares J.", "Ines K."};
        Map<String, Integer> attente = Map.of("salon-karim", 4, "barber-ennasr", 6, "coiff-style-ariana", 1,
                "salon-lina", 3, "labo-el-menzah", 5, "agence-ariana-centre", 7, "municipalite-ariana", 9);

        for (Etablissement e : tous) {
            int nAttente = attente.getOrDefault(e.getSlug(), 0);
            long ordre = 0;
            // Historique de la journée (pour le tableau de bord)
            int nServis = e == karim ? 30 : 6;
            long fenetre = Math.max(60, Duration.between(ouverture, maintenant).toMinutes() - 30);
            for (int i = 0; i < nServis && maintenant.isAfter(ouverture.plusSeconds(3600)); i++) {
                Prestation p = e.getPrestations().get(r.nextInt(e.getPrestations().size()));
                Instant cree = ouverture.plus(Duration.ofMinutes((long) (fenetre * Math.pow(r.nextDouble(), 0.8))));
                Instant appele = cree.plus(Duration.ofMinutes(5 + r.nextInt(15)));
                if (appele.isAfter(maintenant)) continue;
                Ticket t = ticket(e, p, prenoms[r.nextInt(prenoms.length)], ++ordre);
                t.setCreeLe(cree);
                t.setAppeleLe(appele);
                t.setTermineLe(appele.plus(Duration.ofMinutes(Math.max(3, p.getDureeMinutes() + r.nextInt(7) - 3))));
                t.setStatut(r.nextInt(14) == 0 ? StatutTicket.ABSENT : StatutTicket.SERVI);
                tickets.save(t);
            }
            // Clients en cours aux postes du Salon Karim
            if (e == karim) {
                Ticket c1 = ticket(e, e.getPrestations().get(2), "Mehdi Gharbi", ++ordre);
                c1.setCreeLe(maintenant.minus(Duration.ofMinutes(26)));
                c1.setAppeleLe(maintenant.minus(Duration.ofMinutes(4)));
                c1.setStatut(StatutTicket.APPELE);
                c1.setPoste("Poste 1 · Karim");
                c1.setReponsesJson("{\"nom\":\"Mehdi Gharbi\",\"telephone\":\"22111222\",\"coiffeur\":\"Peu importe\",\"remarque\":\"Dégradé bas\"}");
                c1.setFormulaireVersion(3);
                tickets.save(c1);
            }
            for (int i = 0; i < nAttente; i++) {
                boolean prio = e == karim && i == 2;
                Prestation p = e.getPrestations().get(i % e.getPrestations().size());
                Ticket t = ticket(e, p, prenoms[(i + 3) % prenoms.length], ++ordre);
                t.setPrioritaire(prio);
                t.setNumero(prio ? "P-03" : t.getNumero());
                t.setSurPlace(i == 1);
                t.setCreeLe(maintenant.minus(Duration.ofMinutes((nAttente - i) * 5L + 1)));
                if (e == karim) {
                    t.setFormulaireVersion(3);
                    t.setReponsesJson("{\"nom\":\"" + t.getNomClient() + "\",\"telephone\":\"22333444\",\"coiffeur\":\"Peu importe\"}");
                }
                tickets.save(t);
            }
            e.setCompteurPrioritaire(e == karim ? 3 : 0);
        }
    }

    private Ticket ticket(Etablissement e, Prestation p, String nom, long ordre) {
        Ticket t = new Ticket();
        t.setCode(UUID.randomUUID().toString());
        t.setEtablissement(e);
        t.setPrestation(p);
        e.setCompteurNormal(e.getCompteurNormal() + 1);
        t.setNumero(String.format("%s-%02d", e.getCategorie().getPrefixe(), e.getCompteurNormal()));
        t.setNomClient(nom);
        t.setReponsesJson("{\"nom\":\"" + nom + "\"}");
        t.setOrdre(ordre);
        t.setFormulaireVersion(1);
        return t;
    }
}
