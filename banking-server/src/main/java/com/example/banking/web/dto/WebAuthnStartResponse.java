package com.example.banking.web.dto;

/** flowId a renvoyer au finish + JSON d'options pour navigator.credentials. */
public record WebAuthnStartResponse(String flowId, String optionsJson) {}
