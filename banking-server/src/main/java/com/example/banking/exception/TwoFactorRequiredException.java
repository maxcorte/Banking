package com.example.banking.exception;

public class TwoFactorRequiredException extends BankingException {
    public TwoFactorRequiredException(String message) {
        super("TWO_FACTOR_REQUIRED", message);
    }
}
