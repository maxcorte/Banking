package com.example.banking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, HttpStatus> STATUS_BY_CODE = Map.ofEntries(
            Map.entry("ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("INSUFFICIENT_FUNDS", HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry("INVALID_TRANSFER", HttpStatus.BAD_REQUEST),
            Map.entry("USERNAME_TAKEN", HttpStatus.CONFLICT),
            Map.entry("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED),
            Map.entry("FORBIDDEN", HttpStatus.FORBIDDEN),
            Map.entry("ACCOUNT_NOT_EMPTY", HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry("BENEFICIARY_EXISTS", HttpStatus.CONFLICT),
            Map.entry("BENEFICIARY_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("TOO_MANY_ATTEMPTS", HttpStatus.TOO_MANY_REQUESTS),
            Map.entry("INVALID_REFRESH", HttpStatus.UNAUTHORIZED)
    );

    @ExceptionHandler(BankingException.class)
    public ProblemDetail handleBanking(BankingException ex) {
        HttpStatus status = STATUS_BY_CODE.getOrDefault(ex.getCode(), HttpStatus.BAD_REQUEST);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setProperty("code", ex.getCode());
        return problem;
    }
}
