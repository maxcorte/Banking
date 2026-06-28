package com.example.banking.web;

import com.example.banking.domain.Account;
import com.example.banking.domain.BankTransaction;
import com.example.banking.domain.TransactionCategory;
import com.example.banking.domain.User;
import com.example.banking.exception.InvalidTransferException;
import com.example.banking.security.AccessControl;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.AccountService;
import com.example.banking.service.AuditService;
import com.example.banking.service.TransferService;
import com.example.banking.web.dto.TransactionResponse;
import com.example.banking.web.dto.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final AccountService accountService;
    private final CurrentUser currentUser;
    private final AccessControl accessControl;
    private final AuditService auditService;

    public TransferController(TransferService transferService,
                             AccountService accountService,
                             CurrentUser currentUser,
                             AccessControl accessControl,
                             AuditService auditService) {
        this.transferService = transferService;
        this.accountService = accountService;
        this.currentUser = currentUser;
        this.accessControl = accessControl;
        this.auditService = auditService;
    }

    /**
     * Virement. Le compte SOURCE (par id) doit appartenir a l'utilisateur (ou ADMIN).
     * La DESTINATION est designee par son numero de compte (IBAN), comme dans une
     * vraie banque : on peut virer vers n'importe quel compte client existant.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        User me = currentUser.require();
        Account source = accountService.get(request.fromAccountId());
        accessControl.assertOwnsOrAdmin(source, me);

        Account destination = accountService.getByNumber(request.toAccountNumber().trim());
        // On interdit de virer vers le compte systeme "monde exterieur".
        if (destination.allowsNegativeBalance()) {
            throw new InvalidTransferException("Compte destinataire invalide.");
        }

        BankTransaction tx = transferService.transfer(
                source.getId(),
                destination.getId(),
                request.amountMinor(),
                idempotencyKey,
                request.description(),
                TransactionCategory.fromNullable(request.category()));

        auditService.record("TRANSFER",
                euros(request.amountMinor()) + " de " + source.getAccountNumber()
                        + " vers " + destination.getAccountNumber());

        return TransactionResponse.from(tx);
    }

    private static String euros(long minor) {
        return String.format(java.util.Locale.FRANCE, "%.2f \u20ac", minor / 100.0);
    }
}
