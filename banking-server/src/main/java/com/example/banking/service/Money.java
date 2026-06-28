package com.example.banking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Aide à la conversion entre montants décimaux (affichage) et unités
 * mineures entières (stockage). On ne stocke et ne calcule JAMAIS en
 * double/float : uniquement des entiers (long) ou BigDecimal.
 */
public final class Money {

    private Money() {
    }

    /** 12.34 EUR -> 1234 (centimes). */
    public static long toMinor(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    /** 1234 (centimes) -> 12.34. */
    public static BigDecimal toDecimal(long minor) {
        return BigDecimal.valueOf(minor).movePointLeft(2);
    }
}
