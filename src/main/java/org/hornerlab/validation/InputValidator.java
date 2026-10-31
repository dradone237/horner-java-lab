package org.hornerlab.validation;

import org.hornerlab.core.Polynomial;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Conversion et validation des saisies textuelles (CLI et GUI).
 *
 * <p>Syntaxe acceptée pour un nombre : signe optionnel, chiffres, point décimal optionnel,
 * exposant optionnel ({@code 3}, {@code -2.5}, {@code +4}, {@code .5}, {@code 1e-3}, {@code 2.5E+4}).
 * Le signe moins typographique « − » (U+2212) est accepté comme « - ».
 * La virgule n'est <b>jamais</b> un séparateur décimal : elle sépare les coefficients.
 *
 * <p>Aucune valeur invalide n'est convertie silencieusement en zéro : toute saisie refusée
 * provoque une {@link InvalidInputException} explicite.
 */
public final class InputValidator {

    private static final Pattern NUMBER = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");

    private final InputLimits limits;

    public InputValidator() {
        this(InputLimits.DEFAULT);
    }

    public InputValidator(InputLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public InputLimits limits() {
        return limits;
    }

    /** Convertit un nombre décimal saisi en {@link BigDecimal}, ou lève une exception explicite. */
    public BigDecimal parseNumber(String text, String field) {
        if (text == null) {
            throw new InvalidInputException(field, "valeur absente.");
        }
        String t = text.strip().replace('−', '-');
        if (t.isEmpty()) {
            throw new InvalidInputException(field, "valeur vide.");
        }
        if (t.length() > limits.maxNumberLength()) {
            throw new InvalidInputException(field, "valeur trop longue (" + t.length()
                    + " caractères, maximum " + limits.maxNumberLength() + ").");
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.contains("nan")) {
            throw new InvalidInputException(field, "NaN (« pas un nombre ») n'est pas une valeur acceptée.");
        }
        if (lower.contains("inf") || t.contains("∞")) {
            throw new InvalidInputException(field, "l'infini n'est pas une valeur acceptée.");
        }
        if (t.indexOf(',') >= 0) {
            throw new InvalidInputException(field, "« " + t + " » contient une virgule : "
                    + "utilisez le point comme séparateur décimal (exemple : 1.5).");
        }
        if (!NUMBER.matcher(t).matches()) {
            throw new InvalidInputException(field, "« " + t + " » n'est pas un nombre valide "
                    + "(exemples acceptés : 3, -2.5, 1e-3, 2.5E+4).");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(field, "« " + t + " » : exposant hors des limites représentables.");
        }
        if (Math.abs((long) value.scale()) > limits.maxScaleMagnitude()) {
            throw new InvalidInputException(field, "ordre de grandeur hors limites : l'exposant décimal doit rester entre −"
                    + limits.maxScaleMagnitude() + " et +" + limits.maxScaleMagnitude() + ".");
        }
        return value;
    }

    /**
     * Convertit une liste « 2,-6,2,-1 » (crochets facultatifs) en coefficients,
     * en puissances décroissantes.
     */
    public List<BigDecimal> parseCoefficients(String text) {
        String field = "coefficients";
        if (text == null) {
            throw new InvalidInputException(field, "liste absente.");
        }
        String t = text.strip();
        if (t.length() > limits.maxInputLength()) {
            throw new InvalidInputException(field, "saisie trop longue (" + t.length()
                    + " caractères, maximum " + limits.maxInputLength() + ").");
        }
        if (t.startsWith("[") && t.endsWith("]")) {
            t = t.substring(1, t.length() - 1).strip();
        }
        if (t.isEmpty()) {
            throw new InvalidInputException(field, "liste vide : saisissez par exemple 2,-6,2,-1.");
        }
        if (t.indexOf(';') >= 0) {
            throw new InvalidInputException(field,
                    "séparateur « ; » incorrect : séparez les coefficients par des virgules (exemple : 2,-6,2,-1).");
        }
        String[] parts = t.split(",", -1);
        if (parts.length > limits.maxCoefficients()) {
            throw new InvalidInputException(field, "trop de coefficients (" + parts.length + ", maximum "
                    + limits.maxCoefficients() + ", soit un degré au plus égal à " + (limits.maxCoefficients() - 1) + ").");
        }
        List<BigDecimal> result = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].strip();
            String partField = "coefficient n°" + (i + 1);
            if (part.isEmpty()) {
                throw new InvalidInputException(partField,
                        "valeur vide (deux virgules consécutives, ou virgule en début ou en fin de liste ?).");
            }
            if (part.chars().anyMatch(Character::isWhitespace)) {
                throw new InvalidInputException(partField, "« " + part + " » contient un espace : "
                        + "séparez les coefficients par des virgules (exemple : 2,-6,2,-1).");
            }
            result.add(parseNumber(part, partField));
        }
        return result;
    }

    /** Liste de coefficients → {@link Polynomial} (zéros dominants normalisés). */
    public Polynomial parsePolynomial(String text) {
        return Polynomial.of(parseCoefficients(text));
    }

    /**
     * Champs de saisie individuels (GUI), du coefficient dominant au terme constant.
     * Chaque champ est nommé d'après sa puissance (a3, a2, a1, a0).
     */
    public Polynomial polynomialFromFields(List<String> fieldTexts) {
        if (fieldTexts == null || fieldTexts.isEmpty()) {
            throw new InvalidInputException("coefficients", "aucun coefficient saisi.");
        }
        if (fieldTexts.size() > limits.maxCoefficients()) {
            throw new InvalidInputException("coefficients", "trop de coefficients (maximum "
                    + limits.maxCoefficients() + ").");
        }
        int n = fieldTexts.size() - 1;
        List<BigDecimal> values = new ArrayList<>(fieldTexts.size());
        for (int i = 0; i <= n; i++) {
            values.add(parseNumber(fieldTexts.get(i), "coefficient a" + (n - i)));
        }
        return Polynomial.of(values);
    }

    /** Entier borné (précision, nombre de cas, répétitions…). */
    public int parseInteger(String text, String field, int min, int max) {
        long value = parseLong(text, field, min, max);
        return (int) value;
    }

    /** Entier long borné (graine aléatoire…). */
    public long parseLong(String text, String field, long min, long max) {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException(field, "valeur vide.");
        }
        String t = text.strip().replace('−', '-');
        if (!t.matches("[+-]?\\d{1,19}")) {
            throw new InvalidInputException(field, "« " + t + " » n'est pas un entier valide.");
        }
        long value;
        try {
            value = Long.parseLong(t);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(field, "« " + t + " » dépasse la capacité d'un entier.");
        }
        if (value < min || value > max) {
            throw new InvalidInputException(field, "doit être compris entre " + min + " et " + max + ".");
        }
        return value;
    }

    /** Liste d'entiers bornés séparés par des virgules (par exemple les tailles d'un benchmark). */
    public List<Integer> parseIntegerList(String text, String field, int min, int max) {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException(field, "liste vide.");
        }
        List<Integer> values = new ArrayList<>();
        for (String part : text.split(",", -1)) {
            values.add(parseInteger(part, field, min, max));
        }
        return values;
    }

    /** Conversion d'un flottant : NaN et ±∞ refusés, valeur décimale la plus courte conservée. */
    public BigDecimal fromDouble(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new InvalidInputException(field, value + " n'est pas une valeur finie (NaN et l'infini sont refusés).");
        }
        return BigDecimal.valueOf(value);
    }
}
