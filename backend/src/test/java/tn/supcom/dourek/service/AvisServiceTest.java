package tn.supcom.dourek.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class AvisServiceTest {

    @Test
    void ajouterPuisRetirerUnAvisRendLaMoyenneDeDepart() {
        double avec = AvisService.moyenneAvec(4.7, 128, 1);
        assertThat(avec).isLessThan(4.7);
        assertThat(AvisService.moyenneSans(avec, 129, 1)).isCloseTo(4.7, within(1e-9));
    }

    @Test
    void premierEtDernierAvis() {
        assertThat(AvisService.moyenneAvec(0, 0, 4)).isEqualTo(4);
        assertThat(AvisService.moyenneSans(4, 1, 4)).isZero();
    }

    @Test
    void leNomCompletDUnClientNEstJamaisPublic() {
        assertThat(AvisService.nomPublic("Yassine Trabelsi")).isEqualTo("Yassine T.");
        assertThat(AvisService.nomPublic(" Amine ")).isEqualTo("Amine");
        assertThat(AvisService.nomPublic(null)).isEqualTo("Client");
    }
}
