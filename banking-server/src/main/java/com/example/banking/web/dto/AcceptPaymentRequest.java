package com.example.banking.web.dto;

import java.util.UUID;

/** Acceptation d'une demande : compte a debiter. */
public record AcceptPaymentRequest(UUID fromAccountId) {}
