package com.example.banking.web.dto;

import com.example.banking.domain.Account;
import com.example.banking.service.Money;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String ownerName,
        String currency,
        String status,
        long balanceMinor,
        BigDecimal balance
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getId(),
                a.getAccountNumber(),
                a.getOwnerName(),
                a.getCurrency(),
                a.getStatus().name(),
                a.getBalanceMinor(),
                Money.toDecimal(a.getBalanceMinor()));
    }
}
