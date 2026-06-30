package com.example.banking.service;

import java.util.UUID;

/** Demande de notification (in-app + push) pour un utilisateur donne. */
public record UserNotificationEvent(UUID userId, String type, String title, String body) {}
