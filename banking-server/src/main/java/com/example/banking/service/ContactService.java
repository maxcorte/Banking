package com.example.banking.service;

import com.example.banking.domain.Contact;
import com.example.banking.domain.User;
import com.example.banking.exception.InvalidTransferException;
import com.example.banking.repository.ContactRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.web.dto.ContactResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Gestion du carnet de contacts d'un utilisateur. */
@Service
public class ContactService {

    private final ContactRepository contacts;
    private final UserRepository users;

    public ContactService(ContactRepository contacts, UserRepository users) {
        this.contacts = contacts;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> list(UUID ownerUserId) {
        return contacts.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
                .map(c -> {
                    String name = users.findById(c.getContactUserId())
                            .map(User::getUsername).orElse("—");
                    return new ContactResponse(c.getContactUserId(), name);
                })
                .toList();
    }

    /** Ajoute un contact par son nom d'utilisateur. */
    @Transactional
    public ContactResponse add(User owner, String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidTransferException("Nom d'utilisateur requis.");
        }
        User target = users.findByUsername(username.trim())
                .orElseThrow(() -> new InvalidTransferException("Utilisateur introuvable."));
        if (target.getId().equals(owner.getId())) {
            throw new InvalidTransferException("Vous ne pouvez pas vous ajouter vous-même.");
        }
        if (contacts.existsByOwnerUserIdAndContactUserId(owner.getId(), target.getId())) {
            throw new InvalidTransferException("Ce contact existe déjà.");
        }
        contacts.save(new Contact(UUID.randomUUID(), owner.getId(), target.getId()));
        return new ContactResponse(target.getId(), target.getUsername());
    }

    @Transactional
    public void remove(User owner, UUID contactUserId) {
        contacts.findByOwnerUserIdAndContactUserId(owner.getId(), contactUserId)
                .ifPresent(contacts::delete);
    }
}
