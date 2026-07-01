package com.example.banking.repository;

import com.example.banking.domain.UserTotp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserTotpRepository extends JpaRepository<UserTotp, UUID> {
}
