package com.example.banking.repository;

import com.example.banking.domain.PaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {

    List<PaymentRequest> findByPayerUserIdOrderByCreatedAtDesc(UUID payerUserId);

    List<PaymentRequest> findByRequesterUserIdOrderByCreatedAtDesc(UUID requesterUserId);
}
