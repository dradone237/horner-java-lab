package org.hornerlab.validation;

import org.hornerlab.core.Polynomial;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Générateur pseudo-aléatoire <b>reproductible</b> de polynômes et de valeurs de x.
 * Une même graine produit toujours la même séquence ({@link Random} est spécifié par le JDK).
 */
public final class RandomPolynomialGenerator {

    private final long seed;
    private final Random random;

    public RandomPolynomialGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public long seed() {
        return seed;
    }

    /**
     * Polynôme de degré tiré uniformément dans [0, maxDegree] : coefficients entiers positifs ou
     * négatifs, décimaux, et environ 15 % de zéros (le coefficient dominant reste non nul).
     */
    public Polynomial nextPolynomial(int maxDegree) {
        int degree = random.nextInt(maxDegree + 1);
        List<BigDecimal> coefficients = new ArrayList<>(degree + 1);
        for (int i = 0; i <= degree; i++) {
            BigDecimal c = nextCoefficient();
            while (i == 0 && degree > 0 && c.signum() == 0) {
                c = nextCoefficient();
            }
            coefficients.add(c);
        }
        return Polynomial.of(coefficients);
    }

    /** Coefficient : 15 % de zéros, 60 % d'entiers dans [−50, 50], 25 % de décimaux à 1–3 décimales. */
    public BigDecimal nextCoefficient() {
        int kind = random.nextInt(100);
        if (kind < 15) {
            return BigDecimal.ZERO;
        }
        if (kind < 75) {
            return BigDecimal.valueOf(random.nextInt(101) - 50L);
        }
        int scale = 1 + random.nextInt(3);
        return BigDecimal.valueOf(random.nextInt(20_001) - 10_000L, scale);
    }

    /** Valeur de x : entier dans [−10, 10] ou décimal à 1–2 décimales dans [−5, 5]. */
    public BigDecimal nextX() {
        if (random.nextBoolean()) {
            return BigDecimal.valueOf(random.nextInt(21) - 10L);
        }
        int scale = 1 + random.nextInt(2);
        long bound = 5L * (scale == 1 ? 10 : 100);
        return BigDecimal.valueOf(random.nextLong(2 * bound + 1) - bound, scale);
    }

    /** Scalaire k pour les tests de linéarité : entier dans [−20, 20] ou décimal à une décimale. */
    public BigDecimal nextScalar() {
        return random.nextBoolean()
                ? BigDecimal.valueOf(random.nextInt(41) - 20L)
                : BigDecimal.valueOf(random.nextInt(201) - 100L, 1);
    }
}
