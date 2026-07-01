package com.example.banking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record TransferRequest(
        @NotNull UUID fromAccountId,
        @NotBlank String toAccountNumber,
        @NotNull @Positive Long amountMinor,
        String description,
        String category,
        String totpCode
) {}
