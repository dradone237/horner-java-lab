package org.hornerlab.reporting;

import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.validation.RandomPolynomialGenerator;
import org.hornerlab.validation.ReferenceEvaluator;
import org.hornerlab.validation.ResultVerifier;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Mesure du temps d'exécution de Horner face à l'évaluation naïve (puissances recalculées).
 *
 * <p>Précautions : tours de chauffe (compilation JIT), répétitions, consommation des résultats
 * (évite l'élimination de code mort) et vérification préalable que les méthodes donnent le même
 * résultat. Les durées dépendent de la machine : elles sont <b>indicatives</b>. La mémoire indiquée
 * est le volume <b>alloué</b> par le thread (mesure HotSpot), pas la mémoire résidente.
 */
public final class BenchmarkService {

    /** Au-delà, l'évaluation naïve en O(n²) multiplications est trop lente pour être répétée. */
    static final int NAIVE_MAX_COEFFICIENTS = 2_001;

    private static final BigDecimal X = new BigDecimal("0.99");
    private static final NumericSettings SETTINGS = NumericSettings.controlled(34);

    /** Une mesure. */
    public record Measurement(int coefficients, String method, double meanMillis, double minMillis,
                              long multiplications, long additions, long allocatedBytesPerCall, boolean consistent) {
    }

    /** Ensemble des mesures. */
    public record Report(List<Measurement> measurements, int repetitions, int warmup, long seed) {

        public Report {
            measurements = List.copyOf(measurements);
        }

        public boolean allConsistent() {
            return measurements.stream().allMatch(Measurement::consistent);
        }

        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("# Benchmark Horner vs évaluation naïve\n\n");
            sb.append("- Mode : ").append(SETTINGS.describe()).append(", x = ").append(X.toPlainString()).append('\n');
            sb.append("- Répétitions : ").append(repetitions).append(", tours de chauffe : ").append(warmup)
                    .append(", graine : ").append(seed).append('\n');
            sb.append("- Java : ").append(System.getProperty("java.version")).append(" (")
                    .append(System.getProperty("java.vm.name")).append("), ")
                    .append(Runtime.getRuntime().availableProcessors()).append(" cœurs logiques\n");
            sb.append("- Durées indicatives ; « octets alloués » = allocations du thread par appel (approximatif).\n\n");
            sb.append("| Coefficients | Méthode | Temps moyen (ms) | Temps min (ms) | Multiplications | Additions | Octets alloués/appel | Résultats cohérents |\n");
            sb.append("|---:|---|---:|---:|---:|---:|---:|---|\n");
            for (Measurement m : measurements) {
                sb.append(String.format(Locale.ROOT, "| %d | %s | %.4f | %.4f | %s | %s | %s | %s |%n",
                        m.coefficients(), m.method(), m.meanMillis(), m.minMillis(), count(m.multiplications()),
                        count(m.additions()), count(m.allocatedBytesPerCall()), m.consistent() ? "oui" : "NON"));
            }
            return sb.toString();
        }

        private static String count(long value) {
            return value < 0 ? "n/d" : Long.toString(value);
        }
    }

    /** Lance le benchmark pour chaque taille (nombre de coefficients). */
    public Report run(List<Integer> sizes, int repetitions, int warmup, long seed) {
        if (repetitions < 1 || warmup < 0) {
            throw new IllegalArgumentException("Répétitions ≥ 1 et chauffe ≥ 0 exigées.");
        }
        RandomPolynomialGenerator gen = new RandomPolynomialGenerator(seed);
        List<Measurement> measurements = new ArrayList<>();
        for (int size : sizes) {
            List<BigDecimal> coefficients = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                BigDecimal c = BigDecimal.valueOf(gen.nextScalar().intValue() % 10);
                coefficients.add(i == 0 && c.signum() == 0 ? BigDecimal.ONE : c);
            }
            Polynomial p = Polynomial.of(coefficients);
            int n = p.size() - 1;

            // Vérification préalable : les méthodes comparées calculent bien la même valeur.
            BigDecimal horner = HornerEvaluator.evaluate(p, X, SETTINGS);
            boolean hornerOk = ResultVerifier.verifyEvaluation(p, X, SETTINGS).passed();
            measurements.add(measure(p.size(), "Horner", warmup, repetitions,
                    () -> HornerEvaluator.evaluate(p, X, SETTINGS), n, n, hornerOk));

            measurements.add(measure(p.size(), "Somme des termes (BigDecimal.pow)", warmup, repetitions,
                    () -> ReferenceEvaluator.evaluate(p, X, SETTINGS.mathContext()), -1, -1, hornerOk));

            if (p.size() <= NAIVE_MAX_COEFFICIENTS) {
                ReferenceEvaluator.NaiveEvaluation naive = ReferenceEvaluator.evaluateNaive(p, X, SETTINGS);
                BigDecimal tolerance = ResultVerifier.verifyEvaluation(p, X, SETTINGS).tolerance()
                        .multiply(BigDecimal.valueOf(Math.max(1, n)));
                boolean naiveOk = naive.value().subtract(horner).abs().compareTo(tolerance) <= 0;
                measurements.add(measure(p.size(), "Naïve (puissances recalculées)", warmup, repetitions,
                        () -> ReferenceEvaluator.evaluateNaive(p, X, SETTINGS).value(),
                        naive.multiplications(), naive.additions(), naiveOk));
            }
        }
        return new Report(measurements, repetitions, warmup, seed);
    }

    private static Measurement measure(int size, String method, int warmup, int repetitions,
                                       Supplier<BigDecimal> task, long multiplications, long additions,
                                       boolean consistent) {
        int sink = 0;
        for (int i = 0; i < warmup; i++) {
            sink += task.get().signum();
        }
        double total = 0;
        double min = Double.MAX_VALUE;
        long allocatedBefore = allocatedBytes();
        for (int i = 0; i < repetitions; i++) {
            long t0 = System.nanoTime();
            sink += task.get().signum();
            double ms = (System.nanoTime() - t0) / 1e6;
            total += ms;
            min = Math.min(min, ms);
        }
        long allocatedAfter = allocatedBytes();
        long perCall = allocatedBefore < 0 || allocatedAfter < 0 ? -1 : (allocatedAfter - allocatedBefore) / repetitions;
        if (sink == Integer.MIN_VALUE) {
            System.out.print(""); // consommation du résultat, empêche l'élimination du calcul
        }
        return new Measurement(size, method, total / repetitions, min, multiplications, additions, perCall, consistent);
    }

    /** Octets alloués par le thread courant (HotSpot) ; −1 si la mesure n'est pas disponible. */
    private static long allocatedBytes() {
        if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean
                && bean.isThreadAllocatedMemorySupported() && bean.isThreadAllocatedMemoryEnabled()) {
            return bean.getCurrentThreadAllocatedBytes();
        }
        return -1;
    }
}
