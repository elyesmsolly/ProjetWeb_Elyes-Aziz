package tn.supcom.dourek.config;

import org.springframework.http.HttpMethod;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
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
        byte[] octets = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(octets, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey cle) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(cle));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey cle) {
        return NimbusJwtDecoder.withSecretKey(cle).macAlgorithm(MacAlgorithm.HS256).build();
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
