package org.hornerlab;

import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.HornerResult;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.PolynomialFormatter;
import org.hornerlab.core.SyntheticDivision;
import org.hornerlab.examples.ExampleLibrary;
import org.hornerlab.reporting.BenchmarkService;
import org.hornerlab.reporting.TestReportService;
import org.hornerlab.validation.InputValidator;
import org.hornerlab.validation.ResultVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Chaînes complètes : saisie → validation → parsing → Horner → résultat → affichage. */
class HornerIntegrationTest {

    private final InputValidator validator = new InputValidator();

    @Test
    @DisplayName("Saisie « 2,-6,2,-1 » et « 3 » → affichage P(3) = 5")
    void evaluationChain() {
        Polynomial p = validator.parsePolynomial("2,-6,2,-1");
        BigDecimal x = validator.parseNumber("3", "x");
        HornerResult r = HornerEvaluator.evaluateWithSteps(p, x);
        String display = "P(" + PolynomialFormatter.formatNumber(x) + ") = " + PolynomialFormatter.formatNumber(r.value());
        assertEquals("P(3) = 5", display);
        assertEquals("2x³ − 6x² + 2x − 1", PolynomialFormatter.toUnicode(p));
    }

    @Test
    @DisplayName("Saisie → division synthétique → quotient → reste → identité vérifiée par l'oracle")
    void divisionChain() {
        Polynomial p = validator.parsePolynomial("[2, -6, 2, -1]");
        DivisionResult d = SyntheticDivision.divide(p, validator.parseNumber("3", "r"));
        assertEquals("[2, 0, 2]", PolynomialFormatter.formatList(d.quotientCoefficients()));
        assertEquals("5", PolynomialFormatter.formatNumber(d.remainder()));
        assertTrue(ResultVerifier.verifyIdentity(d).passed());
        assertTrue(ResultVerifier.verifyRemainder(d).passed());
    }

    @Test
    @DisplayName("Tous les exemples préchargés se chargent et passent la vérification exacte")
    void examplesAreConsistent() {
        List<ExampleLibrary.Example> examples = ExampleLibrary.load();
        assertTrue(examples.size() >= 8);
        for (ExampleLibrary.Example e : examples) {
            List<ResultVerifier.Check> checks = ResultVerifier.verifyAll(e.polynomial(), e.x(), NumericSettings.exact());
            assertTrue(checks.stream().allMatch(c -> c.verdict() == ResultVerifier.Verdict.IDENTICAL), e.id() + " : " + checks);
            assertFalse(e.toString().isBlank());
        }
        assertEquals("reference", examples.get(0).id());
    }

    @Test
    @DisplayName("Campagne intégrée : tous les contrôles réussissent, rapport exporté")
    void selfTestAndExport(@TempDir Path dir) throws IOException {
        TestReportService.SelfTestReport report = new TestReportService().runSelfTest(TestReportService.DEFAULT_SEED, 100);
        assertTrue(report.allPassed(), report.toText());
        assertEquals(0, report.failedCount());
        Path file = TestReportService.writeMarkdown(report, dir);
        String md = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(md.contains("Graine aléatoire : " + TestReportService.DEFAULT_SEED));
        assertTrue(md.contains("| OK |"));
        Path named = TestReportService.writeMarkdown(report, dir.resolve("sub/rapport.md"));
        assertTrue(Files.exists(named));
    }

    @Test
    @DisplayName("Lecture de rapports Surefire et JaCoCo réels ou absents")
    void readsMavenReports(@TempDir Path dir) throws IOException {
        assertTrue(TestReportService.readSurefireReports(dir.resolve("absent")).isEmpty());
        assertTrue(TestReportService.readSurefireReports(dir).isEmpty());
        Files.writeString(dir.resolve("TEST-a.xml"), "<?xml version=\"1.0\"?><testsuite name=\"a\" tests=\"10\" failures=\"1\" errors=\"0\" skipped=\"2\"></testsuite>");
        Files.writeString(dir.resolve("TEST-b.xml"), "<?xml version=\"1.0\"?><testsuite name=\"b\" tests=\"5\" failures=\"0\" errors=\"1\"></testsuite>");
        TestReportService.MavenTestSummary s = TestReportService.readSurefireReports(dir).orElseThrow();
        assertEquals(new TestReportService.MavenTestSummary(2, 15, 1, 1, 2), s);
        assertEquals(11, s.passed());

        Path csv = dir.resolve("jacoco.csv");
        assertTrue(TestReportService.readJacocoCsv(csv).isEmpty());
        Files.writeString(csv, """
                GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
                g,org.hornerlab.core,A,5,95,1,9,0,10,0,5,0,4
                g,org.hornerlab.core,B,0,100,0,0,0,10,0,5,1,3
                bad line
                """);
        var coverage = TestReportService.readJacocoCsv(csv).orElseThrow();
        assertEquals(1, coverage.size());
        assertEquals(0.975, coverage.get(0).instructionRatio(), 1e-9);
        assertEquals(0.9, coverage.get(0).branchRatio(), 1e-9);
        assertEquals(7.0 / 8.0, coverage.get(0).methodRatio(), 1e-9);
        assertEquals(2, coverage.get(0).classes());
        Files.writeString(dir.resolve("TEST-bad.xml"), "pas du xml");
        assertTrue(TestReportService.readSurefireReports(dir).isEmpty());
    }

    @Test
    @DisplayName("Benchmark rapide : Horner fait n multiplications, la méthode naïve n(n+1)/2 + n + 1")
    void benchmarkSmoke() {
        BenchmarkService.Report report = new BenchmarkService().run(List.of(10, 50), 2, 1, 7);
        assertTrue(report.allConsistent(), report::toMarkdown);
        BenchmarkService.Measurement horner = report.measurements().get(0);
        assertEquals("Horner", horner.method());
        assertEquals(9, horner.multiplications());
        BenchmarkService.Measurement naive = report.measurements().get(2);
        assertEquals(9 * 10 / 2 + 10, naive.multiplications());
        assertTrue(report.toMarkdown().contains("| 50 | Horner |"));
    }
}
