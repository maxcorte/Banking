package com.example.banking.repository;

import com.example.banking.domain.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, UUID> {
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);
    List<WebAuthnCredential> findByUserId(UUID userId);
    Optional<WebAuthnCredential> findFirstByUserId(UUID userId);
    Optional<WebAuthnCredential> findFirstByUserHandle(String userHandle);
    void deleteByIdAndUserId(UUID id, UUID userId);
}
