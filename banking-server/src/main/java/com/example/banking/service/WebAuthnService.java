package com.example.banking.service;

import com.example.banking.domain.User;
import com.example.banking.domain.WebAuthnCredential;
import com.example.banking.exception.BankingException;
import com.example.banking.repository.WebAuthnCredentialRepository;
import com.example.banking.web.dto.WebAuthnStartResponse;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/** Orchestration des ceremonies WebAuthn (enregistrement + authentification). */
@Service
public class WebAuthnService {

    private final RelyingParty rp;
    private final WebAuthnCredentialRepository creds;
    private final WebAuthnFlowStore flows;
    private final SecureRandom random = new SecureRandom();

    public WebAuthnService(RelyingParty rp, WebAuthnCredentialRepository creds, WebAuthnFlowStore flows) {
        this.rp = rp;
        this.creds = creds;
        this.flows = flows;
    }

    /** Prepare l'enregistrement d'une passkey pour l'utilisateur connecte. */
    @Transactional(readOnly = true)
    public WebAuthnStartResponse startRegistration(User user) {
        WebAuthnCredential existing = creds.findFirstByUserId(user.getId()).orElse(null);
        ByteArray userHandle;
        if (existing != null) {
            userHandle = decodeHandle(existing.getUserHandle());
        } else {
            byte[] h = new byte[32];
            random.nextBytes(h);
            userHandle = new ByteArray(h);
        }

        PublicKeyCredentialCreationOptions options = rp.startRegistration(
                StartRegistrationOptions.builder()
                        .user(UserIdentity.builder()
                                .name(user.getUsername())
                                .displayName(user.getUsername())
                                .id(userHandle)
                                .build())
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                .residentKey(ResidentKeyRequirement.REQUIRED)
                                .userVerification(UserVerificationRequirement.REQUIRED)
                                .build())
                        .build());

        String flowId = flows.put(options);
        try {
            return new WebAuthnStartResponse(flowId, options.toCredentialsCreateJson());
        } catch (Exception e) {
            throw new BankingException("WEBAUTHN_FAILED", "Préparation de la passkey échouée.");
        }
    }

    /** Valide la reponse d'enregistrement et stocke la nouvelle passkey. */
    @Transactional
    public void finishRegistration(User user, String flowId, String credentialJson, String label) {
        PublicKeyCredentialCreationOptions request =
                flows.take(flowId, PublicKeyCredentialCreationOptions.class);
        try {
            var pkc = PublicKeyCredential.parseRegistrationResponseJson(credentialJson);
            RegistrationResult result = rp.finishRegistration(FinishRegistrationOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());

            WebAuthnCredential cred = new WebAuthnCredential(
                    UUID.randomUUID(),
                    user.getId(),
                    result.getKeyId().getId().getBase64Url(),
                    result.getPublicKeyCose().getBase64Url(),
                    result.getSignatureCount(),
                    request.getUser().getId().getBase64Url(),
                    (label == null || label.isBlank()) ? "Passkey" : label.trim());
            creds.save(cred);
        } catch (BankingException be) {
            throw be;
        } catch (Exception e) {
            throw new BankingException("WEBAUTHN_FAILED", "Enregistrement de la passkey échoué.");
        }
    }

    /** Prepare une authentification sans nom d'utilisateur (passkey decouvrable). */
    @Transactional(readOnly = true)
    public WebAuthnStartResponse startLogin() {
        AssertionRequest request = rp.startAssertion(StartAssertionOptions.builder().build());
        String flowId = flows.put(request);
        try {
            return new WebAuthnStartResponse(flowId, request.toCredentialsGetJson());
        } catch (Exception e) {
            throw new BankingException("WEBAUTHN_FAILED", "Préparation de la connexion échouée.");
        }
    }

    /** Valide la reponse d'authentification et renvoie le nom d'utilisateur. */
    @Transactional
    public String finishLogin(String flowId, String credentialJson) {
        AssertionRequest request = flows.take(flowId, AssertionRequest.class);
        try {
            var pkc = PublicKeyCredential.parseAssertionResponseJson(credentialJson);
            AssertionResult result = rp.finishAssertion(FinishAssertionOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());
            if (!result.isSuccess()) {
                throw new BankingException("BAD_CREDENTIALS", "Passkey non reconnue.");
            }
            creds.findByCredentialId(result.getCredentialId().getBase64Url())
                    .ifPresent(c -> {
                        c.setSignatureCount(result.getSignatureCount());
                        creds.save(c);
                    });
            return result.getUsername();
        } catch (BankingException be) {
            throw be;
        } catch (Exception e) {
            throw new BankingException("BAD_CREDENTIALS", "Connexion par passkey échouée.");
        }
    }

    @Transactional
    public void deleteCredential(UUID id, UUID userId) {
        creds.deleteByIdAndUserId(id, userId);
    }

    /** Decode base64url en encapsulant l'exception verifiee. */
    private ByteArray decodeHandle(String value) {
        try {
            return ByteArray.fromBase64Url(value);
        } catch (com.yubico.webauthn.data.exception.Base64UrlException e) {
            throw new IllegalStateException("User handle invalide", e);
        }
    }
}
