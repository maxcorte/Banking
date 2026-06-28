package com.example.banking.service;

import com.example.banking.domain.RefreshToken;
import com.example.banking.exception.BankingException;
import com.example.banking.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Gere les refresh tokens : emission, rotation, revocation.
 *
 * Le jeton remis au client est une valeur aleatoire opaque (256 bits). En base
 * on ne stocke que son HACHE SHA-256 : meme en cas de fuite de la base, les
 * jetons ne sont pas réutilisables. La rotation (suppression a chaque usage)
 * limite la fenetre en cas de vol.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long refreshExpirationMs;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.repository = repository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /** Emet un nouveau refresh token pour l'utilisateur et renvoie sa valeur EN CLAIR. */
    @Transactional
    public String issue(UUID userId) {
        String raw = randomToken();
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                userId,
                hash(raw),
                OffsetDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)));
        repository.save(token);
        return raw;
    }

    /**
     * Valide un refresh token et le consomme (rotation) : l'ancien est supprime.
     * Renvoie l'identifiant de l'utilisateur. Leve une exception si invalide/expire.
     */
    @Transactional
    public UUID consume(String rawToken) {
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BankingException(
                        "INVALID_REFRESH", "Session expirée, veuillez vous reconnecter."));
        // Rotation : l'ancien ne pourra plus resservir, meme s'il etait encore valide.
        repository.delete(token);
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BankingException("INVALID_REFRESH", "Session expirée, veuillez vous reconnecter.");
        }
        return token.getUserId();
    }

    /** Revoque un refresh token (deconnexion). Renvoie l'utilisateur concerne si trouve. */
    @Transactional
    public Optional<UUID> revoke(String rawToken) {
        Optional<RefreshToken> token = repository.findByTokenHash(hash(rawToken));
        token.ifPresent(repository::delete);
        return token.map(RefreshToken::getUserId);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
