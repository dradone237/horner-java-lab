package org.hornerlab.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mise en forme textuelle des nombres et des polynômes.
 *
 * <p>Les nombres sont affichés <b>sans perte de chiffres</b> : notation décimale habituelle,
 * ou notation scientifique exacte (par exemple {@code 1E-40}) pour les très grands ordres de
 * grandeur, afin d'éviter des chaînes démesurées.
 */
public final class PolynomialFormatter {

    private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";
    private static final String UNICODE_MINUS = "−";

    private PolynomialFormatter() {
    }

    /** Nombre sans zéros inutiles ; notation scientifique exacte pour les ordres de grandeur extrêmes. */
    public static String formatNumber(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        BigDecimal v = value.stripTrailingZeros();
        if (v.signum() == 0) {
            return "0";
        }
        int scale = v.scale();
        if (scale <= 0) {
            return -scale <= 20 ? v.toPlainString() : v.toString();
        }
        long adjustedExponent = (long) v.precision() - scale - 1;
        return adjustedExponent >= -10 ? v.toPlainString() : v.toString();
    }

    /** Liste de coefficients au format {@code [2, -6, 2, -1]}. */
    public static String formatList(List<BigDecimal> values) {
        return values.stream().map(PolynomialFormatter::formatNumber).collect(Collectors.joining(", ", "[", "]"));
    }

    /** Coefficients du polynôme au format {@code [2, -6, 2, -1]}. */
    public static String coefficientList(Polynomial p) {
        return formatList(p.coefficients());
    }

    /** Écriture mathématique avec exposants Unicode : 2x³ − 6x² + 2x − 1. */
    public static String toUnicode(Polynomial p) {
        return format(p, true);
    }

    /** Écriture ASCII (adaptée aux consoles) : 2x^3 - 6x^2 + 2x - 1. */
    public static String toAscii(Polynomial p) {
        return format(p, false);
    }

    private static String format(Polynomial p, boolean unicode) {
        if (p.isZero()) {
            return "0";
        }
        String minus = unicode ? UNICODE_MINUS : "-";
        StringBuilder sb = new StringBuilder();
        List<BigDecimal> c = p.coefficients();
        int n = c.size() - 1;
        for (int i = 0; i <= n; i++) {
            BigDecimal a = c.get(i);
            if (a.signum() == 0) {
                continue;
            }
            int power = n - i;
            boolean negative = a.signum() < 0;
            if (sb.isEmpty()) {
                if (negative) {
                    sb.append(minus);
                }
            } else {
                sb.append(negative ? " " + minus + " " : " + ");
            }
            BigDecimal abs = a.abs();
            String number = formatNumber(abs);
            if (power == 0) {
                sb.append(number);
            } else {
                if (abs.compareTo(BigDecimal.ONE) != 0) {
                    sb.append(number.contains("E") ? "(" + number + ")" : number);
                }
                sb.append('x');
                if (power > 1) {
                    sb.append(unicode ? superscript(power) : "^" + power);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Forme imbriquée de Horner, par exemple ((2x − 6)x + 2)x − 1 pour [2, −6, 2, −1].
     * Elle montre directement l'enchaînement « × x puis + coefficient ».
     */
    public static String nestedForm(Polynomial p, boolean unicode) {
        String minus = unicode ? UNICODE_MINUS : "-";
        List<BigDecimal> c = p.coefficients();
        BigDecimal leading = c.get(0);
        String expr = (leading.signum() < 0 ? minus : "") + formatNumber(leading.abs());
        for (int i = 1; i < c.size(); i++) {
            String base;
            if (i == 1) {
                boolean one = leading.abs().compareTo(BigDecimal.ONE) == 0;
                base = (leading.signum() < 0 ? minus : "") + (one ? "" : formatNumber(leading.abs())) + "x";
            } else {
                base = "(" + expr + ")x";
            }
            BigDecimal a = c.get(i);
            if (a.signum() == 0) {
                expr = base;
            } else {
                expr = base + (a.signum() < 0 ? " " + minus + " " : " + ") + formatNumber(a.abs());
            }
        }
        return expr;
    }

    /** Facteur linéaire (x − r) : « (x - 3) », « (x + 2) » ou « x » si r = 0. */
    public static String formatDivisor(BigDecimal root, boolean unicode) {
        if (root.signum() == 0) {
            return "x";
        }
        String minus = unicode ? UNICODE_MINUS : "-";
        return root.signum() > 0
                ? "(x " + minus + " " + formatNumber(root) + ")"
                : "(x + " + formatNumber(root.abs()) + ")";
    }

    /** Nombre entre parenthèses s'il est négatif : utile pour écrire « 2 × (-3) ». */
    public static String formatOperand(BigDecimal value) {
        String text = formatNumber(value);
        return value.signum() < 0 ? "(" + text + ")" : text;
    }

    /** Exposant en caractères Unicode, par exemple 12 → ¹². */
    public static String superscript(int power) {
        StringBuilder sb = new StringBuilder();
        for (char digit : Integer.toString(power).toCharArray()) {
            sb.append(SUPERSCRIPTS.charAt(digit - '0'));
        }
        return sb.toString();
    }

    /** Raccourcit un texte très long pour l'affichage, en indiquant sa longueur réelle. */
    public static String abbreviate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        int keep = Math.max(10, maxLength - 30);
        return text.substring(0, keep) + "… (" + text.length() + " caractères au total)";
    }
}
