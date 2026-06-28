package com.example.banking.web;

import com.example.banking.domain.Account;
import com.example.banking.service.AccountService;
import com.example.banking.web.dto.AccountResponse;
import com.example.banking.web.dto.CreateAccountRequest;
import com.example.banking.web.dto.PostingResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(request.ownerName(), request.currency());
        return ResponseEntity
                .created(URI.create("/api/accounts/" + account.getId()))
                .body(AccountResponse.from(account));
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.listCustomerAccounts().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(accountService.get(id));
    }

    @GetMapping("/{id}/history")
    public List<PostingResponse> history(@PathVariable UUID id) {
        return accountService.history(id).stream().map(PostingResponse::from).toList();
    }
}
