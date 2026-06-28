package com.example.banking;

import com.example.banking.domain.Account;
import com.example.banking.domain.Role;
import com.example.banking.domain.User;
import com.example.banking.domain.BankTransaction;
import com.example.banking.domain.Posting;
import com.example.banking.domain.TransactionCategory;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.InvalidTransferException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.service.AccountService;
import com.example.banking.service.DepositService;
import com.example.banking.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests des invariants comptables du grand livre. Tout est exprimé en unités
 * mineures (centimes) : 100_00 = 100,00 €.
 */
class LedgerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired TransferService transferService;
    @Autowired DepositService depositService;
    @Autowired AccountService accountService;
    @Autowired UserRepository users;
    @Autowired AccountRepository accountRepository;

    /** Crée un utilisateur réel puis un de ses comptes (la table accounts
     *  a une clé étrangère vers users : l'owner doit exister). */
    private Account newCustomer() {
        UUID ownerId = UUID.randomUUID();
        users.save(new User(ownerId, "user-" + ownerId, "x", Role.USER));
        return accountService.create("Test " + ownerId, "EUR", ownerId);
    }

    @Test
    @DisplayName("Un virement déplace exactement le montant et conserve l'argent")
    void transfer_movesExactAmount() {
        Account a = newCustomer();
        Account b = newCustomer();
        depositService.deposit(a.getId(), 100_00, UUID.randomUUID().toString(), "Dépôt initial");

        transferService.transfer(a.getId(), b.getId(), 30_00,
                UUID.randomUUID().toString(), "Loyer", TransactionCategory.LOYER);

        assertThat(accountService.get(a.getId()).getBalanceMinor()).isEqualTo(70_00);
        assertThat(accountService.get(b.getId()).getBalanceMinor()).isEqualTo(30_00);
        // Le solde en cache est cohérent avec la somme des écritures.
        assertThat(accountService.isBalanceConsistent(a.getId())).isTrue();
        assertThat(accountService.isBalanceConsistent(b.getId())).isTrue();
    }

    @Test
    @DisplayName("Un virement est en partie double : deux écritures opposées (somme nulle)")
    void transfer_isDoubleEntry() {
        Account a = newCustomer();
        Account b = newCustomer();
        depositService.deposit(a.getId(), 50_00, UUID.randomUUID().toString(), "Dépôt");

        BankTransaction tx = transferService.transfer(a.getId(), b.getId(), 20_00,
                UUID.randomUUID().toString(), "Cadeau", TransactionCategory.CADEAU);

        assertThat(tx.getPostings()).hasSize(2);
        long sum = tx.getPostings().stream().mapToLong(Posting::getAmountMinor).sum();
        assertThat(sum).isZero();
        assertThat(tx.getPostings()).anySatisfy(p -> {
            assertThat(p.getAccountId()).isEqualTo(a.getId());
            assertThat(p.getAmountMinor()).isEqualTo(-20_00);
        });
        assertThat(tx.getPostings()).anySatisfy(p -> {
            assertThat(p.getAccountId()).isEqualTo(b.getId());
            assertThat(p.getAmountMinor()).isEqualTo(20_00);
        });
    }

    @Test
    @DisplayName("Un virement supérieur au solde est refusé, sans déplacer d'argent")
    void insufficientFunds_isRejected() {
        Account a = newCustomer();
        Account b = newCustomer();
        depositService.deposit(a.getId(), 50_00, UUID.randomUUID().toString(), "Dépôt");

        assertThatThrownBy(() -> transferService.transfer(a.getId(), b.getId(), 80_00,
                UUID.randomUUID().toString(), "Trop", TransactionCategory.AUTRES))
                .isInstanceOf(InsufficientFundsException.class);

        // Rien n'a bougé.
        assertThat(accountService.get(a.getId()).getBalanceMinor()).isEqualTo(50_00);
        assertThat(accountService.get(b.getId()).getBalanceMinor()).isZero();
    }

    @Test
    @DisplayName("La même clé d'idempotence ne rejoue pas le virement")
    void idempotency_doesNotDoubleProcess() {
        Account a = newCustomer();
        Account b = newCustomer();
        depositService.deposit(a.getId(), 100_00, UUID.randomUUID().toString(), "Dépôt");

        String key = UUID.randomUUID().toString();
        BankTransaction first = transferService.transfer(a.getId(), b.getId(), 20_00,
                key, "Paiement", TransactionCategory.AUTRES);
        // Rejouée avec la même clé : doit renvoyer la transaction existante.
        BankTransaction replay = transferService.transfer(a.getId(), b.getId(), 20_00,
                key, "Paiement", TransactionCategory.AUTRES);

        assertThat(replay.getId()).isEqualTo(first.getId());
        // Le solde n'a été débité qu'une seule fois.
        assertThat(accountService.get(a.getId()).getBalanceMinor()).isEqualTo(80_00);
        assertThat(accountService.get(b.getId()).getBalanceMinor()).isEqualTo(20_00);
    }

    @Test
    @DisplayName("Un virement vers le même compte est refusé")
    void transferToSameAccount_isRejected() {
        Account a = newCustomer();
        depositService.deposit(a.getId(), 10_00, UUID.randomUUID().toString(), "Dépôt");

        assertThatThrownBy(() -> transferService.transfer(a.getId(), a.getId(), 5_00,
                UUID.randomUUID().toString(), "Boucle", TransactionCategory.AUTRES))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    @DisplayName("Un montant nul ou négatif est refusé")
    void nonPositiveAmount_isRejected() {
        Account a = newCustomer();
        Account b = newCustomer();
        depositService.deposit(a.getId(), 10_00, UUID.randomUUID().toString(), "Dépôt");

        assertThatThrownBy(() -> transferService.transfer(a.getId(), b.getId(), 0,
                UUID.randomUUID().toString(), "Zéro", TransactionCategory.AUTRES))
                .isInstanceOf(InvalidTransferException.class);
        assertThatThrownBy(() -> transferService.transfer(a.getId(), b.getId(), -5_00,
                UUID.randomUUID().toString(), "Négatif", TransactionCategory.AUTRES))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    @DisplayName("Le grand livre entier somme toujours à zéro (partie double)")
    void wholeLedger_sumsToZero() {
        Account a = newCustomer();
        Account b = newCustomer();
        Account c = newCustomer();
        depositService.deposit(a.getId(), 200_00, UUID.randomUUID().toString(), "Dépôt");
        transferService.transfer(a.getId(), b.getId(), 75_00,
                UUID.randomUUID().toString(), "T1", TransactionCategory.AUTRES);
        transferService.transfer(b.getId(), c.getId(), 25_00,
                UUID.randomUUID().toString(), "T2", TransactionCategory.AUTRES);

        // La somme des soldes de TOUS les comptes (y compris le compte
        // "monde extérieur", dont le solde est négatif) vaut exactement 0.
        long total = accountRepository.findAll().stream()
                .mapToLong(Account::getBalanceMinor)
                .sum();
        assertThat(total).isZero();
    }
}
