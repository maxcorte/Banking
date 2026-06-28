package com.example.banking.service;

import com.example.banking.domain.BankTransaction;
import com.example.banking.domain.TransactionCategory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Gere les depots. Un depot est modelise comme un virement depuis le compte
 * "monde exterieur" (autorise a passer en negatif) vers le compte client.
 * La partie double et toutes les protections du virement s'appliquent donc
 * automatiquement (idempotence, verrou, equilibre des ecritures).
 */
@Service
public class DepositService {

    /** Identifiant fixe du compte "monde exterieur" (cf. migration V2). */
    public static final UUID EXTERNAL_ACCOUNT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TransferService transferService;

    public DepositService(TransferService transferService) {
        this.transferService = transferService;
    }

    public BankTransaction deposit(UUID targetAccountId,
                                   long amountMinor,
                                   String idempotencyKey,
                                   String description) {
        String desc = (description != null && !description.isBlank()) ? description : "Depot";
        return transferService.transfer(
                EXTERNAL_ACCOUNT_ID, targetAccountId, amountMinor, idempotencyKey, desc,
                TransactionCategory.AUTRES);
    }
}
