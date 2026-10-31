package org.hornerlab.validation;

import org.hornerlab.core.DerivativeEvaluator;
import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.HornerStep;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.RootStatus;
import org.hornerlab.core.SyntheticDivision;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compare les résultats de Horner à l'oracle indépendant ({@link ReferenceEvaluator}).
 *
 * <p><b>Stratégie de comparaison :</b>
 * <ul>
 *   <li>Mode exact : égalité numérique stricte ({@code compareTo == 0}), tolérance nulle.</li>
 *   <li>Mode contrôlé (précision p) : la référence est calculée avec 2p + 10 chiffres
 *       (plus les chiffres des données) ; la tolérance est la borne d'erreur a priori de Horner
 *       (Higham, <i>Accuracy and Stability of Numerical Algorithms</i>, §5.1) :
 *       |ĥ − P(x)| ≤ γ(2n) · Σ|a_i|·|x|^i, avec γ(k) = k·u / (1 − k·u) et u = ½·10^(1−p),
 *       multipliée par 2 par sécurité.</li>
 * </ul>
 * Toute différence au-delà de la tolérance est signalée comme {@link Verdict#MISMATCH}.
 */
public final class ResultVerifier {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final MathContext BOUND_CONTEXT = new MathContext(10, RoundingMode.UP);

    private ResultVerifier() {
    }

    /** Verdict d'une comparaison. */
    public enum Verdict {
        IDENTICAL("Identique"),
        WITHIN_TOLERANCE("Proche sous tolérance"),
        MISMATCH("Incohérent");

        private final String label;

        Verdict(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * Résultat d'un contrôle.
     *
     * @param name       intitulé du contrôle
     * @param computed   valeur calculée par l'algorithme vérifié
     * @param reference  valeur de référence (oracle indépendant)
     * @param difference |computed − reference|
     * @param tolerance  écart maximal admis (0 en mode exact)
     * @param verdict    conclusion
     * @param method     méthode de référence utilisée
     */
    public record Check(String name, BigDecimal computed, BigDecimal reference, BigDecimal difference,
                        BigDecimal tolerance, Verdict verdict, String method) {

        public boolean passed() {
            return verdict != Verdict.MISMATCH;
        }
    }

    /** Compare deux valeurs : identiques, proches sous tolérance ou incohérentes. */
    public static Check compare(String name, BigDecimal computed, BigDecimal reference, BigDecimal tolerance,
                                String method) {
        Objects.requireNonNull(computed, "computed");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(tolerance, "tolerance");
        BigDecimal difference = computed.subtract(reference).abs();
        Verdict verdict;
        if (difference.signum() == 0) {
            verdict = Verdict.IDENTICAL;
        } else if (difference.compareTo(tolerance) <= 0) {
            verdict = Verdict.WITHIN_TOLERANCE;
        } else {
            verdict = Verdict.MISMATCH;
        }
        return new Check(name, computed, reference, difference, tolerance, verdict, method);
    }

    /** P(x) par Horner comparé à la somme des termes a_i·x^i. */
    public static Check verifyEvaluation(Polynomial p, BigDecimal x, NumericSettings settings) {
        BigDecimal horner = HornerEvaluator.evaluate(p, x, settings);
        String name = "P(x) : Horner vs somme des termes";
        if (settings.isExact()) {
            return compare(name, horner, ReferenceEvaluator.evaluate(p, x), BigDecimal.ZERO, "égalité exacte");
        }
        MathContext ref = referenceContext(settings, p, x);
        BigDecimal reference = ReferenceEvaluator.evaluate(p, x, ref);
        BigDecimal magnitude = ReferenceEvaluator.evaluate(p.withAbsoluteCoefficients(), x.abs(), ref);
        return compare(name, horner, reference, errorBound(2L * iterations(p), settings, magnitude),
                "borne a priori 2·γ(2n)·Σ|aᵢ||x|^i");
    }

    /** P'(x) par Horner étendu comparé à la dérivée symbolique évaluée terme à terme. */
    public static Check verifyDerivative(Polynomial p, BigDecimal x, NumericSettings settings) {
        BigDecimal horner = DerivativeEvaluator.evaluate(p, x, settings).derivative();
        Polynomial derivative = p.derivative();
        String name = "P'(x) : Horner étendu vs dérivée symbolique";
        if (settings.isExact()) {
            return compare(name, horner, ReferenceEvaluator.evaluate(derivative, x), BigDecimal.ZERO, "égalité exacte");
        }
        MathContext ref = referenceContext(settings, p, x);
        BigDecimal reference = ReferenceEvaluator.evaluate(derivative, x, ref);
        BigDecimal magnitude = ReferenceEvaluator.evaluate(p.withAbsoluteCoefficients().derivative(), x.abs(), ref);
        return compare(name, horner, reference, errorBound(4L * iterations(p), settings, magnitude),
                "borne a priori 2·γ(4n)·Σ i|aᵢ||x|^(i−1)");
    }

    /** Le reste de la division par (x − r) comparé à P(r) calculé par l'oracle. */
    public static Check verifyRemainder(DivisionResult division) {
        Polynomial p = division.dividend();
        BigDecimal r = division.root();
        NumericSettings settings = division.settings();
        String name = "Reste R vs P(r) (oracle)";
        if (settings.isExact()) {
            return compare(name, division.remainder(), ReferenceEvaluator.evaluate(p, r), BigDecimal.ZERO,
                    "égalité exacte");
        }
        MathContext ref = referenceContext(settings, p, r);
        BigDecimal reference = ReferenceEvaluator.evaluate(p, r, ref);
        BigDecimal magnitude = ReferenceEvaluator.evaluate(p.withAbsoluteCoefficients(), r.abs(), ref);
        return compare(name, division.remainder(), reference, errorBound(2L * iterations(p), settings, magnitude),
                "borne a priori 2·γ(2n)·Σ|aᵢ||r|^i");
    }

    /**
     * Identité P(x) = (x − r)·Q(x) + R, vérifiée par multiplication polynomiale exacte.
     * En mode contrôlé, chaque coefficient reconstruit diffère de P d'au plus les deux arrondis
     * de l'étape correspondante : u·(|produit| + |accumulateur|) ; la tolérance double ce maximum.
     */
    public static Check verifyIdentity(DivisionResult division) {
        String name = "Identité P(x) = (x − r)·Q(x) + R";
        BigDecimal deviation = division.identityMaxDeviation();
        if (division.settings().isExact()) {
            return compare(name, deviation, BigDecimal.ZERO, BigDecimal.ZERO, "multiplication polynomiale exacte");
        }
        BigDecimal largest = BigDecimal.ZERO;
        for (HornerStep step : division.hornerScheme().steps()) {
            if (!step.isInitialization()) {
                largest = largest.max(step.product().abs().add(step.accumulatorAfter().abs()));
            }
        }
        BigDecimal tolerance = division.settings().unitRoundoff().multiply(largest).multiply(TWO).round(BOUND_CONTEXT);
        return compare(name, deviation, BigDecimal.ZERO, tolerance, "écart max par coefficient, borne 2·u·max(|b·r| + |b|)");
    }

    /**
     * Statut de r en tant que racine. En mode contrôlé, la tolérance est la borne d'erreur
     * a priori : un reste inférieur à cette borne est indiscernable de zéro (racine approximative).
     */
    public static RootStatus classifyRoot(DivisionResult division) {
        if (division.settings().isExact()) {
            return division.rootStatus(null);
        }
        return division.rootStatus(verifyRemainder(division).tolerance());
    }

    /** Batterie complète de contrôles pour un polynôme et un point x (la division utilise r = x). */
    public static List<Check> verifyAll(Polynomial p, BigDecimal x, NumericSettings settings) {
        List<Check> checks = new ArrayList<>();
        checks.add(verifyEvaluation(p, x, settings));
        checks.add(verifyDerivative(p, x, settings));
        DivisionResult division = SyntheticDivision.divide(p, x, settings);
        checks.add(verifyRemainder(division));
        checks.add(verifyIdentity(division));

        BigDecimal absSum = ReferenceEvaluator.sumOfCoefficients(p.withAbsoluteCoefficients());
        BigDecimal toleranceAtOne = errorBound(2L * iterations(p), settings, absSum);
        checks.add(compare("P(0) = a₀", HornerEvaluator.evaluate(p, BigDecimal.ZERO, settings),
                ReferenceEvaluator.valueAtZero(p), settings.isExact() ? BigDecimal.ZERO : toleranceAtOne,
                "terme constant"));
        checks.add(compare("P(1) = Σ aᵢ", HornerEvaluator.evaluate(p, BigDecimal.ONE, settings),
                ReferenceEvaluator.sumOfCoefficients(p), toleranceAtOne, "somme des coefficients"));
        checks.add(compare("P(−1) = Σ (−1)^i aᵢ", HornerEvaluator.evaluate(p, BigDecimal.ONE.negate(), settings),
                ReferenceEvaluator.alternatingSum(p), toleranceAtOne, "somme alternée des coefficients"));
        return checks;
    }

    /** Borne 2·γ(k)·magnitude ; 0 en mode exact. */
    static BigDecimal errorBound(long k, NumericSettings settings, BigDecimal magnitude) {
        if (settings.isExact() || k == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal ku = settings.unitRoundoff().multiply(BigDecimal.valueOf(k));
        if (ku.compareTo(BigDecimal.ONE) >= 0) {
            // Borne non informative (précision trop faible pour le degré) : on admet |magnitude|·2.
            return magnitude.abs().multiply(TWO).round(BOUND_CONTEXT);
        }
        BigDecimal gamma = ku.divide(BigDecimal.ONE.subtract(ku), BOUND_CONTEXT);
        return gamma.multiply(magnitude.abs()).multiply(TWO).round(BOUND_CONTEXT);
    }

    private static int iterations(Polynomial p) {
        return p.size() - 1;
    }

    /** Contexte de la référence : 2p + 10 chiffres, plus la taille des données pour ne pas les arrondir. */
    private static MathContext referenceContext(NumericSettings settings, Polynomial p, BigDecimal x) {
        int dataDigits = x.precision();
        for (BigDecimal c : p.coefficients()) {
            dataDigits = Math.max(dataDigits, c.precision());
        }
        return new MathContext(2 * settings.precision() + 10 + dataDigits, RoundingMode.HALF_EVEN);
    }
}
