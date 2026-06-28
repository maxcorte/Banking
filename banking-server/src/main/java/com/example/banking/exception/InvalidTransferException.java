package com.example.banking.exception;

public class InvalidTransferException extends BankingException {
    public InvalidTransferException(String message) {
        super("INVALID_TRANSFER", message);
    }
}
