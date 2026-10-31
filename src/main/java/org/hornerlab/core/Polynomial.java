package org.hornerlab.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Polynôme à coefficients {@link BigDecimal}, immuable.
 *
 * <p><b>Convention de stockage :</b> les coefficients sont rangés par puissances
 * <b>décroissantes</b>. {@code [2, -6, 2, -1]} représente
 * P(x) = 2x³ − 6x² + 2x − 1 ; {@code [3, 0, 0, 0, 2]} représente 3x⁴ + 2.
 *
 * <p><b>Normalisation :</b> les zéros dominants sont supprimés à la construction
 * ({@code [0, 0, 2, 1]} devient {@code [2, 1]}), car ils ne changent ni la valeur ni le degré.
 * Il reste toujours au moins un coefficient.
 *
 * <p><b>Polynôme nul :</b> représenté par {@code [0]}. Son degré vaut par convention −1
 * (notation informatique de −∞ ; le degré du polynôme nul n'est pas défini au sens usuel).
 * Un polynôme constant non nul est de degré 0.
 */
public final class Polynomial {

    private static final Polynomial ZERO = new Polynomial(List.of(BigDecimal.ZERO));

    private final List<BigDecimal> coefficients;

    private Polynomial(List<BigDecimal> normalizedCoefficients) {
        this.coefficients = normalizedCoefficients;
    }

    /**
     * Construit un polynôme à partir de ses coefficients en puissances décroissantes.
     *
     * @throws InvalidPolynomialException si la liste est {@code null}, vide ou contient {@code null}
     */
    public static Polynomial of(List<BigDecimal> coefficients) {
        if (coefficients == null) {
            throw new InvalidPolynomialException("La liste des coefficients est absente (null).");
        }
        if (coefficients.isEmpty()) {
            throw new InvalidPolynomialException(
                    "La liste des coefficients est vide : un polynôme possède au moins un coefficient.");
        }
        for (int i = 0; i < coefficients.size(); i++) {
            if (coefficients.get(i) == null) {
                throw new InvalidPolynomialException("Le coefficient n°" + (i + 1) + " est absent (null).");
            }
        }
        int first = 0;
        while (first < coefficients.size() - 1 && coefficients.get(first).signum() == 0) {
            first++;
        }
        List<BigDecimal> normalized = List.copyOf(coefficients.subList(first, coefficients.size()));
        if (normalized.size() == 1 && normalized.get(0).signum() == 0) {
            return ZERO;
        }
        return new Polynomial(normalized);
    }

    /** @see #of(List) */
    public static Polynomial of(BigDecimal... coefficients) {
        if (coefficients == null) {
            throw new InvalidPolynomialException("La liste des coefficients est absente (null).");
        }
        return of(Arrays.asList(coefficients));
    }

    /** Coefficients entiers, en puissances décroissantes. */
    public static Polynomial of(long... coefficients) {
        if (coefficients == null) {
            throw new InvalidPolynomialException("La liste des coefficients est absente (null).");
        }
        return of(Arrays.stream(coefficients).mapToObj(BigDecimal::valueOf).toList());
    }

    /**
     * Coefficients flottants. NaN et ±∞ sont refusés. La conversion utilise
     * {@link BigDecimal#valueOf(double)} : 0.1 devient exactement 0.1 (représentation décimale
     * la plus courte du double) et non 0.1000000000000000055511151231257827…
     */
    public static Polynomial ofDoubles(double... coefficients) {
        if (coefficients == null) {
            throw new InvalidPolynomialException("La liste des coefficients est absente (null).");
        }
        List<BigDecimal> values = new ArrayList<>(coefficients.length);
        for (int i = 0; i < coefficients.length; i++) {
            double c = coefficients[i];
            if (Double.isNaN(c) || Double.isInfinite(c)) {
                throw new InvalidPolynomialException(
                        "Le coefficient n°" + (i + 1) + " vaut " + c + " : NaN et l'infini ne sont pas acceptés.");
            }
            values.add(BigDecimal.valueOf(c));
        }
        return of(values);
    }

    /** Le polynôme nul, représenté par {@code [0]}. */
    public static Polynomial zero() {
        return ZERO;
    }

    /** Polynôme constant P(x) = c. */
    public static Polynomial constant(BigDecimal c) {
        return of(List.of(Objects.requireNonNull(c, "c")));
    }

    /** Le facteur linéaire (x − r), soit les coefficients {@code [1, -r]}. */
    public static Polynomial linearFactor(BigDecimal root) {
        Objects.requireNonNull(root, "root");
        return of(List.of(BigDecimal.ONE, root.negate()));
    }

    /** Coefficients en puissances décroissantes (liste non modifiable). */
    public List<BigDecimal> coefficients() {
        return coefficients;
    }

    /** Nombre de coefficients stockés (degré + 1, ou 1 pour le polynôme nul). */
    public int size() {
        return coefficients.size();
    }

    /** Degré du polynôme ; −1 par convention pour le polynôme nul. */
    public int degree() {
        return isZero() ? -1 : coefficients.size() - 1;
    }

    public boolean isZero() {
        return coefficients.size() == 1 && coefficients.get(0).signum() == 0;
    }

    /** Vrai pour un polynôme de la forme P(x) = c (y compris le polynôme nul). */
    public boolean isConstant() {
        return coefficients.size() == 1;
    }

    public BigDecimal leadingCoefficient() {
        return coefficients.get(0);
    }

    public BigDecimal constantTerm() {
        return coefficients.get(coefficients.size() - 1);
    }

    /** Coefficient a_k de x^k (zéro si k dépasse le degré). */
    public BigDecimal coefficientOfPower(int power) {
        if (power < 0) {
            throw new IllegalArgumentException("La puissance doit être positive ou nulle : " + power);
        }
        if (power >= coefficients.size()) {
            return BigDecimal.ZERO;
        }
        return coefficients.get(coefficients.size() - 1 - power);
    }

    /** Dérivée symbolique : d/dx Σ a_k x^k = Σ k·a_k x^(k−1). Calcul exact. */
    public Polynomial derivative() {
        if (isConstant()) {
            return ZERO;
        }
        int n = coefficients.size() - 1;
        List<BigDecimal> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(coefficients.get(i).multiply(BigDecimal.valueOf(n - i)));
        }
        return of(result);
    }

    /** Somme exacte de deux polynômes. */
    public Polynomial add(Polynomial other) {
        Objects.requireNonNull(other, "other");
        int size = Math.max(size(), other.size());
        List<BigDecimal> result = new ArrayList<>(size);
        for (int power = size - 1; power >= 0; power--) {
            result.add(coefficientOfPower(power).add(other.coefficientOfPower(power)));
        }
        return of(result);
    }

    /** Produit exact de deux polynômes (produit de Cauchy des coefficients). */
    public Polynomial multiply(Polynomial other) {
        Objects.requireNonNull(other, "other");
        if (isZero() || other.isZero()) {
            return ZERO;
        }
        BigDecimal[] result = new BigDecimal[size() + other.size() - 1];
        Arrays.fill(result, BigDecimal.ZERO);
        for (int i = 0; i < size(); i++) {
            for (int j = 0; j < other.size(); j++) {
                result[i + j] = result[i + j].add(coefficients.get(i).multiply(other.coefficients.get(j)));
            }
        }
        return of(Arrays.asList(result));
    }

    /** Multiplication exacte de tous les coefficients par k. */
    public Polynomial scale(BigDecimal k) {
        Objects.requireNonNull(k, "k");
        return of(coefficients.stream().map(c -> c.multiply(k)).toList());
    }

    /** Polynôme dont les coefficients sont les valeurs absolues de ceux-ci (sert aux bornes d'erreur). */
    public Polynomial withAbsoluteCoefficients() {
        return of(coefficients.stream().map(BigDecimal::abs).toList());
    }

    /** Égalité <i>numérique</i> des coefficients ({@code 2.0} et {@code 2} sont égaux). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Polynomial other) || size() != other.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            if (coefficients.get(i).compareTo(other.coefficients.get(i)) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (BigDecimal c : coefficients) {
            h = 31 * h + c.stripTrailingZeros().hashCode();
        }
        return h;
    }

    @Override
    public String toString() {
        return PolynomialFormatter.toAscii(this);
    }
}
