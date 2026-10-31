package org.hornerlab.gui;

import org.hornerlab.core.DerivativeEvaluator;
import org.hornerlab.core.DerivativeResult;
import org.hornerlab.core.DerivativeStep;
import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.core.HornerResult;
import org.hornerlab.core.HornerStep;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.PolynomialFormatter;
import org.hornerlab.core.SyntheticDivision;
import org.hornerlab.reporting.TestReportService;
import org.hornerlab.validation.InputValidator;
import org.hornerlab.validation.InvalidInputException;
import org.hornerlab.validation.ReferenceEvaluator;
import org.hornerlab.validation.ResultVerifier;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.hornerlab.core.PolynomialFormatter.formatNumber;
import static org.hornerlab.core.PolynomialFormatter.formatOperand;
import static org.hornerlab.core.PolynomialFormatter.toUnicode;

/**
 * Contenu du MODE SOUTENANCE (8 diapositives). Chaque valeur affichée est calculée à l'instant
 * par le moteur réel ; aucune valeur n'est écrite en dur dans le texte.
 */
final class DemonstrationScript {

    /** Une diapositive ; si {@code progressive}, ses éléments apparaissent un par un. */
    record Slide(String title, String lead, List<String> items, boolean progressive) {
    }

    private DemonstrationScript() {
    }

    static List<Slide> build() {
        Polynomial p = Polynomial.of(2, -6, 2, -1);
        BigDecimal x = BigDecimal.valueOf(3);
        HornerResult horner = HornerEvaluator.evaluateWithSteps(p, x);
        String px = "P(" + formatNumber(x) + ") = " + formatNumber(horner.value());
        List<Slide> slides = new ArrayList<>();

        slides.add(new Slide("1 — Le polynôme étudié", "P(x) = " + toUnicode(p), List.of(
                "Coefficients rangés par puissances décroissantes : " + PolynomialFormatter.coefficientList(p),
                "Degré : " + p.degree() + "   —   valeur choisie : x = " + formatNumber(x),
                "Objectif : calculer " + "P(" + formatNumber(x) + ") avec le moins d'opérations possible"), false));

        slides.add(new Slide("2 — Le principe de Horner", "P(x) = " + PolynomialFormatter.nestedForm(p, true), List.of(
                "On part du coefficient dominant.",
                "À chaque étape : accumulateur ← accumulateur × x + coefficient suivant.",
                "Aucune puissance de x n'est calculée.",
                "Degré n : exactement n multiplications et n additions."), false));

        List<String> manual = new ArrayList<>();
        for (HornerStep s : horner.steps()) {
            manual.add(s.isInitialization()
                    ? "Étape " + s.number() + " : accumulateur = " + formatNumber(s.coefficient()) + "  (coefficient dominant)"
                    : "Étape " + s.number() + " : " + formatOperand(s.accumulatorBefore()) + " × " + formatNumber(x)
                    + " + " + formatOperand(s.coefficient()) + " = " + formatNumber(s.accumulatorAfter()));
        }
        manual.add("Résultat : " + px);
        slides.add(new Slide("3 — Exécution pas à pas pour x = " + formatNumber(x), px, manual, true));

        StringBuilder terms = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (int power = p.size() - 1; power >= 0; power--) {
            BigDecimal a = p.coefficientOfPower(power);
            BigDecimal term = a.multiply(x.pow(power));
            String sep = terms.isEmpty() ? "" : " + ";
            terms.append(sep).append(formatOperand(a)).append("·").append(formatNumber(x)).append(power > 1 ? PolynomialFormatter.superscript(power) : power == 1 ? "" : "⁰");
            values.append(values.isEmpty() ? "" : " + ").append(formatOperand(term));
        }
        ResultVerifier.Check check = ResultVerifier.verifyEvaluation(p, x, NumericSettings.exact());
        ReferenceEvaluator.NaiveEvaluation naive = ReferenceEvaluator.evaluateNaive(p, x, NumericSettings.exact());
        slides.add(new Slide("4 — Comparaison avec l'évaluation classique", "Somme des termes a_i·x^i", List.of(
                terms.toString(),
                "= " + values + " = " + formatNumber(ReferenceEvaluator.evaluate(p, x)),
                "Oracle indépendant : " + formatNumber(check.reference()) + "   Horner : " + formatNumber(check.computed())
                        + "   → " + check.verdict().label(),
                "Horner : " + horner.multiplications() + " multiplications + " + horner.additions() + " additions",
                "Naïve (puissances recalculées) : " + naive.multiplications() + " multiplications + " + naive.additions() + " additions"),
                true));

        DivisionResult d = SyntheticDivision.divide(p, x);
        DivisionResult rootCase = SyntheticDivision.divide(Polynomial.of(1, -6, 11, -6), BigDecimal.valueOf(2));
        slides.add(new Slide("5 — Division synthétique par " + PolynomialFormatter.formatDivisor(x, true),
                "Les accumulateurs de Horner sont les coefficients du quotient", List.of(
                "Schéma : " + String.join("  |  ", d.hornerScheme().steps().stream().map(s -> formatNumber(s.accumulatorAfter())).toList()),
                "Quotient Q(x) = " + toUnicode(d.quotient()) + "   " + PolynomialFormatter.formatList(d.quotientCoefficients()),
                "Reste R = " + formatNumber(d.remainder()) + " = " + px,
                "Identité " + PolynomialFormatter.formatDivisor(x, true) + "·Q(x) + R = P(x) : "
                        + ResultVerifier.verifyIdentity(d).verdict().label() + " (multiplication polynomiale)",
                "x³ − 6x² + 11x − 6 divisé par (x − 2) : reste " + formatNumber(rootCase.remainder()) + " → 2 : "
                        + rootCase.rootStatus(null).label()), true));

        DerivativeResult der = DerivativeEvaluator.evaluate(p, x);
        List<String> derivative = new ArrayList<>();
        derivative.add("Récurrence : d ← d·x + b   puis   b ← b·x + a   (d utilise l'ancienne valeur de b)");
        for (DerivativeStep s : der.steps()) {
            derivative.add("Étape " + s.number() + " : b = " + formatNumber(s.valueAfter()) + "    d = " + formatNumber(s.derivativeAfter()));
        }
        derivative.add("P'(x) = " + toUnicode(p.derivative()) + "  →  P'(" + formatNumber(x) + ") = "
                + formatNumber(der.derivative()) + "  (oracle : "
                + ResultVerifier.verifyDerivative(p, x, NumericSettings.exact()).verdict().label() + ")");
        slides.add(new Slide("6 — La dérivée en un seul parcours", "P(" + formatNumber(x) + ") = " + formatNumber(der.value())
                + " et P'(" + formatNumber(x) + ") = " + formatNumber(der.derivative()), derivative, true));

        slides.add(new Slide("7 — Quelques tests", "Résultats calculés à l'instant par le moteur", testItems(), true));

        slides.add(new Slide("8 — Conclusion", "Complexité O(n) et robustesse", List.of(
                "Horner : n multiplications + n additions → coût arithmétique O(n)",
                "Évaluation naïve : n(n+1)/2 multiplications pour les puissances → O(n²)",
                "Avec BigDecimal, le coût réel dépend aussi de la taille des nombres",
                "Robustesse : validation des entrées, limites, mode exact / précision contrôlée",
                "Vérification : oracle indépendant, propriétés aléatoires, couverture JaCoCo",
                "Des tests réussis augmentent la confiance, ils ne prouvent pas l'absence de défaut"), true));
        return List.copyOf(slides);
    }

    private static List<String> testItems() {
        List<String> items = new ArrayList<>();
        items.add(line("Normal", "x³ − 6x² + 11x − 6 en x = 2", Polynomial.of(1, -6, 11, -6), "2", "0"));
        items.add(line("Limite", "polynôme nul en x = 5", Polynomial.zero(), "5", "0"));
        items.add(line("Limite", "x = 0 donne le terme constant", Polynomial.of(2, -6, 2, -1), "0", "-1"));
        items.add(line("Limite", "x = −1 donne la somme alternée", Polynomial.of(2, -6, 2, -1), "-1", "-11"));
        items.add(line("Limite", "grands coefficients 10³⁰(x − 1)² en 1.000001", Polynomial.of(
                new BigDecimal("1e30"), new BigDecimal("-2e30"), new BigDecimal("1e30")), "1.000001", "1E+18"));
        try {
            new InputValidator().parseCoefficients("2,abc");
            items.add("Invalide : « 2,abc » ACCEPTÉ — anomalie !");
        } catch (InvalidInputException e) {
            items.add("Invalide : « 2,abc » refusé → " + e.getMessage());
        }
        BigDecimal x = new BigDecimal("1.00000000000000000001");
        Polynomial square = Polynomial.of(1, -2, 1);
        items.add("Précision : (x − 1)² en 1 + 10⁻²⁰ → exact " + formatNumber(HornerEvaluator.evaluate(square, x))
                + ", contrôlé (34 chiffres) " + formatNumber(HornerEvaluator.evaluate(square, x, NumericSettings.controlled()))
                + " : jamais confondu avec une racine exacte");
        items.add(TestReportService.readSurefireReports(Path.of("target", "surefire-reports"))
                .map(t -> "Dernière campagne Maven : " + t.tests() + " tests, " + t.failures() + " échecs, "
                        + t.errors() + " erreurs, " + t.skipped() + " ignorés")
                .orElse("Campagne Maven : rapport non disponible ici (lancer « mvn verify »)"));
        return items;
    }

    private static String line(String kind, String label, Polynomial p, String x, String expected) {
        BigDecimal value = HornerEvaluator.evaluate(p, new BigDecimal(x));
        boolean ok = value.compareTo(new BigDecimal(expected)) == 0;
        return kind + " : " + label + " → " + formatNumber(value) + (ok ? "   (attendu " + formatNumber(new BigDecimal(expected)) + " : OK)"
                : "   (attendu " + expected + " : ÉCHEC)");
    }
}
