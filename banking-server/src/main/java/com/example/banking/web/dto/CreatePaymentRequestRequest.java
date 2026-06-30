package com.example.banking.web.dto;

import java.util.UUID;

/** Creation d'une demande : compte a crediter, destinataire, montant, motif. */
public record CreatePaymentRequestRequest(
        UUID toAccountId,
        String payerUsername,
        long amountMinor,
        String description
) {}
