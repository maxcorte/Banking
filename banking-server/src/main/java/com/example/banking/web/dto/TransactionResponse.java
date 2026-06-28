package com.example.banking.web.dto;

import com.example.banking.domain.BankTransaction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String reference,
        String description,
        OffsetDateTime createdAt,
        List<PostingResponse> postings
) {
    public static TransactionResponse from(BankTransaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getReference(),
                tx.getDescription(),
                tx.getCreatedAt(),
                tx.getPostings().stream().map(PostingResponse::from).toList());
    }
}
