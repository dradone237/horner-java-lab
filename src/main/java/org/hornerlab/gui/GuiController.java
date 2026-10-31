package org.hornerlab.gui;

import org.hornerlab.core.DerivativeEvaluator;
import org.hornerlab.core.DerivativeResult;
import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.HornerResult;
import org.hornerlab.core.HornerStep;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.PolynomialFormatter;
import org.hornerlab.core.RootStatus;
import org.hornerlab.core.SyntheticDivision;
import org.hornerlab.validation.InputLimits;
import org.hornerlab.validation.InputValidator;
import org.hornerlab.validation.ResultVerifier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static org.hornerlab.core.PolynomialFormatter.formatNumber;
import static org.hornerlab.core.PolynomialFormatter.formatOperand;

/**
 * Logique de l'interface graphique, indépendante de Swing (donc testable sans affichage).
 * Tous les résultats proviennent du moteur réel ({@code org.hornerlab.core}).
 */
public final class GuiController {

    /** Résultat d'un calcul demandé depuis l'interface. */
    public record Computation(Polynomial polynomial, BigDecimal x, NumericSettings settings, HornerResult horner,
                              DerivativeResult derivative, DivisionResult divisionAtX, RootStatus rootStatus,
                              long elapsedNanos) {
    }

    private final InputValidator validator = new InputValidator(InputLimits.GUI);
    private final List<Consumer<Computation>> listeners = new ArrayList<>();
    private Computation current;

    public InputValidator validator() {
        return validator;
    }

    public void addListener(Consumer<Computation> listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public Optional<Computation> current() {
        return Optional.ofNullable(current);
    }

    /** Valide la saisie, exécute Horner (+ dérivée, + division en r = x) et prévient les écrans. */
    public Computation compute(List<String> coefficientTexts, String xText, NumericSettings settings) {
        Polynomial p = validator.polynomialFromFields(coefficientTexts);
        BigDecimal x = validator.parseNumber(xText, "x");
        long t0 = System.nanoTime();
        HornerEvaluator.evaluate(p, x, settings);
        long elapsed = System.nanoTime() - t0;
        HornerResult horner = HornerEvaluator.evaluateWithSteps(p, x, settings);
        DerivativeResult derivative = DerivativeEvaluator.evaluate(p, x, settings);
        DivisionResult division = SyntheticDivision.divide(p, x, settings);
        current = new Computation(p, x, settings, horner, derivative, division,
                ResultVerifier.classifyRoot(division), elapsed);
        listeners.forEach(l -> l.accept(current));
        return current;
    }

    /** Division synthétique du polynôme courant par (x − r). */
    public DivisionResult divide(String rootText) {
        Computation c = requireCurrent();
        return SyntheticDivision.divide(c.polynomial(), validator.parseNumber(rootText, "r"), c.settings());
    }

    /** Contrôles Horner / oracle pour le calcul courant. */
    public List<ResultVerifier.Check> verify() {
        Computation c = requireCurrent();
        return ResultVerifier.verifyAll(c.polynomial(), c.x(), c.settings());
    }

    private Computation requireCurrent() {
        if (current == null) {
            throw new IllegalStateException("Aucun calcul : saisissez un polynôme et cliquez sur « Calculer ».");
        }
        return current;
    }

    /** Formule affichée pendant la saisie, ou vide si un champ est invalide. */
    public Optional<String> formulaPreview(List<String> coefficientTexts) {
        try {
            return Optional.of("P(x) = " + PolynomialFormatter.toUnicode(validator.polynomialFromFields(coefficientTexts)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Lignes du tableau des étapes : Étape | Coefficient | Acc. avant | Multiplication | Addition | Acc. après. */
    public static List<String[]> stepRows(HornerResult result) {
        List<String[]> rows = new ArrayList<>();
        String x = formatOperand(result.x());
        for (HornerStep s : result.steps()) {
            if (s.isInitialization()) {
                rows.add(new String[] {String.valueOf(s.number()), formatNumber(s.coefficient()), "—",
                        "—", "—", formatNumber(s.accumulatorAfter())});
            } else {
                rows.add(new String[] {String.valueOf(s.number()), formatNumber(s.coefficient()),
                        formatNumber(s.accumulatorBefore()),
                        formatOperand(s.accumulatorBefore()) + " × " + x + " = " + formatNumber(s.product()),
                        formatOperand(s.product()) + " + " + formatOperand(s.coefficient()) + " = " + formatNumber(s.accumulatorAfter()),
                        formatNumber(s.accumulatorAfter())});
            }
        }
        return rows;
    }

    /** Explication en une phrase d'une étape (affichée sous le tableau). */
    public static String explainStep(HornerResult result, int index) {
        HornerStep s = result.steps().get(index);
        if (s.isInitialization()) {
            return "Étape " + s.number() + " : on part du coefficient dominant a" + s.power() + " = "
                    + formatNumber(s.coefficient()) + ".";
        }
        return "Étape " + s.number() + " : on multiplie l'accumulateur " + formatNumber(s.accumulatorBefore())
                + " par x = " + formatNumber(result.x()) + " (" + formatNumber(s.product()) + "), puis on ajoute a"
                + s.power() + " = " + formatNumber(s.coefficient()) + " → " + formatNumber(s.accumulatorAfter()) + ".";
    }

    /** Schéma de Horner de la division : 3 lignes (coefficients, produits r·b, sommes b). */
    public static List<String[]> divisionScheme(DivisionResult d) {
        List<HornerStep> steps = d.hornerScheme().steps();
        String[] coefficients = new String[steps.size() + 1];
        String[] products = new String[steps.size() + 1];
        String[] sums = new String[steps.size() + 1];
        coefficients[0] = "Coefficients de P";
        products[0] = "× r = " + formatNumber(d.root());
        sums[0] = "Sommes (Q puis R)";
        for (int i = 0; i < steps.size(); i++) {
            HornerStep s = steps.get(i);
            coefficients[i + 1] = formatNumber(s.coefficient());
            products[i + 1] = s.isInitialization() ? "" : formatNumber(s.product());
            sums[i + 1] = formatNumber(s.accumulatorAfter()) + (i == steps.size() - 1 ? "  = R" : "");
        }
        return List.of(coefficients, products, sums);
    }

    /** Libellé d'un champ de coefficient : « a₃ (x³) ». */
    public static String fieldLabel(int power) {
        String sub = Integer.toString(power).chars().mapToObj(ch -> String.valueOf((char) ('₀' + ch - '0')))
                .reduce("", String::concat);
        return switch (power) {
            case 0 -> "a₀ (constante)";
            case 1 -> "a₁ (x)";
            default -> "a" + sub + " (x" + PolynomialFormatter.superscript(power) + ")";
        };
    }
}
