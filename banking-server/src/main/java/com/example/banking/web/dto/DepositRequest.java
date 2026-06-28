package com.example.banking.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositRequest(
        @NotNull @Positive Long amountMinor,
        String description
) {}
