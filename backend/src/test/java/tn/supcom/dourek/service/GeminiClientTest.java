package tn.supcom.dourek.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Choix du modèle quand celui de la configuration n'existe plus. */
class GeminiClientTest {

    private static String prefere(String... noms) {
        return List.of(noms).stream().max(Comparator.comparingDouble(GeminiClient::note)).orElseThrow();
    }

    @Test
    void prefereLeFlashStableLePlusRecent() {
        assertThat(prefere("gemini-2.5-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-4-hydrogen",
                "gemini-4-flash-preview")).isEqualTo("gemini-3.5-flash");
    }

    @Test
    void prefereFlashLiteAuxModelesPayants() {
        assertThat(prefere("gemini-3.1-pro-preview", "gemini-3.1-flash-lite", "gemini-4-hydrogen")).isEqualTo("gemini-3.1-flash-lite");
    }
}
