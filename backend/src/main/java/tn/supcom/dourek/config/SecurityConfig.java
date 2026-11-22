package tn.supcom.dourek.config;

import org.springframework.http.HttpMethod;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import tn.supcom.dourek.repository.UtilisateurRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Sécurité : recherche publique ; tickets réservés au client connecté ; espace /api/pro protégé.
 * Le rôle (CLIENT, GERANT ou EMPLOYE) est dans le jeton ; les mots de passe sont hachés avec BCrypt.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtres(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/avis/**").authenticated()
                .requestMatchers("/api/pro/gerant/**").hasRole("GERANT")
                .requestMatchers("/api/pro/**").hasAnyRole("GERANT", "EMPLOYE")
                // Tout ce qui concerne un ticket est réservé au client connecté.
                .requestMatchers(HttpMethod.POST, "/api/etablissements/*/tickets").hasRole("CLIENT")
                .requestMatchers("/api/tickets/**", "/api/client/**").hasRole("CLIENT")
                .anyRequest().permitAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(convertisseur())));
        return http.build();
    }

    private JwtAuthenticationConverter convertisseur() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(roles);
        return c;
    }

    @Bean
    SecretKey cleJwt(@Value("${dourek.jwt.secret}") String secret) throws NoSuchAlgorithmException {
        // SHA-256 du secret : toujours une clé de 256 bits, quelle que soit la longueur du secret.
        if (secret.isBlank()) {
            // Sans DOUREK_JWT_SECRET : clé aléatoire à chaque démarrage, jamais une clé lisible dans le code.
            byte[] hasard = new byte[32];
            new SecureRandom().nextBytes(hasard);
            return new SecretKeySpec(hasard, "HmacSHA256");
        }
        byte[] octets = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(octets, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey cle) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(cle));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey cle, UtilisateurRepository utilisateurs) {
        NimbusJwtDecoder decodeur = NimbusJwtDecoder.withSecretKey(cle).macAlgorithm(MacAlgorithm.HS256).build();
        // Un compte retiré, suspendu ou dont le rôle a changé perd sa session tout de suite.
        OAuth2TokenValidator<Jwt> compteActif = jwt -> utilisateurs.findByEmailIgnoreCase(jwt.getSubject())
                .filter(u -> !u.isSuspendu() && u.getRole().name().equals(jwt.getClaimAsString("role")))
                .map(u -> OAuth2TokenValidatorResult.success())
                .orElseGet(() -> OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Compte inactif", null)));
        decodeur.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), compteActif));
        return decodeur;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Démo en local : le front (port 4200) et les téléphones du même réseau peuvent appeler l'API. */
    @Bean
    CorsConfigurationSource cors() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*", "http://192.168.*.*:*", "http://10.*.*.*:*", "http://172.*.*.*:*"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
