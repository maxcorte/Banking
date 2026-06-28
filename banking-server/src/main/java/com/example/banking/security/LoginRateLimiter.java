package com.example.banking.security;

import com.example.banking.exception.BankingException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limiteur de tentatives de connexion, en memoire.
 *
 * Apres {@value #MAX_ATTEMPTS} echecs dans une fenetre de {@value #WINDOW_MS}
 * millisecondes pour un meme identifiant, les tentatives sont bloquees pendant
 * la duree de la fenetre. Une connexion reussie remet le compteur a zero.
 *
 * Note pedagogique : pour une vraie production multi-instances, un stockage
 * partage (ex. Redis) serait necessaire. En memoire suffit pour une seule
 * instance, ce qui est notre cas.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes

    private record Attempts(int count, long firstAt, long blockedUntil) {}

    private final Map<String, Attempts> byKey = new ConcurrentHashMap<>();

    /** A appeler AVANT de verifier les identifiants. Leve une exception si bloque. */
    public void checkAllowed(String key) {
        Attempts a = byKey.get(normalize(key));
        if (a != null && a.blockedUntil() > System.currentTimeMillis()) {
            throw new BankingException(
                    "TOO_MANY_ATTEMPTS",
                    "Trop de tentatives de connexion. Réessayez dans quelques minutes.");
        }
    }

    /** A appeler apres un echec d'authentification. */
    public void recordFailure(String key) {
        String k = normalize(key);
        long now = System.currentTimeMillis();
        byKey.compute(k, (ignored, a) -> {
            if (a == null || now - a.firstAt() > WINDOW_MS) {
                return new Attempts(1, now, 0L);
            }
            int count = a.count() + 1;
            long blockedUntil = (count >= MAX_ATTEMPTS) ? now + WINDOW_MS : 0L;
            return new Attempts(count, a.firstAt(), blockedUntil);
        });
    }

    /** A appeler apres une connexion reussie. */
    public void reset(String key) {
        byKey.remove(normalize(key));
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
