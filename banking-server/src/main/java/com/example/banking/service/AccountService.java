package com.example.banking.service;

import com.example.banking.domain.Account;
import com.example.banking.domain.AccountStatus;
import com.example.banking.domain.Posting;
import com.example.banking.exception.AccountNotFoundException;
import com.example.banking.exception.BankingException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.PostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    public Account create(String ownerName, String currency, UUID ownerId) {
        Account account = new Account(
                UUID.randomUUID(),
                generateAccountNumber(),
                ownerName,
                currency.toUpperCase());
        account.setOwnerId(ownerId);
        return accounts.save(account);
    }

    @Transactional(readOnly = true)
    public Account get(UUID id) {
        return accounts.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public Account getByNumber(String accountNumber) {
        return accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    @Transactional(readOnly = true)
    public List<Account> listCustomerAccounts() {
        return accounts.findByAllowNegativeBalanceFalseOrderByCreatedAtAsc().stream()
                .filter(a -> a.getStatus() != AccountStatus.CLOSED)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Account> listByOwner(UUID ownerId) {
        return accounts.findByOwnerIdOrderByCreatedAtAsc(ownerId).stream()
                .filter(a -> a.getStatus() != AccountStatus.CLOSED)
                .toList();
    }

    /**
     * Cloture un compte (suppression "metier"). On ne detruit jamais un compte
     * avec un historique : on le passe en CLOSED. Le solde doit etre nul.
     */
    @Transactional
    public void close(UUID id) {
        Account account = get(id);
        if (account.getBalanceMinor() != 0) {
            throw new BankingException("ACCOUNT_NOT_EMPTY",
                    "Le compte doit être vidé (solde à zéro) avant d'être clôturé.");
        }
        account.setStatus(AccountStatus.CLOSED);
        accounts.save(account);
    }

    @Transactional(readOnly = true)
    public List<Posting> history(UUID accountId) {
        if (!accounts.existsById(accountId)) {
            throw new AccountNotFoundException(accountId.toString());
        }
        return postings.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    /**
     * Historique enrichi : pour chaque ecriture du compte, on retrouve la
     * contrepartie (l'autre compte de la meme transaction = provenance ou
     * destination) et on calcule le solde courant apres chaque mouvement.
     * Renvoye du plus recent au plus ancien.
     */
    @Transactional(readOnly = true)
    public List<TransactionLine> historyLines(UUID accountId) {
        if (!accounts.existsById(accountId)) {
            throw new AccountNotFoundException(accountId.toString());
        }
        // Ordre chronologique pour cumuler le solde correctement.
        List<Posting> chronological = postings.findByAccountIdOrderByCreatedAtAsc(accountId);

        // Pré-charge les comptes contreparties en une seule requête.
        Set<UUID> counterpartyIds = new HashSet<>();
        for (Posting p : chronological) {
            counterpartyOf(p, accountId).ifPresent(cp -> counterpartyIds.add(cp.getAccountId()));
        }
        Map<UUID, Account> byId = new HashMap<>();
        for (Account a : accounts.findAllById(counterpartyIds)) {
            byId.put(a.getId(), a);
        }

        List<TransactionLine> lines = new ArrayList<>();
        long running = 0L;
        for (Posting p : chronological) {
            running += p.getAmountMinor();

            Optional<Posting> sibling = counterpartyOf(p, accountId);
            boolean isDeposit = sibling
                    .map(s -> DepositService.EXTERNAL_ACCOUNT_ID.equals(s.getAccountId()))
                    .orElse(false);

            String cpName;
            String cpNumber;
            if (isDeposit) {
                cpName = "Dépôt (banque)";
                cpNumber = null;
            } else {
                Account cp = sibling.map(s -> byId.get(s.getAccountId())).orElse(null);
                cpName = cp != null ? cp.getOwnerName() : null;
                cpNumber = cp != null ? cp.getAccountNumber() : null;
            }

            var category = p.getTransaction().getCategory();
            lines.add(new TransactionLine(
                    p.getId(),
                    p.getCreatedAt(),
                    p.getAmountMinor(),
                    p.getCurrency(),
                    p.getTransaction().getDescription(),
                    isDeposit ? "DEPOSIT" : "TRANSFER",
                    cpName,
                    cpNumber,
                    category != null ? category.name() : null,
                    running));
        }

        // Plus recent en premier pour l'affichage.
        Collections.reverse(lines);
        return lines;
    }

    /** L'ecriture soeur (autre compte) de la transaction, s'il y en a une. */
    private Optional<Posting> counterpartyOf(Posting posting, UUID accountId) {
        return posting.getTransaction().getPostings().stream()
                .filter(other -> !other.getAccountId().equals(accountId))
                .findFirst();
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
