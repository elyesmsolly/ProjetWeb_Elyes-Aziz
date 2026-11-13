package tn.supcom.dourek.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Accès à l'API Claude (SDK officiel Anthropic), partagé par l'assistant de recherche et le générateur de formulaire.
 * La clé vient de la variable d'environnement ANTHROPIC_API_KEY : elle n'est jamais écrite dans le code.
 */
@Component
public class ClaudeClient {

    private final AnthropicClient api;
    private final String modele;

    public ClaudeClient(@Value("${dourek.assistant.modele}") String modele,
                        @Value("${ANTHROPIC_API_KEY:}") String cle) {
        this.modele = modele;
        this.api = cle.isBlank() ? null : AnthropicOkHttpClient.builder().apiKey(cle.trim()).build();
    }

    public boolean actif() {
        return api != null;
    }

    public AnthropicClient api() {
        return api;
    }

    public String modele() {
        return modele;
    }
}
