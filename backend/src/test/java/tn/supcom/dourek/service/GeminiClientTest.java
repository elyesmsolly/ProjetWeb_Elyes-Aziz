package tn.supcom.dourek.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ordre des modèles essayés quand celui de la configuration ne répond pas. */
class GeminiClientTest {

    private static List<String> classes(String... noms) {
        return List.of(noms).stream().sorted(Comparator.comparingDouble(GeminiClient::note).reversed()).toList();
    }

    @Test
    void prefereFlashLitePuisFlashPuisLesAutres() {
        assertThat(classes("gemini-4-hydrogen", "gemini-3.5-flash", "gemini-3.5-flash-lite"))
                .containsExactly("gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-4-hydrogen");
    }

    @Test
    void aFamilleEgalePrefereLeStableLePlusRecent() {
        assertThat(classes("gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-4-flash-lite-preview").getFirst())
                .isEqualTo("gemini-3.5-flash-lite");
        assertThat(classes("gemini-3.6-flash", "gemini-3.8-flash", "gemini-3-flash-preview").getFirst())
                .isEqualTo("gemini-3.8-flash");
    }

    @Test
    void uneErreurDeCleOuDeSchemaNeFaitPasChangerDeModele() {
        assertThat(new GeminiClient.ErreurGemini(400, "API key not valid").autreModelePossible()).isFalse();
        assertThat(new GeminiClient.ErreurGemini(403, "refusé").autreModelePossible()).isFalse();
        assertThat(new GeminiClient.ErreurGemini(404, "modèle retiré").autreModelePossible()).isTrue();
        assertThat(new GeminiClient.ErreurGemini(429, "quota").autreModelePossible()).isTrue();
        assertThat(new GeminiClient.ErreurGemini(503, "surcharge").autreModelePossible()).isTrue();
    }
}
