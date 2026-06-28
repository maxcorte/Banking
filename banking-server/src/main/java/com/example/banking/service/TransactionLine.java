package com.example.banking.service;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Une ligne d'historique enrichie : mouvement + contrepartie + solde courant. */
public record TransactionLine(
        UUID id,
        OffsetDateTime at,
        long amountMinor,
        String currency,
        String description,
        String kind,                 // "DEPOSIT" ou "TRANSFER"
        String counterpartyName,     // nom de la contrepartie (ou null)
        String counterpartyNumber,   // IBAN de la contrepartie (ou null)
        String category,             // categorie de la transaction (ou null)
        long balanceAfterMinor
) {}
