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
}
