package tn.supcom.dourek.web;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Erreurs de l'API au format { status, message } : le front affiche « message » tel quel
 * (« E-mail ou mot de passe incorrect », « La file est en pause »…).
 */
@RestControllerAdvice
public class ErreursApi {

    private static ResponseEntity<Map<String, Object>> erreur(int statut, String message) {
        return ResponseEntity.status(statut).body(Map.of("status", statut, "message", message));
    }

    /** Erreurs métier levées par les services. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> metier(ResponseStatusException ex) {
        String message = ex.getReason() == null || ex.getReason().isBlank() ? "Une erreur est survenue" : ex.getReason();
        return erreur(ex.getStatusCode().value(), message);
    }

    /** Requête refusée par la validation (@Valid) : on nomme les champs en cause. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String champs = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getField).distinct()
                .collect(Collectors.joining(", "));
        return erreur(400, "Champ manquant ou invalide : " + (champs.isBlank() ? "vérifiez votre saisie" : champs));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> illisible(HttpMessageNotReadableException ex) {
        return erreur(400, "Requête illisible : vérifiez les données envoyées");
    }
}
