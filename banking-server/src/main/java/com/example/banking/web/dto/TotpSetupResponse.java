package com.example.banking.web.dto;

/** Reponse d'enrolement 2FA : secret (saisie manuelle) + URI otpauth (QR). */
public record TotpSetupResponse(String secret, String otpauthUri) {}
