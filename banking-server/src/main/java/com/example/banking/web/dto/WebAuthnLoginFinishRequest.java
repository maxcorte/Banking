package com.example.banking.web.dto;

/** credential = JSON (chaine) renvoye par navigator.credentials.get(). */
public record WebAuthnLoginFinishRequest(String flowId, String credential) {}
