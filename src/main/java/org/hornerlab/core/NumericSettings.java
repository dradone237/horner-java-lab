package org.hornerlab.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Politique de calcul numérique utilisée par les algorithmes.
 *
 * <ul>
 *   <li><b>Mode exact</b> : additions et multiplications {@link BigDecimal} sans aucun arrondi.
 *       La taille des nombres croît avec le degré ; elle est bornée par {@code maxExactDigits}
 *       (environ, en chiffres décimaux du nombre non mis à l'échelle) pour éviter
 *       une consommation de mémoire et de temps excessive.</li>
 *   <li><b>Mode précision contrôlée</b> : chaque opération est arrondie selon le
 *       {@link MathContext} (par défaut 34 chiffres significatifs, {@link RoundingMode#HALF_EVEN}).</li>
 * </ul>
 */
public record NumericSettings(CalculationMode mode, MathContext mathContext, int maxExactDigits) {

    public static final int DEFAULT_PRECISION = 34;
    public static final int MAX_PRECISION = 1000;
    public static final int DEFAULT_MAX_EXACT_DIGITS = 200_000;

    private static final double BITS_PER_DIGIT = Math.log(10) / Math.log(2);

    public NumericSettings {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(mathContext, "mathContext");
        if (mode == CalculationMode.EXACT && mathContext.getPrecision() != 0) {
            throw new IllegalArgumentException("Le mode exact exige MathContext.UNLIMITED.");
        }
        if (mode == CalculationMode.CONTROLLED
                && (mathContext.getPrecision() < 1 || mathContext.getPrecision() > MAX_PRECISION)) {
            throw new IllegalArgumentException(
                    "La précision doit être comprise entre 1 et " + MAX_PRECISION + " chiffres significatifs.");
        }
        if (maxExactDigits < 1) {
            throw new IllegalArgumentException("La limite de chiffres du mode exact doit être positive.");
        }
    }

    /** Mode exact avec la limite de taille par défaut. */
    public static NumericSettings exact() {
        return exact(DEFAULT_MAX_EXACT_DIGITS);
    }

    public static NumericSettings exact(int maxExactDigits) {
        return new NumericSettings(CalculationMode.EXACT, MathContext.UNLIMITED, maxExactDigits);
    }

    /** Précision contrôlée : 34 chiffres significatifs, HALF_EVEN. */
    public static NumericSettings controlled() {
        return controlled(DEFAULT_PRECISION);
    }

    public static NumericSettings controlled(int precision) {
        if (precision < 1 || precision > MAX_PRECISION) {
            throw new IllegalArgumentException(
                    "La précision doit être comprise entre 1 et " + MAX_PRECISION + " chiffres significatifs.");
        }
        return new NumericSettings(CalculationMode.CONTROLLED,
                new MathContext(precision, RoundingMode.HALF_EVEN), DEFAULT_MAX_EXACT_DIGITS);
    }

    public boolean isExact() {
        return mode == CalculationMode.EXACT;
    }

    /** Nombre de chiffres significatifs (0 en mode exact). */
    public int precision() {
        return mathContext.getPrecision();
    }

    /** a × b selon la politique de calcul. */
    public BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return isExact() ? a.multiply(b) : a.multiply(b, mathContext);
    }

    /** a + b selon la politique de calcul. */
    public BigDecimal add(BigDecimal a, BigDecimal b) {
        return isExact() ? a.add(b) : a.add(b, mathContext);
    }

    /**
     * En mode exact, vérifie que la valeur reste dans la limite de taille configurée.
     *
     * @throws PrecisionLimitExceededException si la limite est dépassée
     */
    public void checkSize(BigDecimal value) {
        if (isExact() && value.unscaledValue().bitLength() > Math.ceil(maxExactDigits * BITS_PER_DIGIT)) {
            throw new PrecisionLimitExceededException(
                    "Le calcul exact dépasse la limite de " + maxExactDigits + " chiffres. "
                            + "Utilisez le mode précision contrôlée (par exemple 34 chiffres significatifs).",
                    maxExactDigits);
        }
    }

    /** Unité d'arrondi u = ½·10^(1−p) en mode contrôlé ; 0 en mode exact. */
    public BigDecimal unitRoundoff() {
        if (isExact()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("0.5").scaleByPowerOfTen(1 - precision());
    }

    /** Description lisible en français. */
    public String describe() {
        if (isExact()) {
            return "exact (aucun arrondi intermédiaire)";
        }
        return "précision contrôlée (" + precision() + " chiffres significatifs, "
                + mathContext.getRoundingMode() + ")";
    }
}
