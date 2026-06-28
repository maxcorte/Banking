package com.example.banking.service;

import com.example.banking.domain.Account;
import com.example.banking.domain.BankTransaction;
import com.example.banking.domain.Posting;
import com.example.banking.exception.AccountNotFoundException;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.InvalidTransferException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public TransferService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    /**
     * Effectue un virement de fromId vers toId. Toute la méthode est une
     * seule transaction SQL (ACID) : soit tout réussit, soit rien.
     */
    @Transactional
    public BankTransaction transfer(UUID fromId,
                                    UUID toId,
                                    long amountMinor,
                                    String idempotencyKey,
                                    String description) {

        // 1. Idempotence : si cette requête a déjà été traitée, on renvoie
        //    la transaction existante sans rien rejouer.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = transactions.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // 2. Validations de base
        if (amountMinor <= 0) {
            throw new InvalidTransferException("Le montant doit être strictement positif.");
        }
        if (fromId.equals(toId)) {
            throw new InvalidTransferException("Les comptes source et destination sont identiques.");
        }

        // 3. Verrouillage pessimiste des DEUX comptes, toujours dans le même
        //    ordre (tri par id) pour éviter les interblocages (deadlocks)
        //    entre deux virements croisés A->B et B->A simultanés.
        List<UUID> ordered = List.of(fromId, toId).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        accounts.findByIdForUpdate(ordered.get(0))
                .orElseThrow(() -> new AccountNotFoundException(ordered.get(0).toString()));
        accounts.findByIdForUpdate(ordered.get(1))
                .orElseThrow(() -> new AccountNotFoundException(ordered.get(1).toString()));

        // Rechargés (managés, verrouillés) dans le sens métier
        Account from = accounts.findById(fromId)
                .orElseThrow(() -> new AccountNotFoundException(fromId.toString()));
        Account to = accounts.findById(toId)
                .orElseThrow(() -> new AccountNotFoundException(toId.toString()));

        // 4. Règles métier
        if (!from.isActive() || !to.isActive()) {
            throw new InvalidTransferException("Un des comptes n'est pas actif.");
        }
        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new InvalidTransferException(
                    "Devises différentes (" + from.getCurrency() + " / " + to.getCurrency() + ").");
        }
        if (from.getBalanceMinor() < amountMinor) {
            throw new InsufficientFundsException();
        }

        // 5. Écritures en partie double : -montant côté source, +montant côté
        //    destination. Leur somme vaut 0 (vérifié aussi par la base).
        String currency = from.getCurrency();
        BankTransaction tx = new BankTransaction(
                UUID.randomUUID(),
                "TRX-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(),
                idempotencyKey,
                description);
        tx.addPosting(new Posting(UUID.randomUUID(), from.getId(), -amountMinor, currency));
        tx.addPosting(new Posting(UUID.randomUUID(), to.getId(), amountMinor, currency));

        // 6. Mise à jour des soldes en cache (sous verrou)
        from.debit(amountMinor);
        to.credit(amountMinor);

        // 7. Persistance (le solde des comptes managés est écrit au commit)
        return transactions.save(tx);
    }
}
