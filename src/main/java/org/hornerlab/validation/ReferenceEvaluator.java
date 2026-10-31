package org.hornerlab.validation;

import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/**
 * Oracle indépendant : évalue un polynôme directement par sa définition
 * P(x) = Σ a_i · x^i, sans jamais appeler l'algorithme de Horner.
 *
 * <p>Les puissances sont calculées par {@link BigDecimal#pow(int)}, la somme terme à terme.
 * Cette méthode est plus coûteuse que Horner ; elle sert de référence pour les tests.
 */
public final class ReferenceEvaluator {

    private ReferenceEvaluator() {
    }

    /** Résultat de l'évaluation naïve avec comptage des opérations. */
    public record NaiveEvaluation(BigDecimal value, long multiplications, long additions) {
    }

    /** Σ a_i · x^i en arithmétique exacte. */
    public static BigDecimal evaluate(Polynomial p, BigDecimal x) {
        Objects.requireNonNull(p, "p");
        Objects.requireNonNull(x, "x");
        BigDecimal sum = BigDecimal.ZERO;
        for (int power = 0; power < p.size(); power++) {
            BigDecimal a = p.coefficientOfPower(power);
            if (a.signum() != 0) {
                sum = sum.add(a.multiply(x.pow(power)));
            }
        }
        return sum;
    }

    /** Σ a_i · x^i, chaque opération étant arrondie selon {@code mc}. */
    public static BigDecimal evaluate(Polynomial p, BigDecimal x, MathContext mc) {
        Objects.requireNonNull(p, "p");
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(mc, "mc");
        BigDecimal sum = BigDecimal.ZERO;
        for (int power = 0; power < p.size(); power++) {
            BigDecimal a = p.coefficientOfPower(power);
            if (a.signum() != 0) {
                sum = sum.add(a.multiply(x.pow(power, mc), mc), mc);
            }
        }
        return sum;
    }

    /** P(0) = a_0. */
    public static BigDecimal valueAtZero(Polynomial p) {
        return p.coefficientOfPower(0);
    }

    /** P(1) = Σ a_i. */
    public static BigDecimal sumOfCoefficients(Polynomial p) {
        return p.coefficients().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** P(−1) = Σ (−1)^i · a_i. */
    public static BigDecimal alternatingSum(Polynomial p) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int power = 0; power < p.size(); power++) {
            BigDecimal a = p.coefficientOfPower(power);
            sum = power % 2 == 0 ? sum.add(a) : sum.subtract(a);
        }
        return sum;
    }

    /** P'(x) via la dérivée symbolique, évaluée terme à terme (indépendant du Horner étendu). */
    public static BigDecimal derivativeAt(Polynomial p, BigDecimal x) {
        return evaluate(p.derivative(), x);
    }

    /**
     * Évaluation naïve « par puissances » : chaque x^i est recalculé par i multiplications
     * successives. Coût : n(n+1)/2 multiplications pour les puissances, plus n+1 multiplications
     * par les coefficients et n additions. Sert de point de comparaison avec Horner.
     */
    public static NaiveEvaluation evaluateNaive(Polynomial p, BigDecimal x, NumericSettings settings) {
        Objects.requireNonNull(p, "p");
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(settings, "settings");
        BigDecimal sum = BigDecimal.ZERO;
        long multiplications = 0;
        long additions = 0;
        for (int power = 0; power < p.size(); power++) {
            BigDecimal xPower = BigDecimal.ONE;
            for (int k = 0; k < power; k++) {
                xPower = settings.multiply(xPower, x);
                multiplications++;
            }
            BigDecimal term = settings.multiply(p.coefficientOfPower(power), xPower);
            multiplications++;
            if (power == 0) {
                sum = term;
            } else {
                sum = settings.add(sum, term);
                additions++;
            }
        }
        return new NaiveEvaluation(sum, multiplications, additions);
    }
}
