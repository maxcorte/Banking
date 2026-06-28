package com.example.banking.repository;

import com.example.banking.domain.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<BankTransaction, UUID> {

    Optional<BankTransaction> findByIdempotencyKey(String idempotencyKey);
}
