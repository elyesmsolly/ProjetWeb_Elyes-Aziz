package tn.supcom.dourek.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Introuvable {
    private Introuvable() {}

    public static ResponseStatusException de(String quoi) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, quoi + " introuvable");
    }

    public static ResponseStatusException requete(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public static ResponseStatusException conflit(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
