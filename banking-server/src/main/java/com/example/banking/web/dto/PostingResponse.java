package com.example.banking.web.dto;

import com.example.banking.domain.Posting;

import java.util.UUID;

public record PostingResponse(
        UUID id,
        UUID accountId,
        long amountMinor,
        String currency
) {
    public static PostingResponse from(Posting p) {
        return new PostingResponse(p.getId(), p.getAccountId(), p.getAmountMinor(), p.getCurrency());
    }
}
