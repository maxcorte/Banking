package com.example.banking.web;

import com.example.banking.domain.User;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.AuditService;
import com.example.banking.service.BeneficiaryService;
import com.example.banking.web.dto.BeneficiaryResponse;
import com.example.banking.web.dto.CreateBeneficiaryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public BeneficiaryController(BeneficiaryService beneficiaryService,
                                 CurrentUser currentUser,
                                 AuditService auditService) {
        this.beneficiaryService = beneficiaryService;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    public List<BeneficiaryResponse> list() {
        User me = currentUser.require();
        return beneficiaryService.list(me.getId()).stream()
                .map(BeneficiaryResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse add(@Valid @RequestBody CreateBeneficiaryRequest request) {
        User me = currentUser.require();
        BeneficiaryResponse created = BeneficiaryResponse.from(
                beneficiaryService.add(me.getId(), request.label(), request.accountNumber()));
        auditService.record("BENEFICIARY_ADDED", created.label() + " (" + created.accountNumber() + ")");
        return created;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id) {
        User me = currentUser.require();
        beneficiaryService.remove(me.getId(), id);
        auditService.record("BENEFICIARY_REMOVED", "Beneficiaire " + id);
    }
}
