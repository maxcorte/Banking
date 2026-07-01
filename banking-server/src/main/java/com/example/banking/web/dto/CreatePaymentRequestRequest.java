package com.example.banking.web.dto;

import java.util.UUID;

/** Creation d'une demande : compte a crediter, IBAN du destinataire, montant, motif. */
public record CreatePaymentRequestRequest(
        UUID toAccountId,
        String payerAccountNumber,
        long amountMinor,
        String description
) {}
