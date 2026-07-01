package com.example.banking.security.webauthn;

import com.example.banking.domain.User;
import com.example.banking.repository.UserRepository;
import com.example.banking.repository.WebAuthnCredentialRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Adapte le stockage JPA des passkeys a l'interface attendue par la lib Yubico. */
@Component
public class JpaCredentialRepository implements CredentialRepository {

    private final WebAuthnCredentialRepository creds;
    private final UserRepository users;

    public JpaCredentialRepository(WebAuthnCredentialRepository creds, UserRepository users) {
        this.creds = creds;
        this.users = users;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return users.findByUsername(username)
                .map(u -> creds.findByUserId(u.getId()).stream()
                        .map(c -> PublicKeyCredentialDescriptor.builder()
                                .id(ByteArray.fromBase64Url(c.getCredentialId()))
                                .build())
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return users.findByUsername(username)
                .flatMap(u -> creds.findFirstByUserId(u.getId()))
                .map(c -> ByteArray.fromBase64Url(c.getUserHandle()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return creds.findFirstByUserHandle(userHandle.getBase64Url())
                .flatMap(c -> users.findById(c.getUserId()))
                .map(User::getUsername);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return creds.findByCredentialId(credentialId.getBase64Url()).map(this::toRegistered);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return creds.findByCredentialId(credentialId.getBase64Url())
                .map(c -> Set.of(toRegistered(c)))
                .orElseGet(Set::of);
    }

    private RegisteredCredential toRegistered(com.example.banking.domain.WebAuthnCredential c) {
        return RegisteredCredential.builder()
                .credentialId(ByteArray.fromBase64Url(c.getCredentialId()))
                .userHandle(ByteArray.fromBase64Url(c.getUserHandle()))
                .publicKeyCose(ByteArray.fromBase64Url(c.getPublicKeyCose()))
                .signatureCount(c.getSignatureCount())
                .build();
    }
}
