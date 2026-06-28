package com.example.banking.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Verifie au demarrage que les secrets sensibles ne sont pas restes a leur
 * valeur par defaut. En local : simple avertissement. En deploiement
 * (app.security.enforce-secrets=true) : le demarrage echoue, pour eviter
 * d'exposer une application avec un secret JWT connu de tous.
 */
@Component
public class SecretsGuard {

    private static final Logger log = LoggerFactory.getLogger(SecretsGuard.class);

    private static final String DEFAULT_JWT =
            "dev-secret-change-me-please-32chars-minimum-0123456789";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin1234";

    private final String jwtSecret;
    private final String adminPassword;
    private final boolean enforce;

    public SecretsGuard(@Value("${app.jwt.secret}") String jwtSecret,
                        @Value("${app.admin.password:admin1234}") String adminPassword,
                        @Value("${app.security.enforce-secrets:false}") boolean enforce) {
        this.jwtSecret = jwtSecret;
        this.adminPassword = adminPassword;
        this.enforce = enforce;
    }

    @PostConstruct
    void check() {
        boolean weakJwt = DEFAULT_JWT.equals(jwtSecret);
        boolean weakAdmin = DEFAULT_ADMIN_PASSWORD.equals(adminPassword);

        if (!weakJwt && !weakAdmin) {
            return;
        }

        String message = "Secrets par defaut detectes ("
                + (weakJwt ? "JWT_SECRET " : "")
                + (weakAdmin ? "ADMIN_PASSWORD " : "")
                + "). Definissez des valeurs fortes avant toute exposition publique.";

        if (enforce) {
            throw new IllegalStateException(
                    message + " (app.security.enforce-secrets=true bloque le demarrage)");
        }
        log.warn("=====================================================================");
        log.warn("  ATTENTION : {}", message);
        log.warn("  OK en local, mais NE PAS exposer sur Internet en l'etat.");
        log.warn("=====================================================================");
    }
}
