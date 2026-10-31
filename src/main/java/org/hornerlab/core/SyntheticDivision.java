package org.hornerlab.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Division synthétique de P(x) par (x − r).
 *
 * <p>Le schéma est exactement celui de Horner évalué en r : les accumulateurs successifs
 * b_0 = a_n, b_i = b_(i−1)·r + a_(n−i) sont les coefficients du quotient Q(x), et le dernier
 * accumulateur est le reste R. On a donc P(x) = (x − r)·Q(x) + R et R = P(r).
 *
 * <p><b>Polynôme constant :</b> P(x) = c n'est pas divisible par un polynôme de degré 1 ;
 * le quotient est le polynôme nul (représenté par {@code [0]}) et le reste vaut c.
 */
public final class SyntheticDivision {

    private SyntheticDivision() {
    }

    /** Division exacte de P par (x − r). */
    public static DivisionResult divide(Polynomial polynomial, BigDecimal root) {
        return divide(polynomial, root, NumericSettings.exact());
    }

    /** Division de P par (x − r) selon la politique de calcul fournie. */
    public static DivisionResult divide(Polynomial polynomial, BigDecimal root, NumericSettings settings) {
        Objects.requireNonNull(polynomial, "polynomial");
        Objects.requireNonNull(root, "root");
        HornerResult scheme = HornerEvaluator.evaluateWithSteps(polynomial, root, settings);

        if (polynomial.isConstant()) {
            return new DivisionResult(polynomial, root, Polynomial.zero(), polynomial.constantTerm(), scheme, settings);
        }
        List<HornerStep> steps = scheme.steps();
        List<BigDecimal> quotient = new ArrayList<>(steps.size() - 1);
        for (int i = 0; i < steps.size() - 1; i++) {
            quotient.add(steps.get(i).accumulatorAfter());
        }
        BigDecimal remainder = steps.get(steps.size() - 1).accumulatorAfter();
        return new DivisionResult(polynomial, root, Polynomial.of(quotient), remainder, scheme, settings);
    }
}
