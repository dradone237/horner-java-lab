package org.hornerlab;

import org.hornerlab.core.CalculationMode;
import org.hornerlab.core.HornerStep;
import org.hornerlab.core.InvalidPolynomialException;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.PolynomialFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;

import static org.hornerlab.HornerEvaluatorTest.assertNumericEquals;
import static org.hornerlab.HornerEvaluatorTest.bd;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests du modèle Polynomial, du formateur et de NumericSettings (boîte blanche du paquet core). */
class PolynomialTest {

    @Test
    @DisplayName("Convention décroissante : [2, -6, 2, -1] = 2x³ − 6x² + 2x − 1")
    void descendingConvention() {
        Polynomial p = Polynomial.of(2, -6, 2, -1);
        assertEquals(3, p.degree());
        assertNumericEquals("2", p.leadingCoefficient());
        assertNumericEquals("-1", p.constantTerm());
        assertNumericEquals("-6", p.coefficientOfPower(2));
        assertNumericEquals("0", p.coefficientOfPower(10));
        assertThrows(IllegalArgumentException.class, () -> p.coefficientOfPower(-1));
        assertEquals("2x^3 - 6x^2 + 2x - 1", p.toString());
    }

    @Test
    @DisplayName("Zéros dominants supprimés, zéros intermédiaires conservés")
    void normalization() {
        assertEquals(List.of(bd("3"), bd("0"), bd("0"), bd("0"), bd("2")), Polynomial.of(0, 0, 3, 0, 0, 0, 2).coefficients());
        assertEquals(4, Polynomial.of(0, 3, 0, 0, 0, 2).degree());
    }

    @Test
    @DisplayName("Polynôme nul : [0], degré −1 ; constante : degré 0")
    void zeroAndConstant() {
        assertTrue(Polynomial.of(0, 0, 0).isZero());
        assertEquals(-1, Polynomial.zero().degree());
        assertEquals(Polynomial.zero(), Polynomial.of(bd("0.000")));
        assertEquals(0, Polynomial.of(7).degree());
        assertTrue(Polynomial.of(7).isConstant());
        assertFalse(Polynomial.of(7).isZero());
        assertEquals(1, Polynomial.zero().size());
    }

    @Test
    @DisplayName("Entrées invalides : null, liste vide, coefficient null, NaN")
    void invalidInputs() {
        assertThrows(InvalidPolynomialException.class, () -> Polynomial.of((List<BigDecimal>) null));
        assertThrows(InvalidPolynomialException.class, () -> Polynomial.of(List.of()));
        assertThrows(InvalidPolynomialException.class, () -> Polynomial.of((BigDecimal[]) null));
        assertThrows(InvalidPolynomialException.class, () -> Polynomial.of((long[]) null));
        assertThrows(InvalidPolynomialException.class, () -> Polynomial.of(Arrays.asList(BigDecimal.ONE, null)));
        assertThrows(InvalidPolynomialException.class, () -> Polynomial.ofDoubles(1.0, Double.NaN));
        assertThrows(NullPointerException.class, () -> Polynomial.constant(null));
        assertThrows(NullPointerException.class, () -> Polynomial.linearFactor(null));
    }

    @Test
    @DisplayName("ofDoubles : 0.1 converti exactement en 0.1")
    void doublesAreConvertedByShortestRepresentation() {
        assertEquals(Polynomial.of(bd("0.1"), bd("0.2")), Polynomial.ofDoubles(0.1, 0.2));
    }

    @Test
    @DisplayName("Dérivée, somme, produit, multiplication par un scalaire")
    void algebra() {
        Polynomial p = Polynomial.of(2, -6, 2, -1);
        assertEquals(Polynomial.of(6, -12, 2), p.derivative());
        assertTrue(Polynomial.of(5).derivative().isZero());
        assertEquals(Polynomial.of(2, -6, 3, 0), p.add(Polynomial.of(1, 1)));
        assertTrue(p.add(p.scale(bd("-1"))).isZero());
        assertEquals(Polynomial.of(1, 0, -1), Polynomial.of(1, 1).multiply(Polynomial.of(1, -1)));
        assertTrue(p.multiply(Polynomial.zero()).isZero());
        assertTrue(Polynomial.zero().multiply(p).isZero());
        assertEquals(Polynomial.of(4, -12, 4, -2), p.scale(bd("2")));
        assertEquals(Polynomial.of(2, 6, 2, 1), p.withAbsoluteCoefficients());
        assertEquals(Polynomial.of(1, -3), Polynomial.linearFactor(bd("3")));
    }

    @Test
    @DisplayName("Égalité numérique : 2.0 = 2, hashCode cohérent")
    void numericEquality() {
        Polynomial a = Polynomial.of(bd("2.0"), bd("1.50"));
        Polynomial b = Polynomial.of(bd("2"), bd("1.5"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Polynomial.of(2, 1));
        assertNotEquals(a, Polynomial.of(2));
        assertNotEquals(a, "2x + 1.5");
        assertEquals(a, a);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', textBlock = """
            5          | 5
            2.50       | 2.5
            1000       | 1000
            1E+30      | 1E+30
            1E+20      | 100000000000000000000
            0.000001   | 0.000001
            1E-40      | 1E-40
            -0.00      | 0
            """)
    void formatsNumbersWithoutLosingDigits(String value, String expected) {
        assertEquals(expected, PolynomialFormatter.formatNumber(bd(value)));
    }

    @Test
    @DisplayName("Formules Unicode, ASCII, forme imbriquée et diviseur")
    void formulas() {
        Polynomial p = Polynomial.of(2, -6, 2, -1);
        assertEquals("2x³ − 6x² + 2x − 1", PolynomialFormatter.toUnicode(p));
        assertEquals("((2x − 6)x + 2)x − 1", PolynomialFormatter.nestedForm(p, true));
        assertEquals("((2x - 6)x + 2)x - 1", PolynomialFormatter.nestedForm(p, false));
        assertEquals("(((3x)x)x)x + 2", PolynomialFormatter.nestedForm(Polynomial.of(3, 0, 0, 0, 2), false));
        assertEquals("-x + 1", PolynomialFormatter.nestedForm(Polynomial.of(-1, 1), false));
        assertEquals("7", PolynomialFormatter.nestedForm(Polynomial.of(7), false));
        assertEquals("-x^2 + x", PolynomialFormatter.toAscii(Polynomial.of(-1, 1, 0)));
        assertEquals("(1E+30)x^2", PolynomialFormatter.toAscii(Polynomial.of(bd("1e30"), BigDecimal.ZERO, BigDecimal.ZERO)));
        assertEquals("0", PolynomialFormatter.toUnicode(Polynomial.zero()));
        assertEquals("x¹²", PolynomialFormatter.toUnicode(Polynomial.of(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        assertEquals("[2, -6, 2, -1]", PolynomialFormatter.coefficientList(p));
        assertEquals("(x - 3)", PolynomialFormatter.formatDivisor(bd("3"), false));
        assertEquals("(x + 2)", PolynomialFormatter.formatDivisor(bd("-2"), false));
        assertEquals("x", PolynomialFormatter.formatDivisor(BigDecimal.ZERO, true));
        assertEquals("(-3)", PolynomialFormatter.formatOperand(bd("-3")));
        assertEquals("abc", PolynomialFormatter.abbreviate("abc", 10));
        assertTrue(PolynomialFormatter.abbreviate("x".repeat(200), 50).contains("200 caractères"));
    }

    @Test
    @DisplayName("NumericSettings : modes, bornes et unité d'arrondi")
    void numericSettings() {
        assertTrue(NumericSettings.exact().isExact());
        assertEquals(0, NumericSettings.exact().precision());
        assertEquals(34, NumericSettings.controlled().precision());
        assertNumericEquals("5E-34", NumericSettings.controlled().unitRoundoff());
        assertNumericEquals("0", NumericSettings.exact().unitRoundoff());
        assertTrue(NumericSettings.controlled(10).describe().contains("10 chiffres"));
        assertTrue(NumericSettings.exact().describe().contains("exact"));
        assertThrows(IllegalArgumentException.class, () -> NumericSettings.controlled(0));
        assertThrows(IllegalArgumentException.class, () -> NumericSettings.controlled(1001));
        assertThrows(IllegalArgumentException.class, () -> NumericSettings.exact(0));
        assertThrows(IllegalArgumentException.class,
                () -> new NumericSettings(CalculationMode.EXACT, MathContext.DECIMAL64, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new NumericSettings(CalculationMode.CONTROLLED, MathContext.UNLIMITED, 10));
        assertThrows(NullPointerException.class, () -> new NumericSettings(null, MathContext.UNLIMITED, 10));
    }

    @Test
    @DisplayName("HornerStep : cohérence des champs vérifiée")
    void hornerStepValidation() {
        assertThrows(IllegalArgumentException.class, () -> new HornerStep(0, 0, BigDecimal.ONE, null, null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new HornerStep(1, -1, BigDecimal.ONE, null, null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new HornerStep(2, 0, BigDecimal.ONE, BigDecimal.ONE, null, BigDecimal.ONE));
    }
}
