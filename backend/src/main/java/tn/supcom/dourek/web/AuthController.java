package tn.supcom.dourek.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.supcom.dourek.dto.Dtos.*;
import tn.supcom.dourek.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/connexion")
    public AuthReponse connexion(@Valid @RequestBody LoginRequest req) {
        return auth.connexion(req);
    }

    @PostMapping("/inscription-client")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthReponse inscriptionClient(@Valid @RequestBody InscriptionClientRequest req) {
        return auth.inscriptionClient(req);
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthReponse inscription(@Valid @RequestBody InscriptionRequest req) {
        return auth.inscription(req);
    }
}
