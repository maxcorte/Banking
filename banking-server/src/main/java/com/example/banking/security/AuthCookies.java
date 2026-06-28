package com.example.banking.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Centralise la gestion des cookies d'authentification.
 *
 * - access_token  : JWT court, envoye a toutes les routes (path "/").
 * - refresh_token : jeton long, restreint aux routes /api/auth.
 *
 * Tous deux sont httpOnly (invisibles au JavaScript, donc non volables par XSS),
 * Secure (HTTPS) et SameSite=Strict (envoyes uniquement depuis notre propre
 * site : protection contre le CSRF).
 */
@Component
public class AuthCookies {

    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";
    private static final String REFRESH_PATH = "/api/auth";

    private final boolean secure;
    private final long accessMaxAgeMs;
    private final long refreshMaxAgeMs;

    public AuthCookies(@Value("${app.auth.cookie-secure:true}") boolean secure,
                       @Value("${app.jwt.access-expiration-ms}") long accessMaxAgeMs,
                       @Value("${app.jwt.refresh-expiration-ms}") long refreshMaxAgeMs) {
        this.secure = secure;
        this.accessMaxAgeMs = accessMaxAgeMs;
        this.refreshMaxAgeMs = refreshMaxAgeMs;
    }

    public void write(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(ACCESS, accessToken, "/", Duration.ofMillis(accessMaxAgeMs)).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(REFRESH, refreshToken, REFRESH_PATH, Duration.ofMillis(refreshMaxAgeMs)).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS, "", "/", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH, "", REFRESH_PATH, Duration.ZERO).toString());
    }

    public Optional<String> read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (var cookie : request.getCookies()) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
