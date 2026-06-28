package com.example.banking.web;

import com.example.banking.domain.Account;
import com.example.banking.domain.Role;
import com.example.banking.domain.User;
import com.example.banking.security.AccessControl;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.AccountService;
import com.example.banking.service.AuditService;
import com.example.banking.web.dto.AccountResponse;
import com.example.banking.web.dto.CreateAccountRequest;
import com.example.banking.web.dto.TransactionLineResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUser currentUser;
    private final AccessControl accessControl;
    private final AuditService auditService;

    public AccountController(AccountService accountService,
                            CurrentUser currentUser,
                            AccessControl accessControl,
                            AuditService auditService) {
        this.accountService = accountService;
        this.currentUser = currentUser;
        this.accessControl = accessControl;
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        User me = currentUser.require();
        Account account = accountService.create(request.ownerName(), request.currency(), me.getId());
        auditService.record("ACCOUNT_CREATED", "Compte " + account.getAccountNumber() + " (" + account.getOwnerName() + ")");
        return ResponseEntity
                .created(URI.create("/api/accounts/" + account.getId()))
                .body(AccountResponse.from(account));
    }

    /** Un ADMIN voit tous les comptes clients ; un USER ne voit que les siens. */
    @GetMapping
    public List<AccountResponse> list() {
        User me = currentUser.require();
        List<Account> accounts = (me.getRole() == Role.ADMIN)
                ? accountService.listCustomerAccounts()
                : accountService.listByOwner(me.getId());
        return accounts.stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        User me = currentUser.require();
        Account account = accountService.get(id);
        accessControl.assertOwnsOrAdmin(account, me);
        return AccountResponse.from(account);
    }

    @GetMapping("/{id}/history")
    public List<TransactionLineResponse> history(@PathVariable UUID id) {
        User me = currentUser.require();
        Account account = accountService.get(id);
        accessControl.assertOwnsOrAdmin(account, me);
        return accountService.historyLines(id).stream().map(TransactionLineResponse::from).toList();
    }

    /** Cloture (suppression "metier") un compte : owner ou ADMIN, solde a zero. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable UUID id) {
        User me = currentUser.require();
        Account account = accountService.get(id);
        accessControl.assertOwnsOrAdmin(account, me);
        accountService.close(id);
        auditService.record("ACCOUNT_CLOSED", "Compte " + account.getAccountNumber());
    }
}
