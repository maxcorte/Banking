package com.example.banking.service;

import com.example.banking.domain.Beneficiary;
import com.example.banking.exception.AccountNotFoundException;
import com.example.banking.exception.BankingException;
import com.example.banking.exception.ForbiddenException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.BeneficiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaries;
    private final AccountRepository accounts;

    public BeneficiaryService(BeneficiaryRepository beneficiaries, AccountRepository accounts) {
        this.beneficiaries = beneficiaries;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<Beneficiary> list(UUID ownerId) {
        return beneficiaries.findByOwnerIdOrderByCreatedAtAsc(ownerId);
    }

    @Transactional
    public Beneficiary add(UUID ownerId, String label, String accountNumber) {
        String iban = accountNumber.trim();
        // On verifie que le compte existe vraiment avant de l'enregistrer.
        if (accounts.findByAccountNumber(iban).isEmpty()) {
            throw new AccountNotFoundException(iban);
        }
        if (beneficiaries.existsByOwnerIdAndAccountNumber(ownerId, iban)) {
            throw new BankingException("BENEFICIARY_EXISTS", "Ce bénéficiaire est déjà enregistré.");
        }
        Beneficiary beneficiary = new Beneficiary(UUID.randomUUID(), ownerId, label.trim(), iban);
        return beneficiaries.save(beneficiary);
    }

    @Transactional
    public void remove(UUID ownerId, UUID beneficiaryId) {
        Beneficiary beneficiary = beneficiaries.findById(beneficiaryId)
                .orElseThrow(() -> new BankingException("BENEFICIARY_NOT_FOUND", "Bénéficiaire introuvable."));
        if (!beneficiary.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Ce bénéficiaire ne vous appartient pas.");
        }
        beneficiaries.delete(beneficiary);
    }
}
