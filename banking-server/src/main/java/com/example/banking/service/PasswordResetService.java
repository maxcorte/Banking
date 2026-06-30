package com.example.banking.service;

import com.example.banking.domain.PasswordResetToken;
import com.example.banking.domain.User;
import com.example.banking.exception.BankingException;
import com.example.banking.repository.PasswordResetTokenRepository;
import com.example.banking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.UUID;

/**
 * Reinitialisation de mot de passe par e-mail.
 *
 * Le jeton remis (dans le lien) est une valeur aleatoire opaque (256 bits) ; en
 * base on ne stocke que son hache SHA-256, a usage unique et a duree de vie
 * courte. Pour ne pas reveler quels e-mails existent, la demande renvoie
 * toujours le meme resultat, qu'un compte corresponde ou non.
 */
@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokens;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuditService auditService;
    private final long expirationMs;
    private final String baseUrl;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetTokenRepository tokens,
                                UserRepository users,
                                PasswordEncoder passwordEncoder,
                                MailService mailService,
                                AuditService auditService,
                                @Value("${app.reset.expiration-ms:1800000}") long expirationMs,
                                @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.tokens = tokens;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.auditService = auditService;
        this.expirationMs = expirationMs;
        this.baseUrl = baseUrl;
    }

    /** Demande de reinitialisation : cree un jeton et envoie le lien par e-mail.
     *  Silencieux si aucun compte ne correspond (anti-enumeration). */
    @Transactional
    public void requestReset(String email) {
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            return; // on ne revele rien
        }
        String raw = randomToken();
        PasswordResetToken token = new PasswordResetToken(
                UUID.randomUUID(),
                user.getId(),
                hash(raw),
                OffsetDateTime.now().plus(Duration.ofMillis(expirationMs)));
        tokens.save(token);

        String link = baseUrl + "/?reset=" + raw;
        mailService.sendPasswordReset(email, link);
        auditService.recordAs(user.getUsername(), "PASSWORD_RESET_REQUEST",
                "Demande de reinitialisation de mot de passe.");
    }

    /** Applique un nouveau mot de passe a partir d'un jeton valide (usage unique). */
    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BankingException(
                        "INVALID_RESET_TOKEN", "Lien invalide ou deja utilise."));
        if (token.isUsed() || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BankingException("INVALID_RESET_TOKEN", "Lien expire ou deja utilise.");
        }
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new BankingException("INVALID_RESET_TOKEN", "Compte introuvable."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        token.setUsed(true);
        tokens.save(token);
        auditService.recordAs(user.getUsername(), "PASSWORD_RESET",
                "Mot de passe reinitialise.");
    }

    private String randomToken() {
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
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
