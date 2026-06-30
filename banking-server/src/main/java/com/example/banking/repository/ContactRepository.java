package com.example.banking.repository;

import com.example.banking.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    List<Contact> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    boolean existsByOwnerUserIdAndContactUserId(UUID ownerUserId, UUID contactUserId);

    Optional<Contact> findByOwnerUserIdAndContactUserId(UUID ownerUserId, UUID contactUserId);
}
