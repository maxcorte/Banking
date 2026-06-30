package com.example.banking.web;

import com.example.banking.domain.AuditEntry;
import com.example.banking.domain.User;
import com.example.banking.security.AccessControl;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.AuditService;
import com.example.banking.web.dto.AuditPageResponse;
import com.example.banking.web.dto.AuditResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consultation du journal d'audit. Reserve aux ADMIN. */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final AccessControl accessControl;

    public AuditController(AuditService auditService,
                           CurrentUser currentUser,
                           AccessControl accessControl) {
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.accessControl = accessControl;
    }

    @GetMapping
    public AuditPageResponse list(@RequestParam(required = false) String q,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "25") int size) {
        User me = currentUser.require();
        accessControl.assertAdmin(me);
        Page<AuditEntry> result = auditService.page(q, page, size);
        List<AuditResponse> items = result.getContent().stream().map(AuditResponse::from).toList();
        return new AuditPageResponse(
                items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.hasNext());
    }
}
