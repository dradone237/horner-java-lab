package org.hornerlab.core;

import java.math.BigDecimal;

/**
 * Étape du schéma de Horner étendu (P et P' simultanément).
 * Pour l'initialisation (n°1), {@code valueBefore} et {@code derivativeBefore} valent {@code null}.
 *
 * @param number           numéro d'affichage (1 = initialisation)
 * @param coefficient      coefficient traité
 * @param valueBefore      b avant l'étape
 * @param derivativeBefore d avant l'étape
 * @param derivativeAfter  d après l'étape : d × x + b(avant)
 * @param valueAfter       b après l'étape : b(avant) × x + coefficient
 */
public record DerivativeStep(int number, BigDecimal coefficient, BigDecimal valueBefore,
                             BigDecimal derivativeBefore, BigDecimal derivativeAfter, BigDecimal valueAfter) {

    public boolean isInitialization() {
        return valueBefore == null;
    }
}
