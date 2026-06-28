package com.example.banking.service;

import com.example.banking.domain.Role;
import com.example.banking.domain.User;
import com.example.banking.exception.BankingException;
import com.example.banking.repository.UserRepository;
import com.example.banking.security.JwtService;
import com.example.banking.security.LoginRateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokens;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginRateLimiter rateLimiter,
                       AuditService auditService,
                       RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.auditService = auditService;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public void register(String username, String rawPassword) {
        if (users.existsByUsername(username)) {
            throw new BankingException("USERNAME_TAKEN", "Nom d'utilisateur déjà pris.");
        }
        User user = new User(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode(rawPassword),
                Role.USER);
        users.save(user);
        auditService.recordAs(username, "REGISTER", "Inscription d'un nouvel utilisateur.");
    }

    @Transactional
    public AuthResult login(String username, String rawPassword) {
        // Limitation anti-force-brute : bloque apres trop d'echecs.
        rateLimiter.checkAllowed(username);

        // Message volontairement identique dans les deux cas : on ne revele pas
        // si c'est le nom ou le mot de passe qui est faux (bonne pratique).
        User user = users.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            rateLimiter.recordFailure(username);
            auditService.recordAs(username, "LOGIN_FAILURE", "Echec de connexion.");
            throw new BankingException("BAD_CREDENTIALS", "Identifiants invalides.");
        }
        rateLimiter.reset(username);
        auditService.recordAs(username, "LOGIN_SUCCESS", "Connexion reussie.");
        return issueTokens(user);
    }

    /** Echange un refresh token valide contre une nouvelle paire (rotation). */
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        UUID userId = refreshTokens.consume(rawRefreshToken);
        User user = users.findById(userId)
                .orElseThrow(() -> new BankingException("INVALID_REFRESH", "Session invalide."));
        return issueTokens(user);
    }

    /** Deconnexion : revoque le refresh token fourni. */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokens.revoke(rawRefreshToken)
                .flatMap(users::findById)
                .ifPresent(user ->
                        auditService.recordAs(user.getUsername(), "LOGOUT", "Déconnexion."));
    }

    private AuthResult issueTokens(User user) {
        String access = jwtService.generateToken(user.getUsername(), user.getRole().name());
        String refresh = refreshTokens.issue(user.getId());
        return new AuthResult(access, refresh, user.getUsername(), user.getRole().name());
    }
}
