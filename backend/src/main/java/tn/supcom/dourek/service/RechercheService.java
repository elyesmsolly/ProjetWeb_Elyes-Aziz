package tn.supcom.dourek.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.supcom.dourek.dto.Dtos.*;
import tn.supcom.dourek.model.Categorie;
import tn.supcom.dourek.model.Etablissement;
import tn.supcom.dourek.repository.EtablissementRepository;

/**
 * Recherche et classement des établissements. Le classement est fait ici, par notre code
 * (distance, attente, avis) : il est contrôlable et testable. C'est la recherche « normale », sans IA ;
 * l'assistant (AssistantService) s'appuie sur les mêmes fiches et le même score.
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
        List<Etablissement> fiches = etablissements.findAll();
        // « caffe » → « cafe », « coifure » → « coiffure » : un mot inconnu est remplacé par le mot connu le plus proche.
        String q = corriger(normaliser(texte), vocabulaire(fiches.stream().map(RechercheService::texteFiche).toList()), Set.of());
        // « coiffure », « barbe », « analyse »… : on devine d'abord la ou les catégories (« coiffure » seul désigne
        // les barbiers ET la coiffure femme), puis on cherche les autres mots.
        boolean filtre = categorie != null && !categorie.isBlank();
        Set<Categorie> devinees = q.isBlank() || filtre ? Set.of() : categoriesDepuisTexte(q);
        List<String> mots = motsUtiles(q);
        // Les mots qui désignent la catégorie (« coiffeur », « coupe »…) ne départagent plus les établissements :
        // ceux qui ont servi à la deviner, ou ceux qui répètent la catégorie déjà choisie dans les filtres.
        if (!devinees.isEmpty()) mots.removeIf(m -> !categoriesDepuisTexte(m).isEmpty() || GENERIQUES.contains(m));
        else if (filtre) mots.removeIf(m -> categoriesDepuisTexte(m).stream().anyMatch(c -> c.name().equalsIgnoreCase(categorie)));
        List<Etablissement> retenus = new ArrayList<>();
        java.util.Map<Etablissement, Integer> points = new java.util.HashMap<>();
        for (Etablissement e : fiches) {
            if (filtre && !memeCategorie(e, categorie)) continue;
            if (!devinees.isEmpty() && !devinees.contains(e.getCategorie())) continue;
            retenus.add(e);
            points.put(e, motsTrouves(e, mots));
        }
        // On garde les établissements qui correspondent au plus grand nombre de mots
        // (« Barber Ennasr » ne renvoie que Barber Ennasr, « coupe pas cher » renvoie tous les barbiers).
        int max = points.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (!mots.isEmpty() && (max > 0 || devinees.isEmpty())) {
            final int seuil = max;
            retenus.removeIf(e -> points.get(e) < seuil || seuil == 0);
        }
        List<EtablissementResume> res = new ArrayList<>();
        for (Etablissement e : retenus) {
            res.add(resume(e, la, lo, false));
        }
        res.sort(Comparator.comparingDouble(EtablissementResume::score));
        if (!res.isEmpty() && res.getFirst().fileOuverte()) res.set(0, marquer(res.getFirst(), true));
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

    /** Copie de la fiche avec ou sans la marque « meilleur choix » (ou « choix de l'assistant »). */
    static EtablissementResume marquer(EtablissementResume r, boolean choix) {
        return new EtablissementResume(r.id(), r.slug(), r.nom(), r.categorie(), r.categorieLibelle(), r.quartier(),
                r.adresse(), r.latitude(), r.longitude(), r.distanceKm(), r.note(), r.nombreAvis(), r.fileOuverte(),
                r.heureFermeture(), r.personnesEnAttente(), r.attenteMinutes(), r.postesOuverts(), r.prestations(),
                r.score(), choix);
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

    /** Mots trop généraux pour départager les établissements une fois la catégorie connue. */
    static final java.util.Set<String> GENERIQUES = java.util.Set.of("homme", "hommes", "femme", "femmes", "dame", "dames", "salon", "salons");

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
        String cible = normaliser(texteFiche(e));
        int n = 0;
        for (String mot : mots)
            if (cible.contains(mot) || (mot.length() >= 5 && cible.contains(mot.substring(0, 5)))) n++;
        return n;
    }

    /** Le texte d'une fiche dans lequel on cherche : nom, type, quartier, adresse et prestations. */
    private static String texteFiche(Etablissement e) {
        StringBuilder sb = new StringBuilder(e.getNom()).append(' ').append(e.libelleCategorie())
                .append(' ').append(e.getQuartier()).append(' ').append(e.getAdresse());
        e.getPrestations().forEach(p -> sb.append(' ').append(p.getNom()));
        return sb.toString();
    }

    // ------------------------------------------------------------------ fautes de frappe

    /** Mots du métier connus même si aucune fiche ne les contient encore : ils servent à corriger les fautes de frappe. */
    private static final Set<String> MOTS_METIER = Set.of("coiffure", "coiffeur", "coiffeuse", "coupe", "cheveux", "barbe", "barbier",
            "rasage", "degrade", "brushing", "meches", "couleur", "lissage", "laboratoire", "analyse", "sang", "bilan",
            "banque", "agence", "poste", "operateur", "telephone", "facture", "abonnement", "mairie", "municipalite",
            "administration", "papier", "extrait", "legalisation", "signature", "cafe", "boire", "boisson", "soif",
            "manger", "restaurant", "pizza", "sandwich", "pharmacie", "medecin", "docteur", "dentiste", "garage",
            "pressing", "homme", "femme", "enfant", "salon");

    /** Vrais mots français qui ressemblent à un mot du métier : ce ne sont pas des fautes de frappe. */
    private static final Set<String> FAUX_AMIS = Set.of("fracture", "couple", "marie", "manque", "porte", "barre", "carte", "cadre", "pose", "sage");

    private static final java.util.regex.Pattern MOT = java.util.regex.Pattern.compile("[a-z]{4,}");

    /** Les mots connus : ceux du métier et ceux des fiches (noms, types, quartiers, prestations). */
    static Set<String> vocabulaire(List<String> textes) {
        Set<String> mots = new java.util.HashSet<>(MOTS_METIER);
        for (String t : textes) for (String m : normaliser(t).split("[^a-z0-9]+")) if (m.length() >= 3) mots.add(m);
        return mots;
    }

    /**
     * Corrige les fautes de frappe d'un texte déjà normalisé : chaque mot inconnu (ni dans le vocabulaire, ni parmi
     * les mots à garder, ni mot-clé d'une catégorie) est remplacé par le mot connu le plus proche, s'il y en a un.
     * Le reste du texte (chiffres, ponctuation) ne bouge pas.
     */
    static String corriger(String texteNormalise, Set<String> vocabulaire, Set<String> aGarder) {
        if (texteNormalise.isBlank()) return texteNormalise;
        return MOT.matcher(texteNormalise).replaceAll(m -> plusProche(m.group(), vocabulaire, aGarder));
    }

    private static String plusProche(String mot, Set<String> vocabulaire, Set<String> aGarder) {
        if (vocabulaire.contains(mot) || VIDES.contains(mot) || aGarder.contains(mot) || FAUX_AMIS.contains(racine(mot))
                || !categoriesDepuisTexte(mot).isEmpty()) return mot;
        // « coiffeur » trouve déjà « coiffure » par son début : ce n'est pas une faute.
        if (mot.length() >= 5 && vocabulaire.stream().anyMatch(v -> v.length() >= 5 && v.startsWith(mot.substring(0, 5)))) return mot;
        String racine = racine(mot), mieux = mot;
        int ecart = 2;
        for (String v : vocabulaire) {
            if (v.length() < 4) continue;
            String r = racine(v);
            // Lettre doublée ou pluriel (« caffe », « cafes ») : toujours accepté. Une lettre fausse : mots de 5 lettres et plus.
            int d = r.equals(racine) ? 0 : mot.length() >= 5 && v.length() >= 5 && uneLettrePres(racine, r) ? 1 : 2;
            if (d < ecart || (d == ecart && d < 2 && v.compareTo(mieux) < 0)) { ecart = d; mieux = v; }
        }
        return mieux;
    }

    /** Forme simplifiée d'un mot pour le comparer : lettres doublées réduites (« caffe » → « cafe »), « s » ou « x » final retiré. */
    static String racine(String mot) {
        String r = mot.replaceAll("(.)\\1+", "$1");
        return r.length() > 4 && (r.endsWith("s") || r.endsWith("x")) ? r.substring(0, r.length() - 1) : r;
    }

    /** Vrai si les deux mots diffèrent d'une seule lettre (remplacée, ajoutée ou oubliée) ou de deux lettres voisines inversées. */
    static boolean uneLettrePres(String a, String b) {
        if (a.equals(b)) return true;
        if (Math.abs(a.length() - b.length()) > 1) return false;
        if (a.length() > b.length()) { String t = a; a = b; b = t; }
        int i = 0;
        while (i < a.length() && a.charAt(i) == b.charAt(i)) i++;
        if (a.length() == b.length()) {
            if (a.substring(i + 1).equals(b.substring(i + 1))) return true;                        // une lettre remplacée
            return i + 1 < a.length() && a.charAt(i) == b.charAt(i + 1) && a.charAt(i + 1) == b.charAt(i)
                    && a.substring(i + 2).equals(b.substring(i + 2));                               // deux lettres inversées
        }
        return a.substring(i).equals(b.substring(i + 1));                                           // une lettre en plus ou en moins
    }

    public static String normaliser(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    /** Catégorie principale devinée depuis un texte (la première si plusieurs sont possibles), ou null. */
    public static Categorie categorieDepuisTexte(String q) {
        Set<Categorie> c = categoriesDepuisTexte(q);
        return c.isEmpty() ? null : c.iterator().next();
    }

    /**
     * Catégories désignées par un texte libre. « coiffure » ou « coupe » sans autre précision désignent les
     * barbiers et la coiffure femme ; « barbe » ou « coupe homme » les barbiers ; « brushing » la coiffure femme.
     */
    public static Set<Categorie> categoriesDepuisTexte(String q) {
        String t = normaliser(q);
        if (t.matches(".*(labo|analyse|prise de sang|bilan|sang).*")) return EnumSet.of(Categorie.LABORATOIRE);
        if (t.matches(".*(banque|agence|poste|operateur|telephon|facture|abonnement).*")) return EnumSet.of(Categorie.AGENCE);
        if (t.matches(".*(mairie|municipal|administration|papier|extrait|legalisation|signature).*")) return EnumSet.of(Categorie.ADMINISTRATION);
        // \b : « madame » ne doit pas être lu comme « dame ».
        boolean femme = t.matches(".*(\\b(femmes?|dames?|filles?)\\b|brushing|meche|couleur|lissage).*")
                && t.matches(".*(coiff|coupe|cheveu|brushing|meche|couleur|lissage|salon).*");
        boolean homme = t.matches(".*(barb|rasage|degrade).*")
                || (t.matches(".*\\b(hommes?|garcons?)\\b.*") && t.matches(".*(coiff|coupe|cheveu|salon).*"));
        if (femme && !homme) return EnumSet.of(Categorie.COIFFURE_FEMME);
        if (homme && !femme) return EnumSet.of(Categorie.BARBIER);
        if (femme || t.matches(".*(coiff|coupe|cheveu).*")) return EnumSet.of(Categorie.BARBIER, Categorie.COIFFURE_FEMME);
        return Set.of();
    }

    /** Mots de la fiche qui disent ce que fait l'établissement : nom, type et prestations (pas l'adresse). */
    static Set<String> motsMetier(EtablissementResume e) {
        StringBuilder sb = new StringBuilder(e.nom()).append(' ').append(e.categorieLibelle());
        e.prestations().forEach(p -> sb.append(' ').append(p.nom()));
        return new java.util.HashSet<>(List.of(normaliser(sb.toString()).split("[^a-z0-9]+")));
    }

    /** Vrai si un des mots cherchés figure dans la fiche (mot entier, ou même début pour les mots longs). */
    static boolean correspond(EtablissementResume e, List<String> mots) {
        Set<String> fiche = motsMetier(e);
        for (String mot : mots)
            for (String f : fiche)
                if (f.equals(mot) || (mot.length() >= 5 && f.length() >= 5 && f.startsWith(mot.substring(0, 5)))) return true;
        return false;
    }
}
