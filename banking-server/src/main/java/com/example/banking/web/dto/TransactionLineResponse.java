package com.example.banking.web.dto;

import com.example.banking.service.TransactionLine;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionLineResponse(
        UUID id,
        OffsetDateTime at,
        long amountMinor,
        String currency,
        String description,
        String kind,
        String counterpartyName,
        String counterpartyNumber,
        String category,
        long balanceAfterMinor
) {
    public static TransactionLineResponse from(TransactionLine l) {
        return new TransactionLineResponse(
                l.id(), l.at(), l.amountMinor(), l.currency(), l.description(),
                l.kind(), l.counterpartyName(), l.counterpartyNumber(),
                l.category(), l.balanceAfterMinor());
    }
}
