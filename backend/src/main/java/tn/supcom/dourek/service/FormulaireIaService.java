package tn.supcom.dourek.service;

import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.supcom.dourek.dto.Dtos.ChampFormulaire;
import tn.supcom.dourek.dto.Dtos.GenerationFormulaireReponse;
import tn.supcom.dourek.dto.Dtos.GenerationFormulaireRequest;
import tn.supcom.dourek.model.Categorie;
import tools.jackson.databind.JsonNode;

/**
 * Création du formulaire client à partir d'une phrase du gérant (« Laboratoire : date de naissance, à jeun… »).
 * 1. Le modèle (Claude, sinon Gemini) propose des champs en sortie structurée.
 * 2. NOTRE code les contrôle : types connus, options valides, longueurs, pas de doublon, et les deux champs
 *    indispensables (nom, téléphone) toujours en tête. Le gérant garde la main : il modifie puis publie lui-même.
 * Sans clé d'IA, ou si l'IA ne répond pas, des règles simples par mots-clés prennent le relais.
 */
@Service
public class FormulaireIaService {

    private static final Logger log = LoggerFactory.getLogger(FormulaireIaService.class);

    /** Nombre maximal de champs proposés, en plus du nom et du téléphone. */
    static final int MAX_CHAMPS = 8;
    private static final int MAX_OPTIONS = 8;

    /** Champ proposé par le modèle (sortie structurée : le schéma JSON est dérivé de ces records). */
    public record ChampGenere(
            @JsonPropertyDescription("Une valeur parmi TEXTE, TEXTE_LONG, TELEPHONE, NOMBRE, LISTE, CHOIX, CASE, DATE")
            String type,
            @JsonPropertyDescription("Intitulé court du champ, en français, sans deux-points")
            String label,
            @JsonPropertyDescription("true seulement si l'employé ne peut pas servir le client sans cette information")
            boolean obligatoire,
            @JsonPropertyDescription("Options proposées pour LISTE et CHOIX ; liste vide pour les autres types")
            List<String> options,
            @JsonPropertyDescription("Court exemple ou précision affiché sous le champ, ou chaîne vide")
            String aide) {}

    public record FormulaireGenere(
            @JsonPropertyDescription("Champs du formulaire, dans l'ordre d'affichage, sans le nom ni le téléphone du client")
            List<ChampGenere> champs) {}

    static final String CONSIGNE = """
            Tu aides le gérant d'un commerce ou d'un service tunisien à préparer le formulaire que ses clients \
            remplissent sur Dourek, une plateforme de files d'attente virtuelles, avant de prendre un ticket à distance. \
            Ce formulaire sert à préparer la visite : l'employé le lit au moment d'appeler le client.

            À partir de la description du gérant, propose les champs du formulaire.
            - Les champs « Nom et prénom » et « Téléphone » du client existent déjà : ne les propose pas.
            - Si le gérant cite des informations précises, crée un champ pour chacune, dans l'ordre où il les cite. \
            S'il décrit seulement son activité, propose 2 à 4 champs vraiment utiles pour cette activité.
            - Reste court : le client remplit ce formulaire sur son téléphone, donc 6 champs au plus.
            - Types possibles : TEXTE (réponse courte), TEXTE_LONG (remarque libre), TELEPHONE (un autre numéro que \
            celui du client), NOMBRE, LISTE (liste déroulante, à partir de 4 options), CHOIX (choix unique entre 2 ou 3 \
            options), CASE (case à cocher), DATE.
            - Pour LISTE et CHOIX, donne les options. Pour les autres types, laisse la liste d'options vide.
            - « obligatoire » vaut true seulement si l'employé ne peut pas servir le client sans cette information.
            - « aide » est un court exemple ou une précision, ou une chaîne vide.
            - Les intitulés sont en français, courts, sans deux-points. La description peut être en français, en arabe \
            ou en dialecte tunisien.
            - Ne demande jamais de mot de passe, de numéro de carte bancaire, ni d'information sans rapport avec la visite.""";

    /** Schéma JSON équivalent pour la sortie structurée de Gemini. */
    private static final Map<String, Object> SCHEMA_GEMINI = Map.of(
            "type", "OBJECT",
            "properties", Map.of("champs", Map.of(
                    "type", "ARRAY",
                    "description", "Champs du formulaire, dans l'ordre d'affichage, sans le nom ni le téléphone du client",
                    "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "type", Map.of("type", "STRING", "enum", List.of("TEXTE", "TEXTE_LONG", "TELEPHONE", "NOMBRE", "LISTE", "CHOIX", "CASE", "DATE")),
                                    "label", Map.of("type", "STRING", "description", "Intitulé court du champ, en français, sans deux-points"),
                                    "obligatoire", Map.of("type", "BOOLEAN"),
                                    "options", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"),
                                            "description", "Options pour LISTE et CHOIX ; liste vide pour les autres types"),
                                    "aide", Map.of("type", "STRING", "description", "Court exemple ou précision, ou chaîne vide")),
                            "required", List.of("type", "label", "obligatoire", "options", "aide")))),
            "required", List.of("champs"));

    private final ClaudeClient claude;
    private final GeminiClient gemini;

    public FormulaireIaService(ClaudeClient claude, GeminiClient gemini) {
        this.claude = claude;
        this.gemini = gemini;
    }

    public GenerationFormulaireReponse generer(GenerationFormulaireRequest req) {
        String demande = demande(req);
        List<ChampFormulaire> champs = List.of();
        String source = "local";
        if (claude.actif()) {
            try {
                champs = controler(avecClaude(demande));
                source = "claude";
            } catch (RuntimeException ex) {
                log.warn("Formulaire par IA : appel à Claude impossible ({})", ex.getMessage());
            }
        }
        if (champs.isEmpty() && gemini.actif()) {
            try {
                champs = controler(avecGemini(demande));
                source = "gemini";
            } catch (RuntimeException ex) {
                log.warn("Formulaire par IA : appel à Gemini impossible ({})", ex.getMessage());
            }
        }
        // Pas d'IA configurée, IA injoignable ou proposition vide : règles locales.
        if (champs.isEmpty()) {
            champs = controler(proposerLocalement(req.description(), req.activite()));
            source = "local";
        }
        List<ChampFormulaire> formulaire = new ArrayList<>(FormulaireService.champsParDefaut());
        formulaire.addAll(champs);
        return new GenerationFormulaireReponse(formulaire, source);
    }

    private static String demande(GenerationFormulaireRequest req) {
        StringBuilder sb = new StringBuilder();
        if (req.etablissement() != null && !req.etablissement().isBlank()) sb.append("Établissement : ").append(req.etablissement().trim()).append('\n');
        if (req.activite() != null && !req.activite().isBlank()) sb.append("Activité : ").append(req.activite().trim()).append('\n');
        return sb.append("Description du gérant : « ").append(req.description().trim()).append(" »").toString();
    }

    // ------------------------------------------------------------------ appels aux modèles

    private List<ChampGenere> avecClaude(String demande) {
        StructuredMessageCreateParams<FormulaireGenere> params = MessageCreateParams.builder()
                .model(claude.modele())
                .maxTokens(8000L)
                .system(CONSIGNE)
                .outputConfig(FormulaireGenere.class)
                .addUserMessage(demande)
                .build();
        FormulaireGenere f = claude.api().messages().create(params).content().stream()
                .flatMap(b -> b.text().stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("réponse vide"))
                .text();
        return f.champs() == null ? List.of() : f.champs();
    }

    private List<ChampGenere> avecGemini(String demande) {
        JsonNode racine = gemini.json(CONSIGNE, demande, SCHEMA_GEMINI);
        List<ChampGenere> champs = new ArrayList<>();
        for (JsonNode c : racine.path("champs")) {
            List<String> options = new ArrayList<>();
            for (JsonNode o : c.path("options")) options.add(o.asString(""));
            champs.add(new ChampGenere(c.path("type").asString(""), c.path("label").asString(""),
                    c.path("obligatoire").asBoolean(false), options, c.path("aide").asString("")));
        }
        return champs;
    }

    // ------------------------------------------------------------------ contrôle (notre code)

    private static final Set<String> DEJA_PRESENTS = Set.of("nom", "prenom", "nom et prenom", "prenom et nom", "nom prenom",
            "nom complet", "votre nom", "nom du client", "nom et prenom du client", "telephone", "numero de telephone",
            "tel", "telephone du client", "numero de telephone du client", "votre telephone", "mobile", "portable", "gsm");

    /**
     * Rend la proposition publiable telle quelle : le modèle propose, mais c'est ce contrôle qui garantit
     * un formulaire valide (mêmes règles que FormulaireService à la publication).
     */
    static List<ChampFormulaire> controler(List<ChampGenere> proposes) {
        List<ChampFormulaire> champs = new ArrayList<>();
        Set<String> vus = new HashSet<>();
        Set<String> cles = new HashSet<>();
        for (ChampGenere p : proposes) {
            if (p == null || champs.size() >= MAX_CHAMPS) continue;
            String label = couper(nettoyer(p.label()).replaceAll("[\\s:*]+$", ""), 80);
            String simple = simplifier(label);
            if (simple.isBlank() || DEJA_PRESENTS.contains(simple) || !vus.add(simple)) continue;
            String type = p.type() == null ? "" : p.type().trim().toUpperCase(Locale.ROOT);
            if (!FormulaireService.TYPES.contains(type)) type = "TEXTE";
            List<String> options = List.of();
            if (type.equals("LISTE") || type.equals("CHOIX")) {
                options = p.options() == null ? List.of() : p.options().stream()
                        .map(o -> couper(nettoyer(o), 60)).filter(o -> !o.isBlank()).distinct().limit(MAX_OPTIONS).toList();
                // Une liste sans vrai choix ne sert à rien : on en fait un champ de texte.
                if (options.size() < 2) {
                    type = "TEXTE";
                    options = List.of();
                }
            }
            String aide = type.equals("CASE") || type.equals("CHOIX") ? "" : couper(nettoyer(p.aide()), 120);
            String cle;
            do cle = "champ_" + Long.toString(ThreadLocalRandom.current().nextLong(36L * 36 * 36 * 36 * 36, 36L * 36 * 36 * 36 * 36 * 36), 36);
            while (!cles.add(cle));
            champs.add(new ChampFormulaire(cle, type, label, p.obligatoire(), options, aide.isBlank() ? null : aide));
        }
        return champs;
    }

    private static String nettoyer(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static String couper(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max).trim();
    }

    /** Minuscules, sans accent ni ponctuation : sert à comparer les intitulés et à repérer les mots-clés. */
    static String simplifier(String s) {
        return RechercheService.normaliser(s).replaceAll("[^a-z0-9]+", " ").trim();
    }

    // ------------------------------------------------------------------ mode local (sans clé)

    private record Regle(Pattern motif, ChampGenere champ) {}

    private static Regle regle(String motif, String type, String label, boolean obligatoire, List<String> options, String aide) {
        return new Regle(Pattern.compile("\\b(" + motif + ")"), new ChampGenere(type, label, obligatoire, options, aide));
    }

    /** Mots-clés (texte simplifié) et champ correspondant, dans l'ordre d'affichage. */
    private static final List<Regle> REGLES = List.of(
            regle("date de naissance|naissance", "DATE", "Date de naissance", true, List.of(), ""),
            regle("age\\b", "NOMBRE", "Âge", false, List.of(), ""),
            regle("cin\\b|carte d identite", "TEXTE", "Numéro de CIN", false, List.of(), "8 chiffres"),
            regle("e ?mail|courriel", "TEXTE", "E-mail", false, List.of(), "exemple@domaine.tn"),
            regle("adresse(?! e ?mail| mail)", "TEXTE", "Adresse", false, List.of(), ""),
            regle("a jeun", "CASE", "Je suis à jeun", false, List.of(), ""),
            regle("ordonnance", "LISTE", "Ordonnance", true, List.of("Papier", "Numérique", "Sans ordonnance"), ""),
            regle("type d analyse|analyses? demandee", "TEXTE", "Analyse demandée", false, List.of(), "Ex. bilan sanguin"),
            regle("allergi", "TEXTE", "Allergies connues", false, List.of(), ""),
            regle("cnam|assurance|mutuelle", "CHOIX", "Assuré CNAM", false, List.of("Oui", "Non"), ""),
            regle("coiffeu(r|se) prefere|barbier prefere|employe prefere", "TEXTE", "Coiffeur préféré", false, List.of(), "Laissez vide si peu importe"),
            regle("immatriculation|matricule|voiture|vehicule", "TEXTE", "Immatriculation du véhicule", false, List.of(), "Ex. 123 TU 4567"),
            regle("marque", "TEXTE", "Marque et modèle", false, List.of(), ""),
            regle("animal|animaux|chien|chat\\b|veterinaire", "TEXTE", "Nom de l'animal", false, List.of(), ""),
            regle("espece|animal|animaux|veterinaire", "LISTE", "Espèce", false, List.of("Chien", "Chat", "Oiseau", "Autre"), ""),
            regle("numero de (client|dossier|compte|contrat)|reference", "TEXTE", "Numéro de dossier", false, List.of(), ""),
            regle("type de document|documents?\\b", "TEXTE", "Document demandé", true, List.of(), "Ex. extrait de naissance"),
            regle("motif|raison|objet de la visite|type de demande", "TEXTE", "Motif de la visite", true, List.of(), ""),
            regle("nombre d articles|articles|vetements|linge", "NOMBRE", "Nombre d'articles", false, List.of(), ""),
            regle("nombre de personnes|combien de personnes|accompagn", "NOMBRE", "Nombre de personnes", false, List.of(), ""),
            regle("date souhaitee|date de (retrait|rendez|passage)|quel jour", "DATE", "Date souhaitée", false, List.of(), ""),
            regle("remarque|commentaire|precision|message|demande particuliere|details?\\b", "TEXTE_LONG", "Remarque", false, List.of(), ""),
            regle("sms|rappel", "CASE", "J'accepte d'être prévenu par SMS", false, List.of(), ""));

    /** Sans IA : un champ par mot-clé reconnu, sinon les champs habituels de l'activité. */
    static List<ChampGenere> proposerLocalement(String description, String activite) {
        String texte = simplifier(description);
        List<ChampGenere> champs = new ArrayList<>();
        for (Regle r : REGLES) if (r.motif().matcher(texte).find()) champs.add(r.champ());
        if (!champs.isEmpty()) return champs;

        Categorie cat = RechercheService.categorieDepuisTexte((activite == null ? "" : activite + " ") + description);
        if (cat == null) cat = Categorie.AUTRE;
        return switch (cat) {
            case BARBIER, COIFFURE_FEMME -> List.of(
                    new ChampGenere("TEXTE", "Coiffeur préféré", false, List.of(), "Laissez vide si peu importe"),
                    new ChampGenere("TEXTE_LONG", "Remarque pour le coiffeur", false, List.of(), "Ex. dégradé court sur les côtés"));
            case LABORATOIRE -> List.of(
                    new ChampGenere("DATE", "Date de naissance", true, List.of(), ""),
                    new ChampGenere("CASE", "Je suis à jeun", false, List.of(), ""),
                    new ChampGenere("LISTE", "Ordonnance", true, List.of("Papier", "Numérique", "Sans ordonnance"), ""));
            case AGENCE -> List.of(
                    new ChampGenere("LISTE", "Motif de la visite", true, List.of("Paiement", "Abonnement", "Réclamation", "Autre"), ""),
                    new ChampGenere("TEXTE", "Numéro de client", false, List.of(), ""));
            case ADMINISTRATION -> List.of(
                    new ChampGenere("LISTE", "Document demandé", true,
                            List.of("Extrait de naissance", "Légalisation de signature", "Copie conforme", "Autre"), ""),
                    new ChampGenere("TEXTE", "Numéro de CIN", false, List.of(), "8 chiffres"));
            default -> List.of(
                    new ChampGenere("TEXTE", "Motif de la visite", true, List.of(), ""),
                    new ChampGenere("TEXTE_LONG", "Remarque", false, List.of(), ""));
        };
    }
}
