package org.hornerlab;

import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.RootStatus;
import org.hornerlab.core.SyntheticDivision;
import org.hornerlab.validation.ReferenceEvaluator;
import org.hornerlab.validation.ResultVerifier;
import org.hornerlab.validation.ResultVerifier.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import static org.hornerlab.HornerEvaluatorTest.assertNumericEquals;
import static org.hornerlab.HornerEvaluatorTest.bd;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Oracle indépendant et vérificateur de résultats (problème de l'oracle). */
class ReferenceEvaluatorTest {

    private static final Polynomial REF = Polynomial.of(2, -6, 2, -1);

    @Test
    @DisplayName("Oracle : somme des termes 2·27 − 6·9 + 2·3 − 1 = 5")
    void oracleMatchesDefinition() {
        assertNumericEquals("5", ReferenceEvaluator.evaluate(REF, bd("3")));
        assertNumericEquals("5", ReferenceEvaluator.evaluate(REF, bd("3"), MathContext.DECIMAL64));
        assertNumericEquals("0", ReferenceEvaluator.evaluate(Polynomial.zero(), bd("3")));
        assertNumericEquals("-1", ReferenceEvaluator.evaluate(REF, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("P(0), P(1), P(−1) et dérivée symbolique")
    void specialValues() {
        assertNumericEquals("-1", ReferenceEvaluator.valueAtZero(REF));
        assertNumericEquals("-3", ReferenceEvaluator.sumOfCoefficients(REF));
        assertNumericEquals("-11", ReferenceEvaluator.alternatingSum(REF));
        assertNumericEquals("20", ReferenceEvaluator.derivativeAt(REF, bd("3")));
    }

    @Test
    @DisplayName("Évaluation naïve : n(n+1)/2 + n + 1 multiplications et n additions")
    void naiveOperationCount() {
        ReferenceEvaluator.NaiveEvaluation naive = ReferenceEvaluator.evaluateNaive(REF, bd("3"), NumericSettings.exact());
        assertNumericEquals("5", naive.value());
        assertEquals(6 + 4, naive.multiplications());
        assertEquals(3, naive.additions());
        assertNumericEquals("7", ReferenceEvaluator.evaluateNaive(Polynomial.of(7), bd("9"), NumericSettings.exact()).value());
    }

    @Test
    @DisplayName("Arguments absents refusés par l'oracle")
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> ReferenceEvaluator.evaluate(null, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> ReferenceEvaluator.evaluate(REF, null, MathContext.DECIMAL32));
        assertThrows(NullPointerException.class, () -> ReferenceEvaluator.evaluate(REF, BigDecimal.ONE, null));
        assertThrows(NullPointerException.class, () -> ReferenceEvaluator.evaluateNaive(REF, BigDecimal.ONE, null));
    }

    @Test
    @DisplayName("Comparateur : identique, proche sous tolérance, incohérent")
    void compareDistinguishesThreeVerdicts() {
        assertEquals(Verdict.IDENTICAL, ResultVerifier.compare("a", bd("5"), bd("5.00"), BigDecimal.ZERO, "m").verdict());
        assertEquals(Verdict.WITHIN_TOLERANCE, ResultVerifier.compare("a", bd("5.001"), bd("5"), bd("0.01"), "m").verdict());
        ResultVerifier.Check mismatch = ResultVerifier.compare("a", bd("6"), bd("5"), bd("0.01"), "m");
        assertEquals(Verdict.MISMATCH, mismatch.verdict());
        assertFalse(mismatch.passed());
        assertNumericEquals("1", mismatch.difference());
        assertEquals("Incohérent", mismatch.verdict().label());
    }

    @Test
    @DisplayName("Mode exact : tous les contrôles identiques pour l'exemple de référence")
    void exactVerificationIsIdentical() {
        List<ResultVerifier.Check> checks = ResultVerifier.verifyAll(REF, bd("3"), NumericSettings.exact());
        assertEquals(7, checks.size());
        assertTrue(checks.stream().allMatch(c -> c.verdict() == Verdict.IDENTICAL), checks::toString);
    }

    @Test
    @DisplayName("Mode contrôlé : écarts d'arrondi admis sous la borne, jamais au-delà")
    void controlledVerificationUsesErrorBound() {
        Polynomial p = Polynomial.of(bd("1.1"), bd("-2.3"), bd("3.7"), bd("-0.9"), bd("5.3"));
        List<ResultVerifier.Check> checks = ResultVerifier.verifyAll(p, bd("1.234567"), NumericSettings.controlled(8));
        assertTrue(checks.stream().allMatch(ResultVerifier.Check::passed), checks::toString);
        assertTrue(checks.stream().anyMatch(c -> c.verdict() == Verdict.WITHIN_TOLERANCE), checks::toString);
        assertTrue(checks.stream().allMatch(c -> c.tolerance().signum() >= 0));
    }

    @Test
    @DisplayName("Précision 1 chiffre : borne non informative mais contrôle exécuté")
    void degenerateBound() {
        Polynomial p = Polynomial.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        ResultVerifier.Check c = ResultVerifier.verifyEvaluation(p, bd("1.5"), NumericSettings.controlled(1));
        assertTrue(c.tolerance().signum() > 0);
    }

    @Test
    @DisplayName("Classement des racines : exacte, approximative, non racine")
    void classifyRoots() {
        assertEquals(RootStatus.EXACT_ROOT, ResultVerifier.classifyRoot(
                SyntheticDivision.divide(Polynomial.of(1, -6, 11, -6), bd("2"))));
        DivisionResult approx = SyntheticDivision.divide(Polynomial.of(1, -2, 1), bd("1.00000000000000000001"),
                NumericSettings.controlled());
        assertEquals(RootStatus.APPROXIMATE_ROOT, ResultVerifier.classifyRoot(approx));
        assertEquals(RootStatus.NOT_A_ROOT, ResultVerifier.classifyRoot(
                SyntheticDivision.divide(Polynomial.of(1, -2, 1), bd("1.00000000000000000001"))));
        assertEquals(RootStatus.NOT_A_ROOT, ResultVerifier.classifyRoot(
                SyntheticDivision.divide(REF, bd("3"), NumericSettings.controlled())));
    }

    @Test
    @DisplayName("Identité de division en mode contrôlé : sous la borne d'arrondi")
    void controlledIdentity() {
        DivisionResult d = SyntheticDivision.divide(Polynomial.of(1, 1, 1, 1), bd("1.23456789"), NumericSettings.controlled(5));
        ResultVerifier.Check c = ResultVerifier.verifyIdentity(d);
        assertEquals(Verdict.WITHIN_TOLERANCE, c.verdict(), c::toString);
        assertEquals(Verdict.WITHIN_TOLERANCE, ResultVerifier.verifyRemainder(d).verdict());
    }
}
