package com.example.banking.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBeneficiaryRequest(
        @NotBlank String label,
        @NotBlank String accountNumber
) {}
