package org.hornerlab.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Résultat de la division synthétique de P(x) par (x − r).
 *
 * @param dividend     P(x)
 * @param root         r
 * @param quotient     Q(x) ; polynôme nul {@code [0]} si P est constant
 * @param remainder    R (égal à P(r))
 * @param hornerScheme schéma de Horner évalué en r (accumulateurs = coefficients de Q puis R)
 * @param settings     politique de calcul utilisée
 */
public record DivisionResult(Polynomial dividend, BigDecimal root, Polynomial quotient, BigDecimal remainder,
                             HornerResult hornerScheme, NumericSettings settings) {

    public DivisionResult {
        Objects.requireNonNull(dividend, "dividend");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(quotient, "quotient");
        Objects.requireNonNull(remainder, "remainder");
        Objects.requireNonNull(hornerScheme, "hornerScheme");
        Objects.requireNonNull(settings, "settings");
    }

    /** Le diviseur (x − r). */
    public Polynomial divisor() {
        return Polynomial.linearFactor(root);
    }

    public List<BigDecimal> quotientCoefficients() {
        return quotient.coefficients();
    }

    /** (x − r)·Q(x) + R, calculé par arithmétique polynomiale exacte (indépendante de Horner). */
    public Polynomial reconstruct() {
        return divisor().multiply(quotient).add(Polynomial.constant(remainder));
    }

    /** Vrai si l'identité P(x) = (x − r)·Q(x) + R est vérifiée exactement. */
    public boolean identityHolds() {
        return reconstruct().equals(dividend);
    }

    /** Plus grand écart absolu, coefficient par coefficient, entre (x − r)·Q(x) + R et P(x). */
    public BigDecimal identityMaxDeviation() {
        Polynomial rebuilt = reconstruct();
        int size = Math.max(rebuilt.size(), dividend.size());
        BigDecimal max = BigDecimal.ZERO;
        for (int power = 0; power < size; power++) {
            BigDecimal gap = rebuilt.coefficientOfPower(power).subtract(dividend.coefficientOfPower(power)).abs();
            max = max.max(gap);
        }
        return max;
    }

    /**
     * Statut de r vis-à-vis de P.
     * <ul>
     *   <li>Mode exact : {@link RootStatus#EXACT_ROOT} si R = 0 exactement, sinon
     *       {@link RootStatus#NOT_A_ROOT} — même si R est minuscule (la tolérance est ignorée).</li>
     *   <li>Mode contrôlé : un arrondi a pu se produire, la racine ne peut donc pas être certifiée ;
     *       {@link RootStatus#APPROXIMATE_ROOT} si |R| ≤ tolérance, sinon {@link RootStatus#NOT_A_ROOT}.</li>
     * </ul>
     */
    public RootStatus rootStatus(BigDecimal tolerance) {
        if (settings.isExact()) {
            return remainder.signum() == 0 ? RootStatus.EXACT_ROOT : RootStatus.NOT_A_ROOT;
        }
        Objects.requireNonNull(tolerance, "tolerance");
        if (tolerance.signum() < 0) {
            throw new IllegalArgumentException("La tolérance doit être positive ou nulle.");
        }
        return remainder.abs().compareTo(tolerance) <= 0 ? RootStatus.APPROXIMATE_ROOT : RootStatus.NOT_A_ROOT;
    }
}
