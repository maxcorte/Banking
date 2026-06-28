package com.example.banking.web.dto;

import com.example.banking.domain.AuditEntry;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditResponse(
        UUID id,
        OffsetDateTime at,
        String actor,
        String action,
        String detail
) {
    public static AuditResponse from(AuditEntry e) {
        return new AuditResponse(e.getId(), e.getAt(), e.getActor(), e.getAction(), e.getDetail());
    }
}
