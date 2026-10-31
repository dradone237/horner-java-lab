package org.hornerlab;

import org.hornerlab.core.DerivativeEvaluator;
import org.hornerlab.core.DerivativeResult;
import org.hornerlab.core.DerivativeStep;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.hornerlab.HornerEvaluatorTest.assertNumericEquals;
import static org.hornerlab.HornerEvaluatorTest.bd;
import static org.hornerlab.HornerEvaluatorTest.poly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests du schéma de Horner étendu (P et P' en un seul parcours). */
class DerivativeEvaluatorTest {

    @Test
    @DisplayName("P(x) = 2x³ − 6x² + 2x − 1 : P(3) = 5 et P'(3) = 20")
    void referenceExample() {
        DerivativeResult r = DerivativeEvaluator.evaluate(Polynomial.of(2, -6, 2, -1), bd("3"));
        assertNumericEquals("5", r.value());
        assertNumericEquals("20", r.derivative());
        assertEquals(6, r.multiplications());
        assertEquals(6, r.additions());
        assertEquals(4, r.steps().size());
        assertTrue(r.steps().get(0).isInitialization());
    }

    @Test
    @DisplayName("Ordre des mises à jour : P(x) = x², P'(3) = 6 (et non 18)")
    void updateOrderMatters() {
        // Mettre à jour b avant d donnerait 18 : ce test détecte cette erreur classique.
        DerivativeResult r = DerivativeEvaluator.evaluate(Polynomial.of(1, 0, 0), bd("3"));
        assertNumericEquals("9", r.value());
        assertNumericEquals("6", r.derivative());
    }

    @Test
    @DisplayName("Étapes intermédiaires de b et d pour l'exemple de référence")
    void referenceSteps() {
        DerivativeResult r = DerivativeEvaluator.evaluate(Polynomial.of(2, -6, 2, -1), bd("3"));
        // b : 2, 0, 2, 5   d : 0, 2, 6, 20
        String[] b = {"2", "0", "2", "5"};
        String[] d = {"0", "2", "6", "20"};
        for (int i = 0; i < 4; i++) {
            DerivativeStep step = r.steps().get(i);
            assertNumericEquals(b[i], step.valueAfter());
            assertNumericEquals(d[i], step.derivativeAfter());
        }
    }

    @ParameterizedTest(name = "[{index}] P = [{0}], x = {1} → P = {2}, P' = {3}")
    @CsvSource(delimiter = '|', textBlock = """
            7               | 5    | 7      | 0
            0               | 5    | 0      | 0
            4,-8            | 10   | 32     | 4
            1,-6,11,-6      | 2    | 0      | -1
            3,0,0,0,2       | -1   | 5      | -12
            1.5,-2.25,0,3   | 0.5  | 2.625  | -1.125
            1,0,0,0,0,0     | 0    | 0      | 0
            1,1             | 0    | 1      | 1
            """)
    void evaluatesValueAndDerivative(String coefficients, String x, String value, String derivative) {
        DerivativeResult r = DerivativeEvaluator.evaluate(poly(coefficients), bd(x));
        assertNumericEquals(value, r.value());
        assertNumericEquals(derivative, r.derivative());
    }

    @Test
    @DisplayName("Mode contrôlé : résultats arrondis, cohérents avec le mode exact")
    void controlledMode() {
        DerivativeResult r = DerivativeEvaluator.evaluate(Polynomial.of(2, -6, 2, -1), bd("3"), NumericSettings.controlled(10));
        assertNumericEquals("5", r.value());
        assertNumericEquals("20", r.derivative());
    }

    @Test
    @DisplayName("Arguments absents refusés")
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> DerivativeEvaluator.evaluate(null, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> DerivativeEvaluator.evaluate(Polynomial.of(1), null));
        assertThrows(NullPointerException.class, () -> DerivativeEvaluator.evaluate(Polynomial.of(1), BigDecimal.ONE, null));
    }
}
