package org.hornerlab;

import org.hornerlab.core.DerivativeEvaluator;
import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.SyntheticDivision;
import org.hornerlab.validation.RandomPolynomialGenerator;
import org.hornerlab.validation.ReferenceEvaluator;
import org.hornerlab.validation.ResultVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests par propriétés sur des données aléatoires reproductibles.
 * Graine par défaut 20261031 ; autre graine : {@code mvn test -Dhorner.seed=123}.
 */
class HornerPropertyTest {

    static final long SEED = Long.getLong("horner.seed", 20261031L);
    static final int CASES = 500;
    static final int MAX_DEGREE = 12;

    private static String context(int i, Polynomial p, BigDecimal x) {
        return "graine " + SEED + ", cas n°" + i + " : P = " + p.coefficients() + ", x = " + x;
    }

    @Test
    @DisplayName("500 cas : Horner = oracle indépendant Σ a_i·x^i (égalité exacte)")
    void hornerMatchesOracle() {
        RandomPolynomialGenerator gen = new RandomPolynomialGenerator(SEED);
        for (int i = 0; i < CASES; i++) {
            Polynomial p = gen.nextPolynomial(MAX_DEGREE);
            BigDecimal x = gen.nextX();
            BigDecimal h = HornerEvaluator.evaluate(p, x);
            final int n = i;
            assertEquals(0, h.compareTo(ReferenceEvaluator.evaluate(p, x)), () -> context(n, p, x));
            assertEquals(0, h.compareTo(HornerEvaluator.evaluateWithSteps(p, x).value()), () -> context(n, p, x));
        }
    }

    @Test
    @DisplayName("500 cas : P(0) = a₀, P(1) = Σ a_i, P(−1) = somme alternée")
    void specialPoints() {
        RandomPolynomialGenerator gen = new RandomPolynomialGenerator(SEED + 1);
        for (int i = 0; i < CASES; i++) {
            Polynomial p = gen.nextPolynomial(MAX_DEGREE);
            final int n = i;
            assertEquals(0, HornerEvaluator.evaluate(p, BigDecimal.ZERO).compareTo(p.constantTerm()), () -> context(n, p, BigDecimal.ZERO));
            assertEquals(0, HornerEvaluator.evaluate(p, BigDecimal.ONE).compareTo(ReferenceEvaluator.sumOfCoefficients(p)), () -> context(n, p, BigDecimal.ONE));
            assertEquals(0, HornerEvaluator.evaluate(p, BigDecimal.ONE.negate()).compareTo(ReferenceEvaluator.alternatingSum(p)), () -> context(n, p, BigDecimal.ONE.negate()));
        }
    }

    @Test
    @DisplayName("500 cas : linéarité (k·P)(x) = k·P(x) et (P + Q)(x) = P(x) + Q(x)")
    void linearity() {
        RandomPolynomialGenerator gen = new RandomPolynomialGenerator(SEED + 2);
        for (int i = 0; i < CASES; i++) {
            Polynomial p = gen.nextPolynomial(MAX_DEGREE);
            Polynomial q = gen.nextPolynomial(MAX_DEGREE);
            BigDecimal x = gen.nextX();
            BigDecimal k = gen.nextScalar();
            BigDecimal px = HornerEvaluator.evaluate(p, x);
            final int n = i;
            assertEquals(0, HornerEvaluator.evaluate(p.scale(k), x).compareTo(px.multiply(k)), () -> context(n, p, x) + ", k = " + k);
            assertEquals(0, HornerEvaluator.evaluate(p.add(q), x).compareTo(px.add(HornerEvaluator.evaluate(q, x))),
                    () -> context(n, p, x) + ", Q = " + q.coefficients());
        }
    }

    @Test
    @DisplayName("500 cas : reste de la division = P(r) et P = (x − r)Q + R")
    void syntheticDivision() {
        RandomPolynomialGenerator gen = new RandomPolynomialGenerator(SEED + 3);
        for (int i = 0; i < CASES; i++) {
            Polynomial p = gen.nextPolynomial(MAX_DEGREE);
            BigDecimal r = gen.nextX();
            DivisionResult d = SyntheticDivision.divide(p, r);
            final int n = i;
            assertEquals(0, d.remainder().compareTo(ReferenceEvaluator.evaluate(p, r)), () -> context(n, p, r));
            assertTrue(d.identityHolds(), () -> context(n, p, r));
            assertEquals(Math.max(0, p.degree() - 1), Math.max(0, d.quotient().degree()), () -> context(n, p, r));
        }
    }

    @Test
    @DisplayName("500 cas : P'(x) par Horner étendu = dérivée symbolique évaluée par l'oracle")
    void derivative() {
        RandomPolynomialGenerator gen = new RandomPolynomialGenerator(SEED + 4);
        for (int i = 0; i < CASES; i++) {
            Polynomial p = gen.nextPolynomial(MAX_DEGREE);
            BigDecimal x = gen.nextX();
            final int n = i;
            var r = DerivativeEvaluator.evaluate(p, x);
            assertEquals(0, r.derivative().compareTo(ReferenceEvaluator.derivativeAt(p, x)), () -> context(n, p, x));
            assertEquals(0, r.value().compareTo(HornerEvaluator.evaluate(p, x)), () -> context(n, p, x));
        }
    }

    @Test
    @DisplayName("500 cas : en précision contrôlée (16 chiffres), l'écart reste sous la borne d'erreur")
    void controlledModeWithinBound() {
        RandomPolynomialGenerator gen = new RandomPolynomialGenerator(SEED + 5);
        for (int i = 0; i < CASES; i++) {
            Polynomial p = gen.nextPolynomial(MAX_DEGREE);
            BigDecimal x = gen.nextX();
            final int n = i;
            ResultVerifier.Check c = ResultVerifier.verifyEvaluation(p, x, NumericSettings.controlled(16));
            assertTrue(c.passed(), () -> context(n, p, x) + " → " + c);
            ResultVerifier.Check d = ResultVerifier.verifyDerivative(p, x, NumericSettings.controlled(16));
            assertTrue(d.passed(), () -> context(n, p, x) + " → " + d);
        }
    }

    @Test
    @DisplayName("Reproductibilité : même graine, mêmes polynômes")
    void generatorIsReproducible() {
        RandomPolynomialGenerator a = new RandomPolynomialGenerator(42);
        RandomPolynomialGenerator b = new RandomPolynomialGenerator(42);
        for (int i = 0; i < 50; i++) {
            assertEquals(a.nextPolynomial(8), b.nextPolynomial(8));
            assertEquals(0, a.nextX().compareTo(b.nextX()));
        }
        assertEquals(42, a.seed());
    }
}
