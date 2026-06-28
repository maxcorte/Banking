package com.example.banking.exception;

public class ForbiddenException extends BankingException {
    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
