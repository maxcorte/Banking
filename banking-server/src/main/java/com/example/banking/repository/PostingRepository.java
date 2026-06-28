package com.example.banking.repository;

import com.example.banking.domain.Posting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    List<Posting> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<Posting> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    /**
     * Solde réel recalculé depuis les écritures (la source de vérité).
     * Sert à vérifier que le solde mis en cache n'a pas dérivé (réconciliation).
     */
    @Query("SELECT COALESCE(SUM(p.amountMinor), 0) FROM Posting p WHERE p.accountId = :accountId")
    long computeBalanceMinor(@Param("accountId") UUID accountId);
}
