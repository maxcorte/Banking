package com.example.banking.security;

import com.example.banking.domain.Account;
import com.example.banking.domain.Role;
import com.example.banking.domain.User;
import com.example.banking.exception.ForbiddenException;
import org.springframework.stereotype.Component;

/** Regles d'autorisation : qui a le droit d'agir sur quel compte. */
@Component
public class AccessControl {

    /** Autorise si l'utilisateur est ADMIN ou proprietaire du compte. */
    public void assertOwnsOrAdmin(Account account, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (account.getOwnerId() == null || !account.getOwnerId().equals(user.getId())) {
            throw new ForbiddenException("Ce compte ne vous appartient pas.");
        }
    }

    /** Autorise uniquement les ADMIN. */
    public void assertAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Action réservée à l'administrateur.");
        }
    }
}
