package com.example.banking.web.dto;

/** credential = JSON (chaine) renvoye par navigator.credentials.create(). */
public record WebAuthnRegisterFinishRequest(String flowId, String credential, String label) {}
