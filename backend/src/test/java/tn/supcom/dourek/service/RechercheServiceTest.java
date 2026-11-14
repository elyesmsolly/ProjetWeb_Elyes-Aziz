package tn.supcom.dourek.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tn.supcom.dourek.model.Categorie;

/** Compréhension d'une recherche en texte libre, sans IA. */
class RechercheServiceTest {

    @Test
    void devineLaCategorie() {
        assertThat(RechercheService.categorieDepuisTexte("coiffeur")).isEqualTo(Categorie.BARBIER);
        assertThat(RechercheService.categorieDepuisTexte("une coupe pas chère")).isEqualTo(Categorie.BARBIER);
        assertThat(RechercheService.categorieDepuisTexte("Brushing")).isEqualTo(Categorie.COIFFURE_FEMME);
        assertThat(RechercheService.categorieDepuisTexte("coupe femme")).isEqualTo(Categorie.COIFFURE_FEMME);
        assertThat(RechercheService.categorieDepuisTexte("prise de sang")).isEqualTo(Categorie.LABORATOIRE);
        assertThat(RechercheService.categorieDepuisTexte("payer ma facture")).isEqualTo(Categorie.AGENCE);
        assertThat(RechercheService.categorieDepuisTexte("extrait de naissance")).isEqualTo(Categorie.ADMINISTRATION);
    }

    @Test
    void neDevinePasQuandRienNeLIndique() {
        assertThat(RechercheService.categorieDepuisTexte("pizza")).isNull();
        assertThat(RechercheService.categorieDepuisTexte("Salon Lina")).isNull();
    }

    @Test
    void madameNEstPasUnSalonPourDames() {
        assertThat(RechercheService.categorieDepuisTexte("bonjour madame, une coupe homme")).isEqualTo(Categorie.BARBIER);
    }

    @Test
    void neGardeQueLesMotsUtiles() {
        assertThat(RechercheService.motsUtiles(RechercheService.normaliser("Une coupe pas cher près de moi"))).containsExactly("coupe");
    }

    @Test
    void coiffureSeulDesigneLesBarbiersEtLaCoiffureFemme() {
        assertThat(RechercheService.categoriesDepuisTexte("coiffure")).containsExactlyInAnyOrder(Categorie.BARBIER, Categorie.COIFFURE_FEMME);
        assertThat(RechercheService.categoriesDepuisTexte("une coupe")).containsExactlyInAnyOrder(Categorie.BARBIER, Categorie.COIFFURE_FEMME);
        assertThat(RechercheService.categoriesDepuisTexte("coiffure homme")).containsExactly(Categorie.BARBIER);
        assertThat(RechercheService.categoriesDepuisTexte("me raser la barbe")).containsExactly(Categorie.BARBIER);
        assertThat(RechercheService.categoriesDepuisTexte("coiffure pour femme")).containsExactly(Categorie.COIFFURE_FEMME);
        assertThat(RechercheService.categoriesDepuisTexte("vêtements homme")).isEmpty();
    }

    // ---- fautes de frappe

    private static final java.util.Set<String> VOCABULAIRE = RechercheService.vocabulaire(java.util.List.of(
            "Café des Jasmins Café Ariana Café express Thé à la menthe", "Salon Karim Barbiers Ariana Coupe homme Barbe", "Labo El Menzah Prise de sang"));

    private static String corrige(String texte) {
        return RechercheService.corriger(RechercheService.normaliser(texte), VOCABULAIRE, java.util.Set.of());
    }

    @Test
    void corrigeLesFautesDeFrappe() {
        assertThat(corrige("caffe")).isEqualTo("cafe");
        assertThat(corrige("Caffé")).isEqualTo("cafe");
        assertThat(corrige("cafes")).isEqualTo("cafe");
        assertThat(corrige("coifure")).isEqualTo("coiffure");
        assertThat(corrige("anlyse de sang")).isEqualTo("analyse de sang");
        assertThat(corrige("sgnature")).isEqualTo("signature");
        assertThat(corrige("un caffe à moins de 3.5 km")).isEqualTo("un cafe a moins de 3.5 km");
    }

    @Test
    void neToucheNiAuxMotsConnusNiAuxMotsCourts() {
        assertThat(corrige("coiffeur")).isEqualTo("coiffeur");
        assertThat(corrige("barbir")).isEqualTo("barbir"); // déjà reconnu comme « barbier » par les mots-clés
        assertThat(corrige("karim")).isEqualTo("karim");
        assertThat(corrige("sans attente")).isEqualTo("sans attente");
        assertThat(corrige("care")).isEqualTo("care");
        assertThat(corrige("pizza")).isEqualTo("pizza");
        assertThat(corrige("une fracture")).isEqualTo("une fracture"); // un vrai mot, pas « facture » mal écrit
        assertThat(corrige("")).isEmpty();
    }

    @Test
    void uneLettreDEcart() {
        assertThat(RechercheService.uneLettrePres("barbir", "barbier")).isTrue();
        assertThat(RechercheService.uneLettrePres("barbeir", "barbier")).isTrue();
        assertThat(RechercheService.uneLettrePres("barbier", "barbier")).isTrue();
        assertThat(RechercheService.uneLettrePres("garage", "barbier")).isFalse();
        assertThat(RechercheService.uneLettrePres("fracas", "facture")).isFalse();
    }
}
