package com.example.banking.exception;

public class InsufficientFundsException extends BankingException {
    public InsufficientFundsException() {
        super("INSUFFICIENT_FUNDS", "Solde insuffisant sur le compte source.");
    }
}
