package tn.supcom.dourek.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.supcom.dourek.dto.Dtos.*;
import tn.supcom.dourek.model.Categorie;
import tn.supcom.dourek.model.Etablissement;
import tn.supcom.dourek.repository.EtablissementRepository;

/**
 * Recherche et classement des établissements. Le classement est fait ici, par notre code
 * (distance, attente, avis) : il est contrôlable et testable. L'assistant IA ne fait qu'expliquer.
 */
@Service
public class RechercheService {

    /** Position par défaut : Ariana (si le navigateur ne donne pas la position). */
    public static final double LAT_ARIANA = 36.8625, LNG_ARIANA = 10.1956;

    private final EtablissementRepository etablissements;
    private final FileService file;
    private final FormulaireService formulaires;

    public RechercheService(EtablissementRepository etablissements, FileService file, FormulaireService formulaires) {
        this.etablissements = etablissements;
        this.file = file;
        this.formulaires = formulaires;
    }

    @Transactional(readOnly = true)
    public List<EtablissementResume> rechercher(String categorie, String texte, Double lat, Double lng) {
        double la = lat == null ? LAT_ARIANA : lat, lo = lng == null ? LNG_ARIANA : lng;
        String q = normaliser(texte);
        // « coiffeur », « barbe », « analyse »… : on devine d'abord la catégorie, puis on cherche les autres mots.
        Categorie devinee = q.isBlank() || (categorie != null && !categorie.isBlank()) ? null : categorieDepuisTexte(q);
        List<String> mots = motsUtiles(q);
        // Les mots qui ont servi à deviner la catégorie (« coiffeur », « coupe »…) ne départagent plus.
        if (devinee != null) mots.removeIf(m -> categorieDepuisTexte(m) != null || m.equals("homme") || m.equals("femme"));
        List<Etablissement> retenus = new ArrayList<>();
        java.util.Map<Etablissement, Integer> points = new java.util.HashMap<>();
        for (Etablissement e : etablissements.findAll()) {
            if (categorie != null && !categorie.isBlank() && !memeCategorie(e, categorie)) continue;
            if (devinee != null && e.getCategorie() != devinee) continue;
            retenus.add(e);
            points.put(e, motsTrouves(e, mots));
        }
        // On garde les établissements qui correspondent au plus grand nombre de mots
        // (« Barber Ennasr » ne renvoie que Barber Ennasr, « coupe pas cher » renvoie tous les barbiers).
        int max = points.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (!mots.isEmpty() && (max > 0 || devinee == null)) {
            final int seuil = max;
            retenus.removeIf(e -> points.get(e) < seuil || seuil == 0);
        }
        List<EtablissementResume> res = new ArrayList<>();
        for (Etablissement e : retenus) {
            res.add(resume(e, la, lo, false));
        }
        res.sort(Comparator.comparingDouble(EtablissementResume::score));
        if (!res.isEmpty() && res.getFirst().fileOuverte()) res.set(0, avecMeilleurChoix(res.getFirst()));
        return res;
    }

    @Transactional(readOnly = true)
    public EtablissementDetail detail(String slug, Double lat, Double lng) {
        Etablissement e = etablissements.findBySlug(slug).orElseThrow(() -> Introuvable.de("Établissement"));
        return new EtablissementDetail(
                resume(e, lat == null ? LAT_ARIANA : lat, lng == null ? LNG_ARIANA : lng, false),
                e.getDescription(), e.getHoraires(), e.getTelephone(), formulaires.actif(e.getId()));
    }

    public EtablissementResume resume(Etablissement e, double lat, double lng, boolean meilleur) {
        int personnes = file.enAttente(e.getId()).size();
        int attente = FileService.estimerAttente(personnes, file.dureeMoyenne(e), e.getPostesOuverts());
        double km = distanceKm(lat, lng, e.getLatitude(), e.getLongitude());
        List<PrestationDto> prest = e.getPrestations().stream()
                .map(p -> new PrestationDto(p.getId(), p.getNom(), p.getDescription(), p.getDureeMinutes(), p.getPrix()))
                .toList();
        return new EtablissementResume(e.getId(), e.getSlug(), e.getNom(), e.getCategorie().name(),
                e.libelleCategorie(), e.getQuartier(), e.getAdresse(), e.getLatitude(), e.getLongitude(),
                Math.round(km * 100) / 100.0, e.getNote(), e.getNombreAvis(), e.isFileOuverte(), e.getHeureFermeture(),
                personnes, attente, e.getPostesOuverts(), prest, score(attente, km, e.getNote(), e.isFileOuverte()), meilleur);
    }

    /**
     * Score (plus petit = meilleur) en « minutes équivalentes » :
     * attente + temps de marche (12 min/km) + pénalité si la note est sous 4,5. File en pause = en dernier.
     */
    public static double score(int attenteMinutes, double distanceKm, double note, boolean ouverte) {
        double s = attenteMinutes + distanceKm * 12 + (4.5 - note) * 10;
        return Math.round((ouverte ? s : s + 10_000) * 10) / 10.0;
    }

    /** Distance à vol d'oiseau (formule de haversine). */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371, dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    static EtablissementResume avecMeilleurChoix(EtablissementResume r) {
        return new EtablissementResume(r.id(), r.slug(), r.nom(), r.categorie(), r.categorieLibelle(), r.quartier(),
                r.adresse(), r.latitude(), r.longitude(), r.distanceKm(), r.note(), r.nombreAvis(), r.fileOuverte(),
                r.heureFermeture(), r.personnesEnAttente(), r.attenteMinutes(), r.postesOuverts(), r.prestations(),
                r.score(), true);
    }

    /** Le filtre accepte un code (BARBIER, AUTRE…) ou un type libre créé par un gérant (ex. « Pressing »). */
    private static boolean memeCategorie(Etablissement e, String categorie) {
        if (e.getCategorie().name().equalsIgnoreCase(categorie)) return true;
        return e.getCategorieLibre() != null && normaliser(e.getCategorieLibre()).equals(normaliser(categorie));
    }

    /** Mots qui ne disent rien sur l'établissement recherché. */
    private static final java.util.Set<String> VIDES = java.util.Set.of("les", "des", "une", "pour", "avec", "chez", "pas",
            "cher", "trop", "pres", "proche", "moi", "vite", "possible", "avant", "apres", "dans", "sur", "qui", "est",
            "veux", "voudrais", "cherche", "besoin", "faire", "tunis", "maintenant", "aujourd", "hui", "bon", "bien");

    static List<String> motsUtiles(String q) {
        List<String> mots = new ArrayList<>();
        for (String m : q.split("[^a-z0-9]+")) if (m.length() > 2 && !VIDES.contains(m)) mots.add(m);
        return mots;
    }

    /**
     * Nombre de mots utiles présents dans la fiche (nom, type, quartier, adresse,
     * prestations). On compare aussi le début du mot (« coiffeur » trouve « coiffure »).
     */
    private static int motsTrouves(Etablissement e, List<String> mots) {
        StringBuilder sb = new StringBuilder(e.getNom()).append(' ').append(e.libelleCategorie())
                .append(' ').append(e.getQuartier()).append(' ').append(e.getAdresse());
        e.getPrestations().forEach(p -> sb.append(' ').append(p.getNom()));
        String cible = normaliser(sb.toString());
        int n = 0;
        for (String mot : mots)
            if (cible.contains(mot) || (mot.length() >= 5 && cible.contains(mot.substring(0, 5)))) n++;
        return n;
    }

    public static String normaliser(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    public static Categorie categorieDepuisTexte(String q) {
        String t = normaliser(q);
        if (t.matches(".*(labo|analyse|prise de sang|bilan|sang).*")) return Categorie.LABORATOIRE;
        if (t.matches(".*(banque|agence|poste|operateur|telephon|facture|abonnement).*")) return Categorie.AGENCE;
        if (t.matches(".*(mairie|municipal|administration|papier|extrait|legalisation|signature).*")) return Categorie.ADMINISTRATION;
        if (t.matches(".*(femme|dame|brushing|meche|couleur|lissage).*") && t.matches(".*(coiff|coupe|brushing|meche|couleur|lissage|salon).*"))
            return Categorie.COIFFURE_FEMME;
        if (t.matches(".*(barb|coupe|degrade|coiff|rasage|cheveu).*")) return Categorie.BARBIER;
        return null;
    }
}
