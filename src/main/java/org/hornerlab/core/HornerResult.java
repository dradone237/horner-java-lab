package org.hornerlab.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Résultat détaillé d'une évaluation de Horner.
 *
 * @param polynomial      polynôme évalué (coefficients en puissances décroissantes)
 * @param x               point d'évaluation
 * @param value           P(x)
 * @param steps           étapes exécutées, dans l'ordre (la première est l'initialisation)
 * @param multiplications nombre de multiplications réellement effectuées
 * @param additions       nombre d'additions réellement effectuées
 * @param settings        politique de calcul utilisée
 */
public record HornerResult(Polynomial polynomial, BigDecimal x, BigDecimal value, List<HornerStep> steps,
                           int multiplications, int additions, NumericSettings settings) {

    public HornerResult {
        Objects.requireNonNull(polynomial, "polynomial");
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(settings, "settings");
        steps = List.copyOf(steps);
    }

    /** Coefficients évalués, en puissances décroissantes. */
    public List<BigDecimal> coefficients() {
        return polynomial.coefficients();
    }

    /** Nombre de tours de boucle (= degré, 0 pour un polynôme constant). */
    public int iterations() {
        return steps.size() - 1;
    }

    /** Ordre exact des opérations, par exemple {@code ["× 3", "+ (-6)", "× 3", "+ 2", …]}. */
    public List<String> operationSequence() {
        List<String> sequence = new ArrayList<>(2 * iterations());
        String xText = PolynomialFormatter.formatNumber(x);
        for (HornerStep step : steps) {
            if (!step.isInitialization()) {
                sequence.add("× " + (x.signum() < 0 ? "(" + xText + ")" : xText));
                String c = PolynomialFormatter.formatNumber(step.coefficient());
                sequence.add("+ " + (step.coefficient().signum() < 0 ? "(" + c + ")" : c));
            }
        }
        return sequence;
    }
}
