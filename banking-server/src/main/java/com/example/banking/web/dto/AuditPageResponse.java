package com.example.banking.web.dto;

import java.util.List;

public record AuditPageResponse(
        List<AuditResponse> items,
        int page,
        int size,
        long total,
        boolean hasMore
) {}
