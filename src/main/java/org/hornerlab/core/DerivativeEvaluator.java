package org.hornerlab.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Horner étendu : calcule P(x) et P'(x) en un seul parcours des coefficients.
 *
 * <pre>
 *   b ← a_n ; d ← 0
 *   pour chaque coefficient suivant a :
 *       d ← d × x + b      (utilise l'ANCIENNE valeur de b)
 *       b ← b × x + a
 *   à la fin : b = P(x), d = P'(x)
 * </pre>
 *
 * <p><b>Invariant :</b> si b_i est la valeur en x du polynôme Q_i formé des i+1 premiers
 * coefficients, alors d_i = Q_i'(x). En effet Q_i(t) = Q_(i−1)(t)·t + a, donc
 * Q_i'(t) = Q_(i−1)'(t)·t + Q_(i−1)(t), ce qui est exactement la mise à jour de d.
 * L'ordre des deux mises à jour est essentiel : mettre à jour b d'abord donnerait un résultat faux.
 */
public final class DerivativeEvaluator {

    private DerivativeEvaluator() {
    }

    /** P(x) et P'(x) en mode exact. */
    public static DerivativeResult evaluate(Polynomial polynomial, BigDecimal x) {
        return evaluate(polynomial, x, NumericSettings.exact());
    }

    /** P(x) et P'(x) selon la politique de calcul fournie. */
    public static DerivativeResult evaluate(Polynomial polynomial, BigDecimal x, NumericSettings settings) {
        Objects.requireNonNull(polynomial, "polynomial");
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(settings, "settings");

        List<BigDecimal> a = polynomial.coefficients();
        BigDecimal b = a.get(0);
        BigDecimal d = BigDecimal.ZERO;
        List<DerivativeStep> steps = new ArrayList<>(a.size());
        steps.add(new DerivativeStep(1, a.get(0), null, null, d, b));
        int multiplications = 0;
        int additions = 0;
        for (int i = 1; i < a.size(); i++) {
            BigDecimal bBefore = b;
            BigDecimal dBefore = d;
            d = settings.add(settings.multiply(dBefore, x), bBefore);
            b = settings.add(settings.multiply(bBefore, x), a.get(i));
            multiplications += 2;
            additions += 2;
            settings.checkSize(b);
            settings.checkSize(d);
            steps.add(new DerivativeStep(i + 1, a.get(i), bBefore, dBefore, d, b));
        }
        return new DerivativeResult(polynomial, x, b, d, steps, multiplications, additions, settings);
    }
}
