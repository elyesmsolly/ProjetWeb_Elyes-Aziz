package tn.supcom.dourek.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import tn.supcom.dourek.dto.Dtos.Criteres;
import tn.supcom.dourek.dto.Dtos.EtablissementResume;
import tn.supcom.dourek.dto.Dtos.PrestationDto;
import tn.supcom.dourek.model.Categorie;
import tn.supcom.dourek.service.AssistantService.Comprehension;
import tn.supcom.dourek.service.AssistantService.Demande;

/** Recherche avec l'IA : ce que NOTRE code contrôle (sans appeler de modèle), et le mode sans IA. */
class AssistantServiceTest {

    private static final EtablissementResume KARIM = fiche(1, "Salon Karim", Categorie.BARBIER, "Barbiers", "Ariana", 0.4, 34, true, "Coupe homme", "Barbe");
    private static final EtablissementResume ENNASR = fiche(2, "Barber Ennasr", Categorie.BARBIER, "Barbiers", "Ennasr", 4.2, 12, true, "Coupe homme", "Barbe");
    private static final EtablissementResume FERME = fiche(3, "Barbershop 2080", Categorie.BARBIER, "Barbiers", "Ariana", 1.1, 0, false, "Coupe homme");
    private static final EtablissementResume LINA = fiche(4, "Salon Lina", Categorie.COIFFURE_FEMME, "Coiffure femme", "Ariana", 3.3, 82, true, "Brushing", "Coupe femme");
    private static final EtablissementResume LABO = fiche(5, "Labo El Menzah", Categorie.LABORATOIRE, "Laboratoires", "El Menzah", 5.0, 13, true, "Prise de sang");
    private static final EtablissementResume CAFE = fiche(6, "Café des Jasmins", Categorie.AUTRE, "Café", "Ariana", 0.8, 6, true, "Café express", "Jus d'orange frais");
    private static final EtablissementResume GHAZELA = fiche(8, "Café El Ghazela", Categorie.AUTRE, "Café", "El Ghazela", 3.6, 15, true, "Café crème", "Cappuccino");
    private static final EtablissementResume PATIO = fiche(7, "Le Patio", Categorie.AUTRE, "Salon de thé", "El Menzah", 5.3, 10, true, "Table pour 2");
    /** Comme dans le service : toutes les fiches, triées par notre score. */
    private static final List<EtablissementResume> TOUS = List.of(KARIM, ENNASR, FERME, LINA, LABO, CAFE, GHAZELA, PATIO).stream()
            .sorted(Comparator.comparingDouble(EtablissementResume::score)).toList();

    private static EtablissementResume fiche(long id, String nom, Categorie categorie, String type, String quartier, double km,
                                             int attente, boolean ouverte, String... prestations) {
        List<PrestationDto> p = Arrays.stream(prestations).map(n -> new PrestationDto(id * 10, n, "", 10, 10)).toList();
        return new EtablissementResume(id, "e" + id, nom, categorie.name(), type, quartier, "Rue " + id + ", " + quartier, 36.86, 10.19,
                km, 4.5, 10, ouverte, 20, attente / 10, attente, 1, p, RechercheService.score(attente, km, 4.5, ouverte), false);
    }

    private static final Criteres AUCUN = new Criteres(null, null, null, null, null, null, false, null);

    // ---- mode sans IA

    @Test
    void coiffureSansIaListeTousLesSalons() {
        Demande d = AssistantService.comprendreLocalement("coiffure", TOUS);
        assertThat(d.ids()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }

    @Test
    void boireQuelqueChoseSansIaTrouveLeCafeEtLeSalonDeThe() {
        Demande d = AssistantService.comprendreLocalement("je veux boire quelque chose", TOUS);
        assertThat(d.ids()).containsExactlyInAnyOrder(6L, 7L, 8L);
    }

    @Test
    void laDistanceMaximaleEstLueDansLaPhrase() {
        Demande d = AssistantService.comprendreLocalement("une coiffure dans moins de 3.5 kM", TOUS);
        assertThat(d.criteres().distanceMaxKm()).isEqualTo(3.5);
        assertThat(d.criteres().attenteMax()).isNull();
        assertThat(AssistantService.classer(TOUS.stream().filter(e -> d.ids().contains(e.id())).toList(), d.criteres(), 600))
                .extracting(EtablissementResume::nom).containsExactly("Salon Karim", "Salon Lina", "Barbershop 2080");
        assertThat(AssistantService.comprendreLocalement("un café à 500 m", TOUS).criteres().distanceMaxKm()).isEqualTo(0.5);
    }

    @Test
    void sansBesoinPrecisTousLesEtablissementsSontGardes() {
        assertThat(AssistantService.comprendreLocalement("le plus proche, moins de 20 min", TOUS).ids()).hasSize(TOUS.size());
    }

    @Test
    void unBesoinInconnuNeRenvoieRien() {
        assertThat(AssistantService.comprendreLocalement("réparer mon vélo", TOUS).ids()).isEmpty();
        assertThat(AssistantService.expliquerLocalement(List.of(), 0)).startsWith("Aucun établissement");
    }

    @Test
    void lesCriteresSontExtraitsSansIa() {
        Demande d = AssistantService.comprendreLocalement("Une barbe à Ennasr avant 18 h, pas trop cher", TOUS);
        assertThat(d.ids()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(d.criteres().prestation()).isEqualTo("Barbe");
        assertThat(d.criteres().quartier()).isEqualTo("ennasr");
        assertThat(d.criteres().avantHeure()).isEqualTo(18);
        assertThat(d.criteres().petitPrix()).isTrue();
    }

    @Test
    void uneFauteDeFrappeNEmpechePasDeTrouverTousLesCafes() {
        for (String demande : List.of("caffe", "un caffe svp", "cafes", "Café", "caffé pas cher", "coffee")) {
            Demande d = AssistantService.comprendreLocalement(demande, TOUS);
            assertThat(d.ids()).as(demande).containsExactlyInAnyOrder(6L, 7L, 8L);
            assertThat(d.typeSeul()).as(demande).isTrue();
        }
        assertThat(AssistantService.comprendreLocalement("coifure", TOUS).ids()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(AssistantService.comprendreLocalement("barbir", TOUS).ids()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void uneDemandeDeTypeSeulEstReconnue() {
        assertThat(AssistantService.comprendreLocalement("coiffure dans moins de 3.5 km", TOUS).typeSeul()).isTrue();
        assertThat(AssistantService.comprendreLocalement("je veux boire quelque chose", TOUS).typeSeul()).isTrue();
        assertThat(AssistantService.comprendreLocalement("une coiffure pour ma femme", TOUS).typeSeul()).isTrue();
        // Un autre besoin, un établissement nommé ou aucun besoin : le modèle décide seul.
        assertThat(AssistantService.comprendreLocalement("une coupe pour mon fils", TOUS).typeSeul()).isFalse();
        assertThat(AssistantService.comprendreLocalement("Barber Ennasr", TOUS).typeSeul()).isFalse();
        assertThat(AssistantService.comprendreLocalement("café des jasmins", TOUS).typeSeul()).isFalse();
        assertThat(AssistantService.comprendreLocalement("le plus proche", TOUS).typeSeul()).isFalse();
        assertThat(AssistantService.comprendreLocalement("je veux manger une pizza", TOUS).typeSeul()).isFalse();
    }

    // ---- contrôle de ce que renvoie le modèle

    @Test
    void unTypeCiteParLeModeleAmeneTousSesEtablissements() {
        Demande d = AssistantService.depuisComprehension(new Comprehension("Café", List.of("Cafés", "salon de thé", "Type inventé"), List.of(6L), "", 0, 0, 0, false, ""), TOUS);
        assertThat(d.ids()).containsExactlyInAnyOrder(6L, 7L, 8L);
        // « Barbier » pour le type « Barbiers » : le pluriel ne compte pas.
        assertThat(AssistantService.depuisComprehension(new Comprehension("Barbe", List.of("Barbier"), List.of(), "", 0, 0, 0, false, ""), TOUS).ids())
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void siLeModeleOublieUnCafeNotreCodeLAjoute() {
        Demande modele = AssistantService.depuisComprehension(new Comprehension("Café", List.of(), List.of(6L), "", 0, 0, 0, false, ""), TOUS);
        Demande complete = AssistantService.completer(modele, AssistantService.comprendreLocalement("caffe", TOUS), TOUS);
        assertThat(complete.ids()).containsExactlyInAnyOrder(6L, 7L, 8L);
        assertThat(complete.criteres()).isEqualTo(modele.criteres());
        // Le modèle a compris le besoin mais n'a cité aucun établissement : nos règles donnent la liste.
        Demande vide = AssistantService.depuisComprehension(new Comprehension("Boire un café", List.of(), List.of(), "", 0, 0, 0, false, ""), TOUS);
        assertThat(AssistantService.completer(vide, AssistantService.comprendreLocalement("caffe", TOUS), TOUS).ids()).containsExactlyInAnyOrder(6L, 7L, 8L);
    }

    @Test
    void leFiletNeChangeRienQuandLeModeleEtNosReglesNeSontPasDAccord() {
        // Le modèle n'a retenu aucun des établissements que nos règles proposent : on garde sa sélection.
        Demande modele = AssistantService.depuisComprehension(new Comprehension("Analyse", List.of(), List.of(5L), "", 0, 0, 0, false, ""), TOUS);
        assertThat(AssistantService.completer(modele, AssistantService.comprendreLocalement("caffe", TOUS), TOUS).ids()).containsExactly(5L);
        Demande rien = AssistantService.depuisComprehension(new Comprehension("Soigner une fracture", List.of(), List.of(), "", 0, 0, 0, false, ""), TOUS);
        assertThat(AssistantService.completer(rien, AssistantService.comprendreLocalement("caffe", TOUS), TOUS).ids()).isEmpty();
        // Demande plus précise qu'un simple type : pas de complément.
        Demande fils = AssistantService.depuisComprehension(new Comprehension("Coupe enfant", List.of(), List.of(1L), "", 0, 0, 0, false, ""), TOUS);
        assertThat(AssistantService.completer(fils, AssistantService.comprendreLocalement("une coupe pour mon fils", TOUS), TOUS).ids()).containsExactly(1L);
    }


    @Test
    void lesIdentifiantsInconnusDuModeleSontIgnores() {
        Demande d = AssistantService.depuisComprehension(new Comprehension("Boire un café", List.of(), Arrays.asList(6L, 999L, null, 7L), "", 0, 0, 0, false, "n'importe quoi"), TOUS);
        assertThat(d.ids()).containsExactly(6L, 7L);
        assertThat(d.criteres().besoin()).isEqualTo("Boire un café");
        assertThat(d.criteres().quartier()).isNull();
        assertThat(d.criteres().distanceMaxKm()).isNull();
        assertThat(d.criteres().avantHeure()).isNull();
        assertThat(d.criteres().priorite()).isNull();
    }

    @Test
    void leChoixDeLAssistantEstLePremierOuvertDuClassement() {
        List<EtablissementResume> liste = AssistantService.marquerChoix(List.of(ENNASR, KARIM, LINA));
        assertThat(liste).extracting(EtablissementResume::nom).containsExactly("Barber Ennasr", "Salon Karim", "Salon Lina");
        assertThat(liste).extracting(EtablissementResume::meilleurChoix).containsExactly(true, false, false);
        // une file en pause n'est jamais le choix, même si elle est seule
        assertThat(AssistantService.marquerChoix(List.of(FERME))).extracting(EtablissementResume::meilleurChoix).containsExactly(false);
    }

    @Test
    void laPrioriteDuClientChangeLeClassement() {
        List<EtablissementResume> coiffure = List.of(KARIM, ENNASR, FERME, LINA);
        assertThat(AssistantService.classer(coiffure, new Criteres(null, null, null, null, null, null, false, "DISTANCE"), 600))
                .extracting(EtablissementResume::nom).containsExactly("Salon Karim", "Salon Lina", "Barber Ennasr", "Barbershop 2080");
        assertThat(AssistantService.classer(coiffure, new Criteres(null, null, null, null, null, null, false, "ATTENTE"), 600))
                .extracting(EtablissementResume::nom).containsExactly("Barber Ennasr", "Salon Karim", "Salon Lina", "Barbershop 2080");
        assertThat(AssistantService.comprendreLocalement("le coiffeur le plus proche", TOUS).criteres().priorite()).isEqualTo("DISTANCE");
        assertThat(AssistantService.comprendreLocalement("une coupe le plus vite possible", TOUS).criteres().priorite()).isEqualTo("ATTENTE");
        assertThat(AssistantService.comprendreLocalement("le barbier le mieux noté", TOUS).criteres().priorite()).isEqualTo("NOTE");
    }

    @Test
    void lesContraintesSontAppliqueesParNotreCode() {
        List<EtablissementResume> coiffure = List.of(KARIM, ENNASR, FERME, LINA);
        // attente de 20 min au plus : les files en pause et les attentes plus longues sont écartées
        assertThat(AssistantService.classer(coiffure, new Criteres(null, null, null, null, 20, null, false, null), 600))
                .extracting(EtablissementResume::nom).containsExactly("Barber Ennasr");
        // avant 11 h, à 10 h 30 : seul celui qui fait passer en moins de 30 min reste
        assertThat(AssistantService.classer(coiffure, new Criteres(null, null, null, 11, null, null, false, null), 630))
                .extracting(EtablissementResume::nom).containsExactly("Barber Ennasr");
        // quartier : seuls les établissements sur place, s'il y en a
        assertThat(AssistantService.classer(coiffure, new Criteres(null, null, "Ennasr", null, null, null, false, null), 600))
                .extracting(EtablissementResume::nom).containsExactly("Barber Ennasr");
        // sans contrainte : tout le monde, les files en pause en dernier
        assertThat(AssistantService.classer(coiffure, AUCUN, 600)).hasSize(4).last().isEqualTo(FERME);
    }

    @Test
    void laMiseEnFormeDuModeleEstRetiree() {
        assertThat(AssistantService.texteSimple("Je vous recommande *Coiff’ Style Ariana*,\n**10 min** d'attente.  "))
                .isEqualTo("Je vous recommande Coiff’ Style Ariana, 10 min d'attente.");
        assertThat(AssistantService.texteSimple("  ** ")).isNull();
        assertThat(AssistantService.depuisComprehension(new Comprehension("boire un verre", List.of(), List.of(6L), "", 0, 0, 0, false, ""), TOUS).criteres().besoin())
                .isEqualTo("Boire un verre");
    }

    @Test
    void leSchemaEnvoyeAClaudeEstValide() {
        assertThatCode(() -> AssistantService.parametresClaude("claude-opus-5-5", "consigne", "message", Comprehension.class, 100L)).doesNotThrowAnyException();
    }
}
