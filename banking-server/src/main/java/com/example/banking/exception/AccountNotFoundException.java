package com.example.banking.exception;

public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String accountRef) {
        super("ACCOUNT_NOT_FOUND", "Compte introuvable : " + accountRef);
    }
}
