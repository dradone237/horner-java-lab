package org.hornerlab.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Évaluation d'un polynôme par la méthode de Horner.
 *
 * <p>Pour P(x) = a_n·x^n + … + a_1·x + a_0, stocké sous la forme {@code [a_n, …, a_0]} :
 * <pre>
 *   acc ← a_n
 *   pour i de 1 à n :
 *       acc ← acc × x + a_(n−i)
 *   retourner acc
 * </pre>
 *
 * <p><b>Invariant de boucle :</b> après le tour i,
 * acc = a_n·x^i + a_(n−1)·x^(i−1) + … + a_(n−i), c'est-à-dire la valeur en x du polynôme
 * formé des i+1 premiers coefficients. Au dernier tour (i = n), acc = P(x).
 *
 * <p><b>Coût :</b> exactement n multiplications et n additions, sans aucun calcul de puissance.
 *
 * <p>Cette classe ne réalise aucun affichage et ne dépend d'aucune interface.
 */
public final class HornerEvaluator {

    private HornerEvaluator() {
    }

    /** P(x) en mode exact. */
    public static BigDecimal evaluate(Polynomial polynomial, BigDecimal x) {
        return evaluate(polynomial, x, NumericSettings.exact());
    }

    /** P(x) selon la politique de calcul fournie. */
    public static BigDecimal evaluate(Polynomial polynomial, BigDecimal x, NumericSettings settings) {
        return run(polynomial, x, settings, null);
    }

    /**
     * P(x) en mode exact à partir d'une liste brute de coefficients (puissances décroissantes).
     * La liste est validée et normalisée par {@link Polynomial#of(List)}.
     */
    public static BigDecimal evaluate(List<BigDecimal> coefficients, BigDecimal x) {
        return evaluate(Polynomial.of(coefficients), x);
    }

    /**
     * Interface flottante : les coefficients et x sont convertis exactement en {@link BigDecimal}
     * (via {@link BigDecimal#valueOf(double)}). NaN et ±∞ sont refusés.
     */
    public static BigDecimal evaluate(double[] coefficients, double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new IllegalArgumentException("x vaut " + x + " : NaN et l'infini ne sont pas acceptés.");
        }
        return evaluate(Polynomial.ofDoubles(coefficients), BigDecimal.valueOf(x));
    }

    /** Évaluation détaillée en mode exact. */
    public static HornerResult evaluateWithSteps(Polynomial polynomial, BigDecimal x) {
        return evaluateWithSteps(polynomial, x, NumericSettings.exact());
    }

    /**
     * Évaluation détaillée : chaque étape enregistrée correspond à une opération réellement
     * exécutée par la boucle (la trace et le calcul partagent le même code).
     */
    public static HornerResult evaluateWithSteps(Polynomial polynomial, BigDecimal x, NumericSettings settings) {
        List<HornerStep> trace = new ArrayList<>();
        BigDecimal value = run(polynomial, x, settings, trace);
        int iterations = trace.size() - 1; // chaque itération = 1 multiplication + 1 addition
        return new HornerResult(polynomial, x, value, trace, iterations, iterations, settings);
    }

    /** Boucle de Horner unique ; {@code trace} vaut {@code null} lorsqu'aucune trace n'est demandée. */
    private static BigDecimal run(Polynomial polynomial, BigDecimal x, NumericSettings settings,
                                  List<HornerStep> trace) {
        Objects.requireNonNull(polynomial, "polynomial");
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(settings, "settings");

        List<BigDecimal> a = polynomial.coefficients();
        int n = a.size() - 1;
        BigDecimal acc = a.get(0);
        if (trace != null) {
            trace.add(HornerStep.initialization(n, acc));
        }
        for (int i = 1; i <= n; i++) {
            BigDecimal before = acc;
            BigDecimal product = settings.multiply(before, x);
            acc = settings.add(product, a.get(i));
            settings.checkSize(acc);
            if (trace != null) {
                trace.add(new HornerStep(i + 1, n - i, a.get(i), before, product, acc));
            }
        }
        return acc;
    }
}
