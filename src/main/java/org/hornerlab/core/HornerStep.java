package org.hornerlab.core;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Une étape réellement exécutée de la méthode de Horner.
 *
 * <p>L'étape n°1 est l'initialisation (acc ← a_n) : {@code accumulatorBefore} et {@code product}
 * valent alors {@code null}. Les étapes suivantes calculent
 * {@code accumulatorAfter = accumulatorBefore × x + coefficient}, avec
 * {@code product = accumulatorBefore × x}.
 *
 * @param number            numéro d'affichage (1 = initialisation)
 * @param power             puissance de x associée au coefficient ajouté
 * @param coefficient       coefficient traité à cette étape
 * @param accumulatorBefore accumulateur avant l'étape ({@code null} pour l'initialisation)
 * @param product           accumulateur × x ({@code null} pour l'initialisation)
 * @param accumulatorAfter  accumulateur après l'étape
 */
public record HornerStep(int number, int power, BigDecimal coefficient, BigDecimal accumulatorBefore,
                         BigDecimal product, BigDecimal accumulatorAfter) {

    public HornerStep {
        if (number < 1 || power < 0) {
            throw new IllegalArgumentException("Numéro d'étape ou puissance invalide.");
        }
        Objects.requireNonNull(coefficient, "coefficient");
        Objects.requireNonNull(accumulatorAfter, "accumulatorAfter");
        if ((accumulatorBefore == null) != (product == null)) {
            throw new IllegalArgumentException("Une étape de calcul exige l'accumulateur précédent et le produit.");
        }
    }

    /** Étape d'initialisation : acc ← a_n. */
    public static HornerStep initialization(int power, BigDecimal leadingCoefficient) {
        return new HornerStep(1, power, leadingCoefficient, null, null, leadingCoefficient);
    }

    public boolean isInitialization() {
        return accumulatorBefore == null;
    }
}
