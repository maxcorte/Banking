package com.example.banking;

import com.example.banking.domain.Account;
import com.example.banking.domain.Role;
import com.example.banking.domain.User;
import com.example.banking.domain.TransactionCategory;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.repository.UserRepository;
import com.example.banking.service.AccountService;
import com.example.banking.service.DepositService;
import com.example.banking.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de concurrence : c'est ici qu'on prouve que le verrou pessimiste
 * (SELECT ... FOR UPDATE) empêche de dépenser deux fois le même argent
 * lorsque plusieurs virements partent du même compte en même temps.
 */
class ConcurrentTransferTest extends AbstractPostgresIntegrationTest {

    @Autowired TransferService transferService;
    @Autowired DepositService depositService;
    @Autowired AccountService accountService;
    @Autowired UserRepository users;

    /** Crée un utilisateur réel puis un de ses comptes (la table accounts
     *  a une clé étrangère vers users : l'owner doit exister). */
    private Account newCustomer() {
        UUID ownerId = UUID.randomUUID();
        users.save(new User(ownerId, "user-" + ownerId, "x", Role.USER));
        return accountService.create("Test " + ownerId, "EUR", ownerId);
    }

    @Test
    @DisplayName("Deux virements simultanés vidant le compte : un seul réussit")
    void concurrentFullTransfers_onlyOneSucceeds() throws Exception {
        Account a = newCustomer();
        Account b = newCustomer();
        depositService.deposit(a.getId(), 100_00, UUID.randomUUID().toString(), "Dépôt");

        int successes = runConcurrentTransfers(a, b, /*threads*/ 2, /*amount*/ 100_00);

        assertThat(successes).isEqualTo(1);
        assertThat(accountService.get(a.getId()).getBalanceMinor()).isZero();
        assertThat(accountService.get(b.getId()).getBalanceMinor()).isEqualTo(100_00);
        assertThat(accountService.isBalanceConsistent(a.getId())).isTrue();
        assertThat(accountService.isBalanceConsistent(b.getId())).isTrue();
    }

    @Test
    @DisplayName("20 virements simultanés sur un solde qui n'en couvre que 10 : exactement 10 passent")
    void concurrentPartialTransfers_exactlyTenSucceed() throws Exception {
        Account a = newCustomer();
        Account b = newCustomer();
        depositService.deposit(a.getId(), 100_00, UUID.randomUUID().toString(), "Dépôt");

        // 20 virements de 10,00 € ; le compte ne peut en financer que 10.
        int successes = runConcurrentTransfers(a, b, /*threads*/ 20, /*amount*/ 10_00);

        assertThat(successes).isEqualTo(10);
        assertThat(accountService.get(a.getId()).getBalanceMinor()).isZero();
        assertThat(accountService.get(b.getId()).getBalanceMinor()).isEqualTo(100_00);
        assertThat(accountService.isBalanceConsistent(a.getId())).isTrue();
    }

    /**
     * Lance {@code threads} virements de {@code amount} de a vers b, démarrés
     * tous en même temps, et renvoie le nombre de réussites (les autres lèvent
     * InsufficientFundsException).
     */
    private int runConcurrentTransfers(Account a, Account b, int threads, long amount)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();                 // tous les threads partent ensemble
                try {
                    transferService.transfer(a.getId(), b.getId(), amount,
                            UUID.randomUUID().toString(), "Concurrent",
                            TransactionCategory.AUTRES);
                    return Boolean.TRUE;
                } catch (InsufficientFundsException expected) {
                    return Boolean.FALSE;   // perdant légitime
                }
            }));
        }

        ready.await();                      // attend que tous soient prêts
        go.countDown();                     // top départ simultané

        int successes = 0;
        for (Future<Boolean> f : futures) {
            if (Boolean.TRUE.equals(f.get())) {
                successes++;
            }
        }
        pool.shutdown();
        return successes;
    }
}
