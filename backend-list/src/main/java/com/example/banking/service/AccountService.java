package com.example.banking.service;

import com.example.banking.domain.Account;
import com.example.banking.domain.Posting;
import com.example.banking.exception.AccountNotFoundException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.PostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final PostingRepository postings;

    public AccountService(AccountRepository accounts, PostingRepository postings) {
        this.accounts = accounts;
        this.postings = postings;
    }

    @Transactional
    public Account create(String ownerName, String currency) {
        Account account = new Account(
                UUID.randomUUID(),
                generateAccountNumber(),
                ownerName,
                currency.toUpperCase());
        return accounts.save(account);
    }

    @Transactional(readOnly = true)
    public Account get(UUID id) {
        return accounts.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public List<Account> listCustomerAccounts() {
        return accounts.findByAllowNegativeBalanceFalseOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public List<Posting> history(UUID accountId) {
        if (!accounts.existsById(accountId)) {
            throw new AccountNotFoundException(accountId.toString());
        }
        return postings.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    /**
     * Vérifie que le solde en cache correspond bien à la somme des écritures
     * (la source de vérité). Outil de réconciliation / audit.
     */
    @Transactional(readOnly = true)
    public boolean isBalanceConsistent(UUID accountId) {
        Account account = get(accountId);
        return account.getBalanceMinor() == postings.computeBalanceMinor(accountId);
    }

    /** Génère un faux IBAN FR à des fins pédagogiques. */
    private String generateAccountNumber() {
        String candidate;
        do {
            StringBuilder sb = new StringBuilder("FR76");
            for (int i = 0; i < 23; i++) {
                sb.append(ThreadLocalRandom.current().nextInt(10));
            }
            candidate = sb.toString();
        } while (accounts.existsByAccountNumber(candidate));
        return candidate;
    }
}
