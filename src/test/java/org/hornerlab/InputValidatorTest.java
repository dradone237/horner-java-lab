package org.hornerlab;

import org.hornerlab.core.Polynomial;
import org.hornerlab.validation.InputLimits;
import org.hornerlab.validation.InputValidator;
import org.hornerlab.validation.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hornerlab.HornerEvaluatorTest.assertNumericEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validation et conversion des saisies (boîte noire sur l'interface publique du validateur). */
class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    @ParameterizedTest(name = "« {0} » → {1}")
    @CsvSource(delimiter = '|', textBlock = """
            3          | 3
            -2.5       | -2.5
            +4         | 4
            .5         | 0.5
            5.         | 5
            1e-3       | 0.001
            2.5E+4     | 25000
            '  7  '    | 7
            −6         | -6
            0          | 0
            -0.000     | 0
            """)
    void acceptsValidNumbers(String text, String expected) {
        assertNumericEquals(expected, validator.parseNumber(text, "x"));
    }

    @ParameterizedTest(name = "« {0} » refusé")
    @ValueSource(strings = {"abc", "1,5", "NaN", "nan", "Infinity", "-inf", "∞", "1e", "--1", "1.2.3", "0x10",
            "1_000", "1 000", "e5", "+", ".", "1e5000", "1e-1001", "12a"})
    void rejectsInvalidNumbers(String text) {
        InvalidInputException e = assertThrows(InvalidInputException.class, () -> validator.parseNumber(text, "x"));
        assertEquals("x", e.field());
        assertTrue(e.getMessage().startsWith("x : "), e.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsMissingNumbers(String text) {
        assertThrows(InvalidInputException.class, () -> validator.parseNumber(text, "x"));
    }

    @Test
    @DisplayName("La virgule décimale est signalée explicitement")
    void decimalCommaHasHelpfulMessage() {
        InvalidInputException e = assertThrows(InvalidInputException.class, () -> validator.parseNumber("1,5", "x"));
        assertTrue(e.getMessage().contains("point"), e.getMessage());
    }

    @Test
    @DisplayName("Nombre trop long refusé")
    void tooLongNumberRejected() {
        String longNumber = "1".repeat(201);
        assertThrows(InvalidInputException.class, () -> validator.parseNumber(longNumber, "x"));
    }

    @ParameterizedTest(name = "« {0} » → [{1}]")
    @CsvSource(delimiter = '|', textBlock = """
            2,-6,2,-1            | 2;-6;2;-1
            ' 2 , -6 , 2 , -1 '  | 2;-6;2;-1
            [2, -6, 2, -1]       | 2;-6;2;-1
            1.5,-2.25,0,3        | 1.5;-2.25;0;3
            1e3,-2E-2            | 1000;-0.02
            7                    | 7
            """)
    void parsesCoefficientLists(String text, String expected) {
        List<String> exp = Arrays.asList(expected.split(";"));
        var actual = validator.parseCoefficients(text);
        assertEquals(exp.size(), actual.size());
        for (int i = 0; i < exp.size(); i++) {
            assertNumericEquals(exp.get(i), actual.get(i));
        }
    }

    @ParameterizedTest(name = "liste « {0} » refusée")
    @ValueSource(strings = {"", "  ", "[]", "1,,2", ",1", "1,", "1;2", "1 2 3", "a,b", "2,-6,x,1", "1,NaN"})
    void rejectsInvalidLists(String text) {
        assertThrows(InvalidInputException.class, () -> validator.parseCoefficients(text));
    }

    @Test
    @DisplayName("Liste absente (null) refusée")
    void rejectsNullList() {
        assertThrows(InvalidInputException.class, () -> validator.parseCoefficients(null));
    }

    @Test
    @DisplayName("Messages ciblés : coefficient fautif, séparateur, espace")
    void listMessagesAreSpecific() {
        assertEquals("coefficient n°2", assertThrows(InvalidInputException.class,
                () -> validator.parseCoefficients("1,,2")).field());
        assertTrue(assertThrows(InvalidInputException.class, () -> validator.parseCoefficients("1;2"))
                .getMessage().contains("virgule"));
        assertTrue(assertThrows(InvalidInputException.class, () -> validator.parseCoefficients("1 2"))
                .getMessage().contains("espace"));
    }

    @Test
    @DisplayName("Nombre maximal de coefficients : limite atteinte acceptée, dépassée refusée")
    void enforcesMaximumCoefficientCount() {
        InputValidator small = new InputValidator(new InputLimits(4, 50, 100, 1000));
        assertEquals(4, small.parseCoefficients("1,2,3,4").size());
        InvalidInputException e = assertThrows(InvalidInputException.class, () -> small.parseCoefficients("1,2,3,4,5"));
        assertTrue(e.getMessage().contains("trop de coefficients"));
        assertThrows(InvalidInputException.class, () -> small.parseCoefficients("1".repeat(1001)));
        assertEquals(10_001, validator.parseCoefficients(String.join(",", Collections.nCopies(10_001, "1"))).size());
        assertThrows(InvalidInputException.class,
                () -> validator.parseCoefficients(String.join(",", Collections.nCopies(10_002, "1"))));
    }

    @Test
    @DisplayName("Zéros dominants normalisés, polynôme nul conservé")
    void parsePolynomialNormalizes() {
        assertEquals(Polynomial.of(2, 1), validator.parsePolynomial("0,0,2,1"));
        assertTrue(validator.parsePolynomial("0,0,0").isZero());
    }

    @Test
    @DisplayName("Champs de la GUI : nommés d'après leur puissance")
    void polynomialFromFields() {
        assertEquals(Polynomial.of(2, -6, 2, -1), validator.polynomialFromFields(List.of("2", "-6", "2", "-1")));
        // [1, x, 3] : a2 = 1, a1 = « x » (fautif), a0 = 3
        assertEquals("coefficient a1", assertThrows(InvalidInputException.class,
                () -> validator.polynomialFromFields(List.of("1", "x", "3"))).field());
        assertThrows(InvalidInputException.class, () -> validator.polynomialFromFields(List.of()));
        assertThrows(InvalidInputException.class, () -> validator.polynomialFromFields(null));
        InputValidator gui = new InputValidator(InputLimits.GUI);
        assertThrows(InvalidInputException.class, () -> gui.polynomialFromFields(Collections.nCopies(32, "1")));
    }

    @Test
    @DisplayName("Entiers bornés, listes d'entiers et flottants")
    void integersAndDoubles() {
        assertEquals(34, validator.parseInteger("34", "precision", 1, 1000));
        assertEquals(-5L, validator.parseLong("−5", "seed", -10, 10));
        assertThrows(InvalidInputException.class, () -> validator.parseInteger("0", "precision", 1, 1000));
        assertThrows(InvalidInputException.class, () -> validator.parseInteger("3.5", "precision", 1, 1000));
        assertThrows(InvalidInputException.class, () -> validator.parseInteger("", "precision", 1, 1000));
        assertThrows(InvalidInputException.class, () -> validator.parseLong("99999999999999999999", "seed", Long.MIN_VALUE, Long.MAX_VALUE));
        assertEquals(List.of(10, 100), validator.parseIntegerList("10,100", "sizes", 1, 1000));
        assertThrows(InvalidInputException.class, () -> validator.parseIntegerList(" ", "sizes", 1, 1000));
        assertNumericEquals("0.1", validator.fromDouble(0.1, "x"));
        assertThrows(InvalidInputException.class, () -> validator.fromDouble(Double.NaN, "x"));
        assertThrows(InvalidInputException.class, () -> validator.fromDouble(Double.NEGATIVE_INFINITY, "x"));
        assertThrows(IllegalArgumentException.class, () -> new InputLimits(0, 1, 1, 1));
    }
}
