package org.hornerlab;

import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.HornerResult;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.reporting.BenchmarkService;
import org.hornerlab.reporting.TestReportService;
import org.hornerlab.validation.RandomPolynomialGenerator;
import org.hornerlab.validation.ReferenceEvaluator;
import org.hornerlab.validation.ResultVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests lourds, exclus de la campagne rapide : {@code mvn verify -Pheavy}. */
@Tag("heavy")
class HornerHeavyTest {

    @Test
    @DisplayName("10 000 coefficients, précision contrôlée : résultat dans la borne d'erreur")
    void tenThousandCoefficientsControlled() {
        RandomPolynomialGenerator gen = new RandomPolynomialGenerator(99);
        List<BigDecimal> coefficients = new ArrayList<>();
        for (int i = 0; i < 10_001; i++) {
            BigDecimal c = gen.nextCoefficient();
            coefficients.add(i == 0 && c.signum() == 0 ? BigDecimal.ONE : c);
        }
        Polynomial p = Polynomial.of(coefficients);
        HornerResult r = HornerEvaluator.evaluateWithSteps(p, new BigDecimal("0.999"), NumericSettings.controlled());
        assertEquals(10_000, r.multiplications());
        assertTrue(ResultVerifier.verifyEvaluation(p, new BigDecimal("0.999"), NumericSettings.controlled()).passed());
    }

    @Test
    @DisplayName("Degré 2 000 en mode exact avec x entier : égalité stricte avec l'oracle")
    void largeExactDegree() {
        List<BigDecimal> coefficients = new ArrayList<>();
        for (int i = 0; i <= 2000; i++) {
            coefficients.add(BigDecimal.valueOf((i % 7) - 3));
        }
        Polynomial p = Polynomial.of(coefficients);
        BigDecimal x = BigDecimal.valueOf(3);
        assertEquals(0, HornerEvaluator.evaluate(p, x).compareTo(ReferenceEvaluator.evaluate(p, x)));
    }

    @Test
    @DisplayName("Campagne de 5 000 cas aléatoires par propriété")
    void bigSelfTest() {
        TestReportService.SelfTestReport report = new TestReportService().runSelfTest(TestReportService.DEFAULT_SEED, 5_000);
        assertTrue(report.allPassed(), report.toText());
    }

    @Test
    @DisplayName("Benchmark 10 → 10 000 coefficients : résultats cohérents")
    void benchmark() {
        BenchmarkService.Report report = new BenchmarkService().run(List.of(10, 100, 1_000, 10_000), 3, 2, 1);
        assertTrue(report.allConsistent(), report.toMarkdown());
    }
}
