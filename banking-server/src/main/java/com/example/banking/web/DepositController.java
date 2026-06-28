package com.example.banking.web;

import com.example.banking.domain.BankTransaction;
import com.example.banking.domain.User;
import com.example.banking.security.AccessControl;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.AuditService;
import com.example.banking.service.DepositService;
import com.example.banking.web.dto.DepositRequest;
import com.example.banking.web.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/{id}/deposit")
public class DepositController {

    private final DepositService depositService;
    private final CurrentUser currentUser;
    private final AccessControl accessControl;
    private final AuditService auditService;

    public DepositController(DepositService depositService,
                            CurrentUser currentUser,
                            AccessControl accessControl,
                            AuditService auditService) {
        this.depositService = depositService;
        this.currentUser = currentUser;
        this.accessControl = accessControl;
        this.auditService = auditService;
    }

    /** Alimente un compte. Reserve aux ADMIN (la banque injecte les fonds). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {

        User me = currentUser.require();
        accessControl.assertAdmin(me);

        BankTransaction tx = depositService.deposit(
                id, request.amountMinor(), idempotencyKey, request.description());
        auditService.record("DEPOSIT", euros(request.amountMinor()) + " sur le compte " + id);
        return TransactionResponse.from(tx);
    }

    private static String euros(long minor) {
        return String.format(java.util.Locale.FRANCE, "%.2f \u20ac", minor / 100.0);
    }
}
