package com.example.banking.service;

/** Couple de jetons renvoye apres connexion ou rafraichissement. */
public record TokenPair(String accessToken, String refreshToken) {}
