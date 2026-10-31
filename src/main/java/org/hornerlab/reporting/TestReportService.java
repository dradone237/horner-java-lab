package org.hornerlab.reporting;

import org.hornerlab.core.DerivativeEvaluator;
import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.HornerResult;
import org.hornerlab.core.HornerStep;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.RootStatus;
import org.hornerlab.core.SyntheticDivision;
import org.hornerlab.validation.InputValidator;
import org.hornerlab.validation.InvalidInputException;
import org.hornerlab.validation.RandomPolynomialGenerator;
import org.hornerlab.validation.ReferenceEvaluator;
import org.hornerlab.validation.ResultVerifier;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Campagne de validation intégrée à l'application (CLI {@code selftest} et écran « Tests et validation »),
 * et lecture des rapports Maven réels (Surefire, JaCoCo) lorsqu'ils existent.
 *
 * <p>Tous les résultats affichés proviennent d'exécutions réelles du moteur ; rien n'est simulé.
 */
public final class TestReportService {

    /** Graine par défaut des campagnes aléatoires (enregistrée dans chaque rapport). */
    public static final long DEFAULT_SEED = 20261031L;

    /** Résultat d'un contrôle. */
    public record CheckResult(String category, String name, boolean passed, String details) {
    }

    /** Rapport de la campagne intégrée. */
    public record SelfTestReport(long seed, int randomCases, List<CheckResult> results, Duration duration,
                                 LocalDateTime date) {

        public SelfTestReport {
            results = List.copyOf(results);
        }

        public long passedCount() {
            return results.stream().filter(CheckResult::passed).count();
        }

        public long failedCount() {
            return results.size() - passedCount();
        }

        public boolean allPassed() {
            return failedCount() == 0;
        }

        public String toText() {
            StringBuilder sb = new StringBuilder();
            sb.append("Campagne de validation intégrée - graine ").append(seed).append(", ")
                    .append(randomCases).append(" cas aléatoires\n");
            for (CheckResult r : results) {
                sb.append(r.passed() ? "[OK]    " : "[ECHEC] ").append(r.category()).append(" - ")
                        .append(r.name()).append(" : ").append(r.details()).append('\n');
            }
            sb.append("Bilan : ").append(results.size()).append(" contrôles, ").append(passedCount())
                    .append(" réussis, ").append(failedCount()).append(" échoués (durée ")
                    .append(duration.toMillis()).append(" ms)\n");
            return sb.toString();
        }

        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("# Rapport de la campagne de validation intégrée\n\n");
            sb.append("- Date : ").append(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append('\n');
            sb.append("- Graine aléatoire : ").append(seed).append('\n');
            sb.append("- Cas aléatoires par propriété : ").append(randomCases).append('\n');
            sb.append("- Durée : ").append(duration.toMillis()).append(" ms\n");
            sb.append("- Bilan : ").append(results.size()).append(" contrôles, ").append(passedCount())
                    .append(" réussis, ").append(failedCount()).append(" échoués\n\n");
            sb.append("| Catégorie | Contrôle | Résultat | Détails |\n|---|---|---|---|\n");
            for (CheckResult r : results) {
                sb.append("| ").append(r.category()).append(" | ").append(r.name()).append(" | ")
                        .append(r.passed() ? "OK" : "ÉCHEC").append(" | ")
                        .append(r.details().replace("|", "\\|")).append(" |\n");
            }
            return sb.toString();
        }
    }

    /** Synthèse des rapports Surefire ({@code target/surefire-reports}). */
    public record MavenTestSummary(int suites, int tests, int failures, int errors, int skipped) {

        public int passed() {
            return tests - failures - errors - skipped;
        }
    }

    /** Couverture d'un paquet, lue dans {@code jacoco.csv}. */
    public record PackageCoverage(String packageName, long instructionsMissed, long instructionsCovered,
                                  long branchesMissed, long branchesCovered, long methodsMissed,
                                  long methodsCovered, long classes) {

        public double instructionRatio() {
            return ratio(instructionsCovered, instructionsMissed);
        }

        public double branchRatio() {
            return ratio(branchesCovered, branchesMissed);
        }

        public double methodRatio() {
            return ratio(methodsCovered, methodsMissed);
        }

        private static double ratio(long covered, long missed) {
            return covered + missed == 0 ? 1.0 : (double) covered / (covered + missed);
        }
    }

    /** Exécute les cas de référence puis les propriétés sur {@code randomCases} cas aléatoires. */
    public SelfTestReport runSelfTest(long seed, int randomCases) {
        long start = System.nanoTime();
        List<CheckResult> results = new ArrayList<>();
        referenceChecks(results);
        propertyChecks(results, seed, randomCases);
        return new SelfTestReport(seed, randomCases, results, Duration.ofNanos(System.nanoTime() - start),
                LocalDateTime.now());
    }

    private static void referenceChecks(List<CheckResult> results) {
        Polynomial ref = Polynomial.of(2, -6, 2, -1);
        BigDecimal three = BigDecimal.valueOf(3);
        check(results, "Référence", "P(3) = 5 pour 2x³ − 6x² + 2x − 1",
                () -> HornerEvaluator.evaluate(ref, three).compareTo(BigDecimal.valueOf(5)) == 0, "valeur attendue 5");
        check(results, "Référence", "Étapes 2, 0, 2, 5", () -> {
            HornerResult r = HornerEvaluator.evaluateWithSteps(ref, three);
            return r.steps().stream().map(HornerStep::accumulatorAfter).map(BigDecimal::intValueExact).toList()
                    .equals(List.of(2, 0, 2, 5)) && r.multiplications() == 3 && r.additions() == 3;
        }, "accumulateurs successifs et 3 multiplications + 3 additions");
        check(results, "Référence", "P'(3) = 20",
                () -> DerivativeEvaluator.evaluate(ref, three).derivative().compareTo(BigDecimal.valueOf(20)) == 0,
                "Horner étendu");
        check(results, "Référence", "Division par (x − 3) : Q = [2, 0, 2], R = 5", () -> {
            DivisionResult d = SyntheticDivision.divide(ref, three);
            return d.quotient().equals(Polynomial.of(2, 0, 2)) && d.remainder().compareTo(BigDecimal.valueOf(5)) == 0
                    && d.identityHolds();
        }, "quotient, reste et identité P = (x − r)Q + R");
        check(results, "Référence", "x³ − 6x² + 11x − 6 : 2 est une racine exacte", () -> {
            DivisionResult d = SyntheticDivision.divide(Polynomial.of(1, -6, 11, -6), BigDecimal.valueOf(2));
            return d.remainder().signum() == 0 && d.rootStatus(null) == RootStatus.EXACT_ROOT;
        }, "reste exactement nul");
        check(results, "Cas particulier", "Polynôme nul", () -> HornerEvaluator.evaluate(Polynomial.zero(),
                BigDecimal.TEN).signum() == 0, "P(10) = 0, degré conventionnel −1");
        check(results, "Cas particulier", "Polynôme constant", () -> HornerEvaluator.evaluate(Polynomial.of(7),
                BigDecimal.valueOf(123)).compareTo(BigDecimal.valueOf(7)) == 0, "P(123) = 7");
        check(results, "Cas particulier", "Zéros intermédiaires 3x⁴ + 2 en x = −1",
                () -> HornerEvaluator.evaluate(Polynomial.of(3, 0, 0, 0, 2), BigDecimal.ONE.negate())
                        .compareTo(BigDecimal.valueOf(5)) == 0, "P(−1) = 5");
        check(results, "Cas particulier", "Coefficients décimaux", () -> HornerEvaluator.evaluate(
                new InputValidator().parsePolynomial("1.5,-2.25,0,3"), new BigDecimal("0.5"))
                .compareTo(new BigDecimal("2.625")) == 0, "P(0.5) = 2.625");
        check(results, "Précision", "Annulation : exact 1E-40, contrôlé 0, jamais confondus", () -> {
            Polynomial square = Polynomial.of(1, -2, 1);
            BigDecimal x = new BigDecimal("1.00000000000000000001");
            BigDecimal exact = HornerEvaluator.evaluate(square, x);
            BigDecimal rounded = HornerEvaluator.evaluate(square, x, NumericSettings.controlled());
            DivisionResult exactDivision = SyntheticDivision.divide(square, x);
            return exact.compareTo(new BigDecimal("1E-40")) == 0 && rounded.signum() == 0
                    && exactDivision.rootStatus(null) == RootStatus.NOT_A_ROOT;
        }, "le mode exact refuse de qualifier x de racine");
        check(results, "Entrée invalide", "« abc » refusé", () -> rejects("abc"), "message explicite, aucune conversion en 0");
        check(results, "Entrée invalide", "« 1,5;2 » refusé", () -> rejects("1,5;2"), "séparateur « ; » signalé");
        check(results, "Entrée invalide", "« NaN » refusé", () -> rejects("1,NaN"), "NaN refusé");
        check(results, "Entrée invalide", "liste vide refusée", () -> rejects("  "), "liste vide signalée");
    }

    private static boolean rejects(String input) {
        try {
            new InputValidator().parseCoefficients(input);
            return false;
        } catch (InvalidInputException e) {
            return !e.getMessage().isBlank();
        }
    }

    private static void propertyChecks(List<CheckResult> results, long seed, int cases) {
        RandomPolynomialGenerator gen = new RandomPolynomialGenerator(seed);
        Map<String, int[]> counters = new LinkedHashMap<>(); // [réussis, échoués]
        String[] names = {"Horner = oracle Σ a_ix^i (exact)", "P(0) = a₀", "P(1) = Σ a_i", "P(−1) = Σ (−1)^i a_i",
                "P_k·(x) = k·P(x)", "(P + Q)(x) = P(x) + Q(x)", "Reste de la division = P(r)",
                "Identité P = (x − r)Q + R", "P'(x) Horner = dérivée symbolique", "Mode contrôlé dans la borne d'erreur"};
        for (String n : names) {
            counters.put(n, new int[2]);
        }
        String firstFailure = null;
        for (int i = 0; i < cases; i++) {
            Polynomial p = gen.nextPolynomial(12);
            Polynomial q = gen.nextPolynomial(12);
            BigDecimal x = gen.nextX();
            BigDecimal k = gen.nextScalar();
            BigDecimal px = HornerEvaluator.evaluate(p, x);
            DivisionResult d = SyntheticDivision.divide(p, x);
            boolean[] outcomes = {
                    px.compareTo(ReferenceEvaluator.evaluate(p, x)) == 0,
                    HornerEvaluator.evaluate(p, BigDecimal.ZERO).compareTo(ReferenceEvaluator.valueAtZero(p)) == 0,
                    HornerEvaluator.evaluate(p, BigDecimal.ONE).compareTo(ReferenceEvaluator.sumOfCoefficients(p)) == 0,
                    HornerEvaluator.evaluate(p, BigDecimal.ONE.negate()).compareTo(ReferenceEvaluator.alternatingSum(p)) == 0,
                    HornerEvaluator.evaluate(p.scale(k), x).compareTo(px.multiply(k)) == 0,
                    HornerEvaluator.evaluate(p.add(q), x).compareTo(px.add(HornerEvaluator.evaluate(q, x))) == 0,
                    d.remainder().compareTo(ReferenceEvaluator.evaluate(p, x)) == 0,
                    d.identityHolds(),
                    DerivativeEvaluator.evaluate(p, x).derivative().compareTo(ReferenceEvaluator.derivativeAt(p, x)) == 0,
                    ResultVerifier.verifyEvaluation(p, x, NumericSettings.controlled(16)).passed()
            };
            for (int j = 0; j < names.length; j++) {
                counters.get(names[j])[outcomes[j] ? 0 : 1]++;
                if (!outcomes[j] && firstFailure == null) {
                    firstFailure = names[j] + " : P = " + p.coefficients() + ", x = " + x;
                }
            }
        }
        for (Map.Entry<String, int[]> e : counters.entrySet()) {
            int ok = e.getValue()[0];
            int ko = e.getValue()[1];
            String details = ok + "/" + cases + " cas réussis (graine " + seed + ")"
                    + (ko > 0 && firstFailure != null && firstFailure.startsWith(e.getKey()) ? " - premier échec : " + firstFailure : "");
            results.add(new CheckResult("Propriété", e.getKey(), ko == 0, details));
        }
    }

    private static void check(List<CheckResult> results, String category, String name, BooleanSupplier test,
                              String details) {
        boolean passed;
        String info = details;
        try {
            passed = test.getAsBoolean();
        } catch (RuntimeException e) {
            passed = false;
            info = details + " - exception : " + e;
        }
        results.add(new CheckResult(category, name, passed, info));
    }

    /** Écrit le rapport Markdown ; si {@code target} est un dossier, un nom horodaté est choisi. */
    public static Path writeMarkdown(SelfTestReport report, Path target) {
        try {
            Path file = target;
            if (Files.isDirectory(target)) {
                file = target.resolve("selftest-" + report.date().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".md");
            }
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, report.toMarkdown(), StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Lit les fichiers {@code TEST-*.xml} de Surefire ; vide si le dossier n'existe pas. */
    public static Optional<MavenTestSummary> readSurefireReports(Path directory) {
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        int suites = 0;
        int tests = 0;
        int failures = 0;
        int errors = 0;
        int skipped = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "TEST-*.xml")) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            for (Path file : files) {
                var root = factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
                suites++;
                tests += intAttribute(root, "tests");
                failures += intAttribute(root, "failures");
                errors += intAttribute(root, "errors");
                skipped += intAttribute(root, "skipped");
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return suites == 0 ? Optional.empty() : Optional.of(new MavenTestSummary(suites, tests, failures, errors, skipped));
    }

    private static int intAttribute(org.w3c.dom.Element element, String name) {
        String value = element.getAttribute(name);
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    /** Lit {@code jacoco.csv} et agrège la couverture par paquet ; vide si absent ou illisible. */
    public static Optional<List<PackageCoverage>> readJacocoCsv(Path csv) {
        if (!Files.isRegularFile(csv)) {
            return Optional.empty();
        }
        try {
            List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
            Map<String, long[]> byPackage = new LinkedHashMap<>();
            for (String line : lines.subList(1, lines.size())) {
                String[] f = line.split(",", -1);
                if (f.length < 13) {
                    continue;
                }
                long[] acc = byPackage.computeIfAbsent(f[1], key -> new long[7]);
                acc[0] += Long.parseLong(f[3]);
                acc[1] += Long.parseLong(f[4]);
                acc[2] += Long.parseLong(f[5]);
                acc[3] += Long.parseLong(f[6]);
                acc[4] += Long.parseLong(f[11]);
                acc[5] += Long.parseLong(f[12]);
                acc[6]++;
            }
            List<PackageCoverage> result = new ArrayList<>();
            byPackage.forEach((pkg, a) -> result.add(new PackageCoverage(pkg, a[0], a[1], a[2], a[3], a[4], a[5], a[6])));
            return Optional.of(List.copyOf(result));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }
}
