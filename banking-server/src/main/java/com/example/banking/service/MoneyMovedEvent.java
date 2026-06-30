package com.example.banking.service;

import java.util.UUID;

/** Evenement emis apres un mouvement d'argent reussi (transfert ou depot). */
public record MoneyMovedEvent(
        UUID fromAccountId,
        UUID toAccountId,
        long amountMinor,
        String currency,
        String description
) {}
