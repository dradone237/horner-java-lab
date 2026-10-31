package org.hornerlab;

import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.HornerResult;
import org.hornerlab.core.HornerStep;
import org.hornerlab.core.InvalidPolynomialException;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.PrecisionLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests unitaires de l'évaluation par la méthode de Horner. */
class HornerEvaluatorTest {

    private static final Polynomial REFERENCE = Polynomial.of(2, -6, 2, -1);

    static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    static Polynomial poly(String commaSeparated) {
        return Polynomial.of(Arrays.stream(commaSeparated.split(",")).map(String::strip).map(BigDecimal::new).toList());
    }

    static void assertNumericEquals(String expected, BigDecimal actual) {
        assertEquals(0, bd(expected).compareTo(actual), () -> "attendu " + expected + " mais obtenu " + actual.toPlainString());
    }

    @Nested
    @DisplayName("Exemples de référence de l'énoncé")
    class Reference {

        @Test
        @DisplayName("P(x) = 2x³ − 6x² + 2x − 1 : P(3) = 5")
        void referenceExampleGivesFive() {
            assertNumericEquals("5", HornerEvaluator.evaluate(REFERENCE, bd("3")));
        }

        @Test
        @DisplayName("Les quatre étapes de P(3) sont 2, 0, 2, 5")
        void referenceExampleStepsAreExact() {
            HornerResult result = HornerEvaluator.evaluateWithSteps(REFERENCE, bd("3"));
            List<HornerStep> steps = result.steps();

            assertEquals(4, steps.size());
            assertTrue(steps.get(0).isInitialization());
            assertNull(steps.get(0).accumulatorBefore());
            assertNumericEquals("2", steps.get(0).accumulatorAfter());

            // Étape 2 : 2 × 3 − 6 = 0
            assertNumericEquals("2", steps.get(1).accumulatorBefore());
            assertNumericEquals("6", steps.get(1).product());
            assertNumericEquals("-6", steps.get(1).coefficient());
            assertNumericEquals("0", steps.get(1).accumulatorAfter());
            // Étape 3 : 0 × 3 + 2 = 2
            assertNumericEquals("0", steps.get(2).product());
            assertNumericEquals("2", steps.get(2).accumulatorAfter());
            // Étape 4 : 2 × 3 − 1 = 5
            assertNumericEquals("6", steps.get(3).product());
            assertNumericEquals("5", steps.get(3).accumulatorAfter());

            assertNumericEquals("5", result.value());
            assertEquals(3, result.multiplications());
            assertEquals(3, result.additions());
            assertEquals(3, result.iterations());
            assertEquals(List.of(1, 2, 3, 4), steps.stream().map(HornerStep::number).toList());
            assertEquals(List.of(3, 2, 1, 0), steps.stream().map(HornerStep::power).toList());
        }

        @Test
        @DisplayName("x³ − 6x² + 11x − 6 : P(2) = 0 (2 est une racine)")
        void knownRootGivesZero() {
            BigDecimal value = HornerEvaluator.evaluate(Polynomial.of(1, -6, 11, -6), bd("2"));
            assertEquals(0, value.signum());
        }

        @Test
        @DisplayName("L'ordre des opérations est : ×x puis +coefficient")
        void operationSequenceIsMultiplyThenAdd() {
            HornerResult result = HornerEvaluator.evaluateWithSteps(REFERENCE, bd("3"));
            assertEquals(List.of("× 3", "+ (-6)", "× 3", "+ 2", "× 3", "+ (-1)"), result.operationSequence());
        }
    }

    @ParameterizedTest(name = "[{index}] P = [{0}], x = {1} → {2}")
    @DisplayName("Cas normaux, particuliers et limites")
    @CsvSource(delimiter = '|', textBlock = """
            # cubique, quadratique, linéaire, constant
            2,-6,2,-1          | 3        | 5
            1,-3,2             | 5        | 12
            4,-8               | 2        | 0
            7                  | 123      | 7
            # coefficients décimaux
            1.5,-2.25,0,3      | 0.5      | 2.625
            0.1,0.2            | 0.3      | 0.23
            # polynôme nul
            0                  | 42       | 0
            0,0,0              | -7       | 0
            # x = 0 : terme constant ; x = 1 : somme ; x = -1 : somme alternée
            2,-6,2,-1          | 0        | -1
            2,-6,2,-1          | 1        | -3
            2,-6,2,-1          | -1       | -11
            # coefficients négatifs, zéros intermédiaires, zéros dominants
            -1,-1,-1           | 2        | -7
            3,0,0,0,2          | -1       | 5
            0,0,2,-6,2,-1      | 3        | 5
            # alternance de signes et nombreux zéros
            1,-1,1,-1,1,-1,1   | 2        | 43
            1,0,0,0,0,0,0,0,0,0,0 | 2     | 1024
            # grands coefficients, petites valeurs, notation scientifique
            1e30,-2e30,1e30    | 1.000001 | 1E+18
            1e-20,1e-20        | 1e-10    | 1.0000000001E-20
            2.5E+3,-1E-2       | 4        | 9999.99
            # résultat de forte amplitude
            1,0,0,0,0,0        | 1000     | 1E+15
            """)
    void evaluatesExpectedValues(String coefficients, String x, String expected) {
        Polynomial p = poly(coefficients);
        assertNumericEquals(expected, HornerEvaluator.evaluate(p, bd(x)));
        assertNumericEquals(expected, HornerEvaluator.evaluateWithSteps(p, bd(x)).value());
    }

    @Test
    @DisplayName("Résultat très proche de zéro : 1E-40 n'est jamais arrondi à 0 en mode exact")
    void tinyResultIsNotConfusedWithZeroInExactMode() {
        Polynomial square = Polynomial.of(1, -2, 1); // (x − 1)²
        BigDecimal value = HornerEvaluator.evaluate(square, bd("1.00000000000000000001"));
        assertNumericEquals("1E-40", value);
        assertEquals(1, value.signum());
    }

    @Test
    @DisplayName("Mode précision contrôlée : l'annulation fait perdre 1E-40 (documenté)")
    void controlledModeLosesTinyResult() {
        Polynomial square = Polynomial.of(1, -2, 1);
        BigDecimal value = HornerEvaluator.evaluate(square, bd("1.00000000000000000001"), NumericSettings.controlled(34));
        assertEquals(0, value.signum());
    }

    @Test
    @DisplayName("Mode contrôlé : chaque opération est arrondie à la précision demandée")
    void controlledModeRoundsToPrecision() {
        BigDecimal value = HornerEvaluator.evaluate(Polynomial.of(1, 0, 0), bd("1.23456789"), NumericSettings.controlled(5));
        assertEquals(5, value.precision());
        assertNumericEquals("1.5242", value); // 1.23456789² = 1.524157875… arrondi en deux temps
    }

    @Test
    @DisplayName("Mode exact et contrôlé coïncident lorsque aucun arrondi n'est nécessaire")
    void exactAndControlledAgreeOnIntegers() {
        assertNumericEquals("5", HornerEvaluator.evaluate(REFERENCE, bd("3"), NumericSettings.controlled(34)));
    }

    @Test
    @DisplayName("Polynôme constant et polynôme nul : aucune opération")
    void constantPolynomialNeedsNoOperation() {
        HornerResult constant = HornerEvaluator.evaluateWithSteps(Polynomial.of(7), bd("123"));
        assertEquals(0, constant.multiplications());
        assertEquals(0, constant.additions());
        assertEquals(1, constant.steps().size());

        HornerResult zero = HornerEvaluator.evaluateWithSteps(Polynomial.zero(), bd("5"));
        assertEquals(0, zero.value().signum());
        assertEquals(0, zero.iterations());
    }

    @Test
    @DisplayName("Degré élevé (1 000) : n multiplications et n additions")
    void highDegreeCountsLinearOperations() {
        BigDecimal[] coefficients = new BigDecimal[1001];
        Arrays.fill(coefficients, BigDecimal.ONE);
        HornerResult result = HornerEvaluator.evaluateWithSteps(Polynomial.of(coefficients), BigDecimal.ONE);
        assertNumericEquals("1001", result.value());
        assertEquals(1000, result.multiplications());
        assertEquals(1000, result.additions());
    }

    @Test
    @DisplayName("Évaluation à partir d'une liste brute de coefficients")
    void evaluatesRawCoefficientList() {
        assertNumericEquals("5", HornerEvaluator.evaluate(List.of(bd("2"), bd("-6"), bd("2"), bd("-1")), bd("3")));
    }

    @Test
    @DisplayName("Interface flottante : valeurs finies acceptées, NaN et infini refusés")
    void doubleInterfaceRejectsNaNAndInfinity() {
        assertNumericEquals("5", HornerEvaluator.evaluate(new double[] {2, -6, 2, -1}, 3.0));
        assertNumericEquals("0.23", HornerEvaluator.evaluate(new double[] {0.1, 0.2}, 0.3));
        assertThrows(IllegalArgumentException.class, () -> HornerEvaluator.evaluate(new double[] {1, 2}, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> HornerEvaluator.evaluate(new double[] {1, 2}, Double.POSITIVE_INFINITY));
        assertThrows(InvalidPolynomialException.class, () -> HornerEvaluator.evaluate(new double[] {1, Double.NaN}, 1.0));
        assertThrows(InvalidPolynomialException.class, () -> HornerEvaluator.evaluate(new double[] {Double.NEGATIVE_INFINITY}, 1.0));
        assertThrows(InvalidPolynomialException.class, () -> HornerEvaluator.evaluate((double[]) null, 1.0));
    }

    @Test
    @DisplayName("Arguments absents : exceptions explicites")
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> HornerEvaluator.evaluate((Polynomial) null, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> HornerEvaluator.evaluate(REFERENCE, null));
        assertThrows(NullPointerException.class, () -> HornerEvaluator.evaluate(REFERENCE, BigDecimal.ONE, null));
        assertThrows(NullPointerException.class, () -> HornerEvaluator.evaluateWithSteps(null, BigDecimal.ONE));
        assertThrows(InvalidPolynomialException.class, () -> HornerEvaluator.evaluate(List.of(), BigDecimal.ONE));
        assertThrows(InvalidPolynomialException.class, () -> HornerEvaluator.evaluate((List<BigDecimal>) null, BigDecimal.ONE));
    }

    @Test
    @DisplayName("Mode exact : la limite de taille des nombres est respectée")
    void exactModeEnforcesDigitLimit() {
        BigDecimal[] coefficients = new BigDecimal[200];
        Arrays.fill(coefficients, BigDecimal.ONE);
        Polynomial p = Polynomial.of(coefficients);
        NumericSettings tight = NumericSettings.exact(50);
        PrecisionLimitExceededException e = assertThrows(PrecisionLimitExceededException.class,
                () -> HornerEvaluator.evaluate(p, bd("123.456"), tight));
        assertEquals(50, e.limitDigits());
        assertTrue(e.getMessage().contains("précision contrôlée"));
        // Même calcul en mode contrôlé : accepté
        assertFalse(HornerEvaluator.evaluate(p, bd("123.456"), NumericSettings.controlled(34)).signum() == 0);
    }
}
