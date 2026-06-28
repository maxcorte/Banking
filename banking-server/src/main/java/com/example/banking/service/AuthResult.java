package com.example.banking.service;

/** Resultat d'une connexion / rafraichissement : jetons + identite. */
public record AuthResult(
        String accessToken,
        String refreshToken,
        String username,
        String role
) {}
