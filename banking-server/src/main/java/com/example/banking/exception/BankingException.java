package com.example.banking.exception;

/** Exception métier de base. Le code sert à mapper vers un statut HTTP. */
public class BankingException extends RuntimeException {

    private final String code;

    public BankingException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
