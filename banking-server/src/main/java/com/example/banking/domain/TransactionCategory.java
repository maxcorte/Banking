package com.example.banking.domain;

/** Categories predefinies pour classer les virements. */
public enum TransactionCategory {
    AUTRES,
    COURSES,
    LOYER,
    SALAIRE,
    FACTURES,
    TRANSPORT,
    LOISIRS,
    RESTAURANT,
    SANTE,
    EPARGNE,
    CADEAU;

    /** Convertit un libelle recu du client en categorie, AUTRES par defaut. */
    public static TransactionCategory fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return AUTRES;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTRES;
        }
    }
}
