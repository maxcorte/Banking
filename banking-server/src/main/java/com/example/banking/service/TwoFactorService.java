package com.example.banking.service;

import com.example.banking.domain.User;
import com.example.banking.domain.UserTotp;
import com.example.banking.exception.InvalidTransferException;
import com.example.banking.exception.TwoFactorRequiredException;
import com.example.banking.repository.UserTotpRepository;
import com.example.banking.web.dto.TotpSetupResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Gestion de la 2FA (TOTP) : enrolement, confirmation, desactivation, verification. */
@Service
public class TwoFactorService {

    private static final String ISSUER = "Ma Banque";

    private final UserTotpRepository repository;
    private final TotpService totp;

    public TwoFactorService(UserTotpRepository repository, TotpService totp) {
        this.repository = repository;
        this.totp = totp;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId) {
        return repository.findById(userId).map(UserTotp::isEnabled).orElse(false);
    }

    /** Prepare (ou re-prepare) un secret non confirme et renvoie le QR/secret. */
    @Transactional
    public TotpSetupResponse setup(User user) {
        UserTotp entry = repository.findById(user.getId()).orElse(null);
        if (entry != null && entry.isEnabled()) {
            throw new InvalidTransferException("La 2FA est déjà activée.");
        }
        String secret = totp.generateSecret();
        if (entry == null) {
            entry = new UserTotp(user.getId(), secret);
        } else {
            entry.resetSecret(secret);
        }
        repository.save(entry);
        String uri = totp.buildOtpAuthUri(secret, user.getUsername(), ISSUER);
        return new TotpSetupResponse(secret, uri);
    }

    /** Confirme l'enrolement avec un premier code valide. */
    @Transactional
    public void enable(User user, String code) {
        UserTotp entry = repository.findById(user.getId())
                .orElseThrow(() -> new InvalidTransferException("Commence par générer un QR code."));
        if (entry.isEnabled()) {
            throw new InvalidTransferException("La 2FA est déjà activée.");
        }
        if (!totp.verify(entry.getSecret(), code)) {
            throw new InvalidTransferException("Code invalide. Réessaie.");
        }
        entry.confirm();
        repository.save(entry);
    }

    /** Desactive la 2FA (necessite un code valide). */
    @Transactional
    public void disable(User user, String code) {
        UserTotp entry = repository.findById(user.getId()).orElse(null);
        if (entry == null || !entry.isEnabled()) {
            return; // deja desactivee
        }
        if (!totp.verify(entry.getSecret(), code)) {
            throw new InvalidTransferException("Code invalide. Réessaie.");
        }
        repository.delete(entry);
    }

    /**
     * A appeler avant une operation sensible : si la 2FA est active, exige un
     * code valide. Sinon, ne fait rien.
     */
    @Transactional(readOnly = true)
    public void requireCodeIfEnabled(UUID userId, String code) {
        UserTotp entry = repository.findById(userId).orElse(null);
        if (entry == null || !entry.isEnabled()) {
            return;
        }
        if (!totp.verify(entry.getSecret(), code)) {
            throw new TwoFactorRequiredException(
                    "Code d'authentification à deux facteurs requis ou invalide.");
        }
    }
}
