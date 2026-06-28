package com.example.banking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @NotBlank String ownerName,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "Devise ISO 4217 (ex. EUR)")
        String currency
) {}
