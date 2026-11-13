package tn.supcom.dourek.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tn.supcom.dourek.dto.Dtos.ChampFormulaire;
import tn.supcom.dourek.service.FormulaireIaService.ChampGenere;

/** Le modèle propose, notre code contrôle : ces tests portent sur le contrôle et sur le mode sans IA. */
class FormulaireIaServiceTest {

    private static ChampGenere champ(String type, String label, List<String> options, String aide) {
        return new ChampGenere(type, label, false, options, aide);
    }

    @Test
    void retireLeNomEtLeTelephoneDejaPresents() {
        List<ChampFormulaire> champs = FormulaireIaService.controler(List.of(
                champ("TEXTE", "Nom et prénom", List.of(), ""),
                champ("TELEPHONE", "Numéro de téléphone :", List.of(), ""),
                champ("TEXTE", "Nom de l'animal", List.of(), ""),
                champ("TELEPHONE", "Téléphone du médecin", List.of(), "")));
        assertThat(champs).extracting(ChampFormulaire::label).containsExactly("Nom de l'animal", "Téléphone du médecin");
    }

    @Test
    void corrigeLesTypesEtLesOptions() {
        List<ChampFormulaire> champs = FormulaireIaService.controler(List.of(
                champ("liste", "Ordonnance :", List.of(" Papier ", "Numérique", "Papier", ""), "ignorée ?"),
                champ("CHOIX", "Une seule option", List.of("Oui"), ""),
                champ("INCONNU", "Médecin traitant", List.of("a", "b"), "Ex. Dr Ben Salah"),
                champ("CASE", "Je suis à jeun", List.of(), "pas d'aide sur une case")));

        assertThat(champs).extracting(ChampFormulaire::type).containsExactly("LISTE", "TEXTE", "TEXTE", "CASE");
        assertThat(champs.get(0).label()).isEqualTo("Ordonnance");
        assertThat(champs.get(0).options()).containsExactly("Papier", "Numérique");
        assertThat(champs.get(1).options()).isEmpty();
        assertThat(champs.get(2).options()).isEmpty();
        assertThat(champs.get(2).aide()).isEqualTo("Ex. Dr Ben Salah");
        assertThat(champs.get(3).aide()).isNull();
    }

    @Test
    void ignoreLesDoublonsLesVidesEtLimiteLeNombre() {
        List<ChampGenere> proposes = new java.util.ArrayList<>(List.of(
                champ("DATE", "Date de naissance", List.of(), ""),
                champ("TEXTE", "date de naissance :", List.of(), ""),
                champ("TEXTE", "   ", List.of(), "")));
        IntStream.range(0, 20).forEach(i -> proposes.add(champ("TEXTE", "Champ " + i, List.of(), "")));

        List<ChampFormulaire> champs = FormulaireIaService.controler(proposes);

        assertThat(champs).hasSize(FormulaireIaService.MAX_CHAMPS);
        assertThat(champs.get(0).type()).isEqualTo("DATE");
        assertThat(champs).extracting(ChampFormulaire::cle).doesNotHaveDuplicates().allMatch(c -> c.matches("champ_[a-z0-9]{6}"));
    }

    @Test
    void coupeLesTextesTropLongs() {
        List<ChampFormulaire> champs = FormulaireIaService.controler(List.of(
                champ("LISTE", "x".repeat(200), List.of("o".repeat(100), "autre"), "a".repeat(300))));
        assertThat(champs.get(0).label()).hasSize(80);
        assertThat(champs.get(0).options().get(0)).hasSize(60);
        assertThat(champs.get(0).aide()).hasSize(120);
    }

    @Test
    void sansIaReconnaitLesMotsCles() {
        List<ChampGenere> champs = FormulaireIaService.proposerLocalement(
                "Laboratoire d'analyses : date de naissance, à jeun ou non, type d'ordonnance", null);
        assertThat(champs).extracting(ChampGenere::label).containsExactly("Date de naissance", "Je suis à jeun", "Ordonnance");
    }

    @Test
    void sansIaNeConfondPasRemarqueEtMarque() {
        List<ChampGenere> champs = FormulaireIaService.proposerLocalement("une remarque et l'adresse e-mail", null);
        assertThat(champs).extracting(ChampGenere::label).containsExactly("E-mail", "Remarque");
    }

    @Test
    void sansIaEtSansMotCleProposeLesChampsDeLActivite() {
        assertThat(FormulaireIaService.proposerLocalement("Salon de coiffure pour hommes", null))
                .extracting(ChampGenere::label).containsExactly("Coiffeur préféré", "Remarque pour le coiffeur");
        assertThat(FormulaireIaService.proposerLocalement("Mon commerce", "Pressing"))
                .extracting(ChampGenere::label).containsExactly("Motif de la visite", "Remarque");
    }

    @Test
    void lesPropositionsLocalesSontToujoursPubliables() {
        for (String d : List.of("laboratoire", "agence", "mairie", "vétérinaire", "garage voiture marque", "pressing vêtements sms")) {
            List<ChampFormulaire> champs = FormulaireIaService.controler(FormulaireIaService.proposerLocalement(d, null));
            assertThat(champs).isNotEmpty().allSatisfy(c -> {
                assertThat(FormulaireService.TYPES).contains(c.type());
                boolean aOptions = c.type().equals("LISTE") || c.type().equals("CHOIX");
                assertThat(c.options().size() >= 2).isEqualTo(aOptions);
            });
        }
    }
}
