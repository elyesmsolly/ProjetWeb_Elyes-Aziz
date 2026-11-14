package tn.supcom.dourek.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import tn.supcom.dourek.dto.Dtos.TicketVue;

/** « Mes tickets » : l'ordre dans lequel le client voit ses tickets. */
class FileServiceTest {

    private static final Instant MIDI = Instant.parse("2026-11-14T11:00:00Z");

    private static TicketVue ticket(String numero, String statut, int attente, int creeIlYaMinutes) {
        return new TicketVue("code-" + numero, numero, statut, 1L, "e1", "Établissement", "Adresse", 36.86, 10.19, "Prestation",
                attente / 5, attente, MIDI.plusSeconds(attente * 60L), MIDI.minusSeconds(creeIlYaMinutes * 60L), null, null, null, 1, false, List.of());
    }

    @Test
    void leTicketQuiPasseLePlusTotEstEnPremier() {
        List<TicketVue> tries = Stream.of(
                        ticket("B-12", "SERVI", 0, 300), ticket("L-07", "EN_ATTENTE", 40, 5), ticket("B-30", "EN_ATTENTE", 12, 2),
                        ticket("A-03", "ANNULE", 0, 60), ticket("C-02", "APPELE", 0, 20))
                .sorted(FileService.ORDRE_MES_TICKETS).toList();
        // Appelé d'abord, puis la plus courte attente ; ensuite les anciens tickets, du plus récent au plus ancien.
        assertThat(tries).extracting(TicketVue::numero).containsExactly("C-02", "B-30", "L-07", "A-03", "B-12");
    }
}
