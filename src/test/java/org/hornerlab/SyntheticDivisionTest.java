package org.hornerlab;

import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.RootStatus;
import org.hornerlab.core.SyntheticDivision;
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

/** Tests de la division synthétique par (x − r). */
class SyntheticDivisionTest {

    @Test
    @DisplayName("(2x³ − 6x² + 2x − 1) ÷ (x − 3) : Q = 2x² + 2, R = 5")
    void referenceExample() {
        DivisionResult d = SyntheticDivision.divide(Polynomial.of(2, -6, 2, -1), bd("3"));
        assertEquals(Polynomial.of(2, 0, 2), d.quotient());
        assertNumericEquals("5", d.remainder());
        assertTrue(d.identityHolds());
        assertEquals(0, d.identityMaxDeviation().signum());
        assertEquals(RootStatus.NOT_A_ROOT, d.rootStatus(null));
        assertEquals(Polynomial.of(1, -3), d.divisor());
    }

    @Test
    @DisplayName("(x³ − 6x² + 11x − 6) ÷ (x − 2) : Q = x² − 4x + 3, R = 0 → racine exacte")
    void exactRoot() {
        DivisionResult d = SyntheticDivision.divide(Polynomial.of(1, -6, 11, -6), bd("2"));
        assertEquals(Polynomial.of(1, -4, 3), d.quotient());
        assertEquals(0, d.remainder().signum());
        assertEquals(RootStatus.EXACT_ROOT, d.rootStatus(null));
    }

    @Test
    @DisplayName("Polynôme constant : quotient nul [0], reste = la constante")
    void constantPolynomial() {
        DivisionResult d = SyntheticDivision.divide(Polynomial.of(7), bd("4"));
        assertTrue(d.quotient().isZero());
        assertEquals(1, d.quotientCoefficients().size());
        assertNumericEquals("7", d.remainder());
        assertTrue(d.identityHolds());
    }

    @Test
    @DisplayName("Polynôme nul : quotient nul, reste nul, r est une racine")
    void zeroPolynomial() {
        DivisionResult d = SyntheticDivision.divide(Polynomial.zero(), bd("4"));
        assertTrue(d.quotient().isZero());
        assertEquals(0, d.remainder().signum());
        assertEquals(RootStatus.EXACT_ROOT, d.rootStatus(null));
    }

    @ParameterizedTest(name = "[{index}] [{0}] ÷ (x − {1}) → Q = [{2}], R = {3}")
    @CsvSource(delimiter = '|', textBlock = """
            2,-6,2,-1       | 0     | 2,-6,2      | -1
            1,0,-1          | -1    | 1,-1        | 0
            1,0,-1          | 1     | 1,1         | 0
            3,0,0,0,2       | 1     | 3,3,3,3     | 5
            4,-8            | 2     | 4           | 0
            1,-2.5,1        | 0.5   | 1,-2        | 0
            2,1             | -0.5  | 2           | 0
            """)
    void dividesAndChecksIdentity(String coefficients, String root, String quotient, String remainder) {
        Polynomial p = poly(coefficients);
        DivisionResult d = SyntheticDivision.divide(p, bd(root));
        assertEquals(poly(quotient), d.quotient());
        assertNumericEquals(remainder, d.remainder());
        assertTrue(d.identityHolds(), "P(x) = (x − r)Q(x) + R");
        assertEquals(0, d.remainder().compareTo(HornerEvaluator.evaluate(p, bd(root))), "R = P(r)");
    }

    @Test
    @DisplayName("Mode contrôlé : 3x − 1 en r ≈ 1/3 → racine approximative, jamais exacte")
    void approximateRootInControlledMode() {
        Polynomial p = Polynomial.of(3, -1);
        BigDecimal r = bd("0.3333333333");
        DivisionResult exact = SyntheticDivision.divide(p, r);
        assertNumericEquals("-1E-10", exact.remainder());
        assertEquals(RootStatus.NOT_A_ROOT, exact.rootStatus(bd("1")), "mode exact : la tolérance est ignorée");

        DivisionResult controlled = SyntheticDivision.divide(p, r, NumericSettings.controlled(34));
        assertEquals(RootStatus.APPROXIMATE_ROOT, controlled.rootStatus(bd("1E-9")));
        assertEquals(RootStatus.NOT_A_ROOT, controlled.rootStatus(bd("1E-12")));
        assertThrows(IllegalArgumentException.class, () -> controlled.rootStatus(bd("-1")));
        assertThrows(NullPointerException.class, () -> controlled.rootStatus(null));
    }

    @Test
    @DisplayName("Mode contrôlé : écart d'identité dû aux arrondis, mesuré et non masqué")
    void controlledIdentityDeviationIsMeasured() {
        DivisionResult d = SyntheticDivision.divide(Polynomial.of(1, 1, 1, 1), bd("1.23456789"), NumericSettings.controlled(5));
        assertTrue(d.identityMaxDeviation().signum() > 0);
        assertEquals(false, d.identityHolds());
    }
}
