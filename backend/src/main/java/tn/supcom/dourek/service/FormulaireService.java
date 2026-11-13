package tn.supcom.dourek.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.supcom.dourek.dto.Dtos.ChampFormulaire;
import tn.supcom.dourek.dto.Dtos.FormulaireDto;
import tn.supcom.dourek.model.Etablissement;
import tn.supcom.dourek.model.Formulaire;
import tn.supcom.dourek.repository.EtablissementRepository;
import tn.supcom.dourek.repository.FormulaireRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Formulaires clients versionnés (stockés en JSON). */
@Service
public class FormulaireService {

    public static final Set<String> TYPES = Set.of(
            "TEXTE", "TEXTE_LONG", "TELEPHONE", "NOMBRE", "LISTE", "CHOIX", "CASE", "DATE");

    private final FormulaireRepository formulaires;
    private final EtablissementRepository etablissements;
    private final ObjectMapper json;

    public FormulaireService(FormulaireRepository formulaires, EtablissementRepository etablissements, ObjectMapper json) {
        this.formulaires = formulaires;
        this.etablissements = etablissements;
        this.json = json;
    }

    /** Formulaire proposé par défaut à un nouvel établissement. */
    public static List<ChampFormulaire> champsParDefaut() {
        return List.of(
                new ChampFormulaire("nom", "TEXTE", "Nom et prénom", true, List.of(), null),
                new ChampFormulaire("telephone", "TELEPHONE", "Téléphone", true, List.of(), "Sert aux alertes"));
    }

    @Transactional(readOnly = true)
    public FormulaireDto actif(Long etablissementId) {
        return formulaires.findFirstByEtablissementIdOrderByVersionDesc(etablissementId)
                .map(this::versDto)
                .orElse(new FormulaireDto(0, champsParDefaut(), null));
    }

    @Transactional
    public FormulaireDto publier(Long etablissementId, List<ChampFormulaire> champs) {
        valider(champs);
        Etablissement e = etablissements.findById(etablissementId).orElseThrow(() -> Introuvable.de("Établissement"));
        int version = formulaires.findFirstByEtablissementIdOrderByVersionDesc(etablissementId)
                .map(Formulaire::getVersion).orElse(0) + 1;
        Formulaire f = formulaires.save(new Formulaire(e, version, json.writeValueAsString(champs)));
        return versDto(f);
    }

    public FormulaireDto versDto(Formulaire f) {
        return new FormulaireDto(f.getVersion(), lireChamps(f.getChampsJson()), f.getPublieLe());
    }

    public List<ChampFormulaire> lireChamps(String champsJson) {
        return json.readValue(champsJson, new TypeReference<List<ChampFormulaire>>() {});
    }

    public String ecrire(Object valeur) {
        return json.writeValueAsString(valeur);
    }

    public Map<String, Object> lireReponses(String reponsesJson) {
        if (reponsesJson == null || reponsesJson.isBlank()) return Map.of();
        return json.readValue(reponsesJson, new TypeReference<Map<String, Object>>() {});
    }

    private void valider(List<ChampFormulaire> champs) {
        Set<String> cles = new HashSet<>();
        for (ChampFormulaire c : champs) {
            if (!TYPES.contains(c.type())) throw Introuvable.requete("Type de champ inconnu : " + c.type());
            if (!cles.add(c.cle())) throw Introuvable.requete("Deux champs ont la même clé : " + c.cle());
            boolean aOptions = c.type().equals("LISTE") || c.type().equals("CHOIX");
            if (aOptions && (c.options() == null || c.options().isEmpty()))
                throw Introuvable.requete("Le champ « " + c.label() + " » doit proposer au moins une option");
        }
    }

    /** Vérifie les réponses du client par rapport à la version du formulaire. */
    public void validerReponses(List<ChampFormulaire> champs, Map<String, Object> reponses) {
        for (ChampFormulaire c : champs) {
            Object v = reponses.get(c.cle());
            boolean vide = v == null || v.toString().isBlank() || (c.type().equals("CASE") && !Boolean.TRUE.equals(v));
            if (c.obligatoire() && vide)
                throw Introuvable.requete("Le champ « " + c.label() + " » est obligatoire");
            if (!vide && c.type().equals("TELEPHONE") && !v.toString().replaceAll("[\\s.+-]", "").matches("\\d{8,12}"))
                throw Introuvable.requete("Numéro de téléphone invalide");
            if (!vide && (c.type().equals("LISTE") || c.type().equals("CHOIX")) && c.options() != null
                    && !c.options().contains(v.toString()))
                throw Introuvable.requete("Réponse non prévue pour « " + c.label() + " »");
        }
    }
}
