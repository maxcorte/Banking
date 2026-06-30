package com.example.banking.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentRequestResponse(
        UUID id,
        String requesterName,
        String payerName,
        String toAccountNumber,
        long amountMinor,
        String currency,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt
) {}
