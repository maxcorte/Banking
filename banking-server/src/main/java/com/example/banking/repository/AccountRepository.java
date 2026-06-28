package com.example.banking.repository;

import com.example.banking.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    /** Comptes clients uniquement (exclut le compte "monde exterieur"). */
    java.util.List<Account> findByAllowNegativeBalanceFalseOrderByCreatedAtAsc();

    /** Comptes appartenant a un proprietaire donne. */
    java.util.List<Account> findByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    /**
     * Charge un compte en posant un verrou exclusif sur sa ligne
     * (SELECT ... FOR UPDATE). Deux virements simultanés touchant le même
     * compte seront ainsi sérialisés : c'est ce qui empêche le double-spend.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);
}
