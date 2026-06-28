package com.example.banking.repository;

import com.example.banking.domain.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    boolean existsByOwnerIdAndAccountNumber(UUID ownerId, String accountNumber);
}
