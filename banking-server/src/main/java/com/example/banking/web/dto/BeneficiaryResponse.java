package com.example.banking.web.dto;

import com.example.banking.domain.Beneficiary;

import java.util.UUID;

public record BeneficiaryResponse(
        UUID id,
        String label,
        String accountNumber
) {
    public static BeneficiaryResponse from(Beneficiary b) {
        return new BeneficiaryResponse(b.getId(), b.getLabel(), b.getAccountNumber());
    }
}
