package com.example.banking.security;

import com.example.banking.domain.User;
import com.example.banking.exception.ForbiddenException;
import com.example.banking.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resout l'utilisateur (entite) actuellement authentifie. */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Authentification requise.");
        }
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Utilisateur inconnu."));
    }
}
