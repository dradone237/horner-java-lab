package org.hornerlab.cli;

import org.hornerlab.core.DerivativeEvaluator;
import org.hornerlab.core.DerivativeResult;
import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.HornerResult;
import org.hornerlab.core.HornerStep;
import org.hornerlab.core.InvalidPolynomialException;
import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.Polynomial;
import org.hornerlab.core.PrecisionLimitExceededException;
import org.hornerlab.core.RootStatus;
import org.hornerlab.core.SyntheticDivision;
import org.hornerlab.core.HornerEvaluator;
import org.hornerlab.examples.ExampleLibrary;
import org.hornerlab.reporting.BenchmarkService;
import org.hornerlab.reporting.TestReportService;
import org.hornerlab.validation.InputValidator;
import org.hornerlab.validation.InvalidInputException;
import org.hornerlab.validation.ResultVerifier;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.hornerlab.core.PolynomialFormatter.coefficientList;
import static org.hornerlab.core.PolynomialFormatter.formatDivisor;
import static org.hornerlab.core.PolynomialFormatter.formatList;
import static org.hornerlab.core.PolynomialFormatter.formatNumber;
import static org.hornerlab.core.PolynomialFormatter.formatOperand;
import static org.hornerlab.core.PolynomialFormatter.toAscii;

/**
 * Interface en ligne de commande. {@link #run(String...)} renvoie un code de sortie au lieu
 * d'appeler {@code System.exit}, ce qui la rend testable.
 *
 * <p>Codes de sortie : 0 succès, 1 erreur interne, 2 erreur d'utilisation, 3 entrée invalide,
 * 4 erreur de calcul (limite dépassée), 5 affichage graphique indisponible, 6 vérification en échec.
 */
public final class CommandLineApp {

    public static final int EXIT_OK = 0;
    public static final int EXIT_INTERNAL_ERROR = 1;
    public static final int EXIT_USAGE = 2;
    public static final int EXIT_INVALID_INPUT = 3;
    public static final int EXIT_CALCULATION = 4;
    public static final int EXIT_NO_DISPLAY = 5;
    public static final int EXIT_VERIFICATION_FAILED = 6;

    /** Taille maximale d'un fichier de coefficients (--coeffs-file). */
    static final long MAX_FILE_BYTES = 3_000_000;
    /** Au-delà de ce nombre de coefficients, la formule n'est pas affichée en entier. */
    private static final int MAX_FORMULA_COEFFICIENTS = 25;

    private final PrintStream out;
    private final PrintStream err;
    private final InputValidator validator = new InputValidator();

    public CommandLineApp(PrintStream out, PrintStream err) {
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
    }

    /** Exécute une commande et renvoie le code de sortie. */
    public int run(String... args) {
        try {
            ArgumentParser.ParsedArguments a = ArgumentParser.parse(args);
            return switch (a.command()) {
                case "help" -> help();
                case "version" -> version();
                case "eval" -> eval(a);
                case "steps" -> steps(a);
                case "derivative" -> derivative(a);
                case "divide" -> divide(a);
                case "verify" -> verify(a);
                case "selftest" -> selftest(a);
                case "benchmark" -> benchmark(a);
                case "examples" -> examples();
                case "gui" -> {
                    err.println("L'interface graphique se lance avec : java -jar horner-java-lab-1.0.0.jar (sans argument) ou gui.");
                    yield EXIT_USAGE;
                }
                default -> throw new UsageException("commande non prise en charge : " + a.command());
            };
        } catch (UsageException e) {
            err.println("Erreur d'utilisation : " + e.getMessage());
            err.println("Tapez --help pour afficher l'aide.");
            return EXIT_USAGE;
        } catch (InvalidInputException | InvalidPolynomialException e) {
            err.println("Entrée invalide - " + e.getMessage());
            return EXIT_INVALID_INPUT;
        } catch (PrecisionLimitExceededException e) {
            err.println("Limite de calcul atteinte : " + e.getMessage());
            return EXIT_CALCULATION;
        } catch (ArithmeticException e) {
            err.println("Erreur de calcul : " + e.getMessage());
            return EXIT_CALCULATION;
        } catch (UncheckedIOException e) {
            err.println("Erreur de fichier : " + e.getCause().getMessage());
            return EXIT_INVALID_INPUT;
        } catch (RuntimeException e) {
            err.println("Erreur interne inattendue : " + e);
            return EXIT_INTERNAL_ERROR;
        }
    }

    // ---------------------------------------------------------------- commandes

    private int eval(ArgumentParser.ParsedArguments a) throws UsageException {
        Polynomial p = polynomial(a);
        BigDecimal x = validator.parseNumber(a.required("x"), "x");
        NumericSettings settings = settings(a);
        BigDecimal value = HornerEvaluator.evaluate(p, x, settings);
        printPolynomial(p);
        printMode(settings);
        out.println("P(" + formatNumber(x) + ") = " + formatNumber(value));
        if (value.signum() == 0) {
            out.println(settings.isExact()
                    ? "Remarque : P(" + formatNumber(x) + ") = 0, donc " + formatNumber(x)
                    + " est une racine de P (les autres racines ne sont pas calculées)."
                    : "Remarque : résultat nul après arrondi ; refaites le calcul en mode exact pour certifier une racine.");
        }
        return EXIT_OK;
    }

    private int steps(ArgumentParser.ParsedArguments a) throws UsageException {
        Polynomial p = polynomial(a);
        BigDecimal x = validator.parseNumber(a.required("x"), "x");
        NumericSettings settings = settings(a);
        HornerResult r = HornerEvaluator.evaluateWithSteps(p, x, settings);
        printPolynomial(p);
        out.println("Coefficients (puissances décroissantes) : " + coefficientList(p));
        out.println("x = " + formatNumber(x));
        printMode(settings);
        for (HornerStep s : r.steps()) {
            if (s.isInitialization()) {
                out.println("Étape " + s.number() + " : accumulateur = a" + s.power() + " = "
                        + formatNumber(s.accumulatorAfter()) + " (coefficient dominant)");
            } else {
                out.println("Étape " + s.number() + " : " + formatOperand(s.accumulatorBefore()) + " * "
                        + formatOperand(x) + " + " + formatOperand(s.coefficient()) + " = "
                        + formatNumber(s.accumulatorAfter()));
            }
        }
        out.println("Résultat : P(" + formatNumber(x) + ") = " + formatNumber(r.value()));
        out.println("Opérations : " + r.multiplications() + " multiplications, " + r.additions() + " additions");
        return EXIT_OK;
    }

    private int derivative(ArgumentParser.ParsedArguments a) throws UsageException {
        Polynomial p = polynomial(a);
        BigDecimal x = validator.parseNumber(a.required("x"), "x");
        NumericSettings settings = settings(a);
        DerivativeResult r = DerivativeEvaluator.evaluate(p, x, settings);
        printPolynomial(p);
        if (p.size() <= MAX_FORMULA_COEFFICIENTS) {
            out.println("P'(x) = " + toAscii(p.derivative()));
        }
        printMode(settings);
        out.println("P(" + formatNumber(x) + ") = " + formatNumber(r.value()));
        out.println("P'(" + formatNumber(x) + ") = " + formatNumber(r.derivative()));
        out.println("Opérations : " + r.multiplications() + " multiplications, " + r.additions()
                + " additions (un seul parcours des coefficients)");
        return EXIT_OK;
    }

    private int divide(ArgumentParser.ParsedArguments a) throws UsageException {
        Polynomial p = polynomial(a);
        BigDecimal r = validator.parseNumber(a.required("root"), "root");
        NumericSettings settings = settings(a);
        DivisionResult d = SyntheticDivision.divide(p, r, settings);
        printPolynomial(p);
        out.println("Diviseur : " + formatDivisor(r, false));
        printMode(settings);
        out.println("Quotient : " + formatList(d.quotientCoefficients()));
        if (d.quotient().size() <= MAX_FORMULA_COEFFICIENTS) {
            out.println("Q(x) = " + toAscii(d.quotient()));
        }
        out.println("Reste : " + formatNumber(d.remainder()));

        ResultVerifier.Check identity = ResultVerifier.verifyIdentity(d);
        ResultVerifier.Check remainder = ResultVerifier.verifyRemainder(d);
        out.println("Identité P(x) = " + formatDivisor(r, false) + " * Q(x) + R : " + identity.verdict().label()
                + " (écart max " + formatNumber(identity.difference()) + ")");
        out.println("Reste = P(" + formatNumber(r) + ") (oracle indépendant) : " + remainder.verdict().label());

        RootStatus status;
        if (a.has("tolerance")) {
            BigDecimal tolerance = validator.parseNumber(a.required("tolerance"), "tolerance");
            if (tolerance.signum() < 0) {
                throw new InvalidInputException("tolerance", "doit être positive ou nulle.");
            }
            status = d.rootStatus(tolerance);
        } else {
            status = ResultVerifier.classifyRoot(d);
        }
        out.println("Statut de r = " + formatNumber(r) + " : " + status.label());
        return identity.passed() && remainder.passed() ? EXIT_OK : EXIT_VERIFICATION_FAILED;
    }

    private int verify(ArgumentParser.ParsedArguments a) throws UsageException {
        Polynomial p = polynomial(a);
        BigDecimal x = validator.parseNumber(a.required("x"), "x");
        NumericSettings settings = settings(a);
        printPolynomial(p);
        out.println("x = " + formatNumber(x) + " ; mode " + settings.describe());
        List<ResultVerifier.Check> checks = ResultVerifier.verifyAll(p, x, settings);
        int identical = 0;
        int close = 0;
        int mismatch = 0;
        for (ResultVerifier.Check c : checks) {
            out.println("[" + c.verdict().label() + "] " + c.name() + " : calculé " + formatNumber(c.computed())
                    + " | référence " + formatNumber(c.reference()) + " | écart " + formatNumber(c.difference())
                    + " | tolérance " + formatNumber(c.tolerance()));
            switch (c.verdict()) {
                case IDENTICAL -> identical++;
                case WITHIN_TOLERANCE -> close++;
                case MISMATCH -> mismatch++;
            }
        }
        out.println("Bilan : " + checks.size() + " contrôles - " + identical + " identiques, " + close
                + " proches sous tolérance, " + mismatch + " incohérents");
        return mismatch == 0 ? EXIT_OK : EXIT_VERIFICATION_FAILED;
    }

    private int selftest(ArgumentParser.ParsedArguments a) {
        long seed = a.option("seed").map(s -> validator.parseLong(s, "seed", Long.MIN_VALUE, Long.MAX_VALUE))
                .orElse(TestReportService.DEFAULT_SEED);
        int count = a.option("count").map(s -> validator.parseInteger(s, "count", 0, 100_000)).orElse(500);
        TestReportService.SelfTestReport report = new TestReportService().runSelfTest(seed, count);
        out.print(report.toText());
        a.option("export").ifPresent(path -> {
            Path written = TestReportService.writeMarkdown(report, Path.of(path));
            out.println("Rapport exporté : " + written.toAbsolutePath());
        });
        return report.allPassed() ? EXIT_OK : EXIT_VERIFICATION_FAILED;
    }

    private int benchmark(ArgumentParser.ParsedArguments a) {
        List<Integer> sizes = a.option("sizes")
                .map(s -> validator.parseIntegerList(s, "sizes", 1, 10_001))
                .orElse(List.of(10, 100, 1_000));
        int repeat = a.option("repeat").map(s -> validator.parseInteger(s, "repeat", 1, 1_000)).orElse(10);
        int warmup = a.option("warmup").map(s -> validator.parseInteger(s, "warmup", 0, 1_000)).orElse(5);
        long seed = a.option("seed").map(s -> validator.parseLong(s, "seed", Long.MIN_VALUE, Long.MAX_VALUE))
                .orElse(TestReportService.DEFAULT_SEED);
        out.println("Benchmark en cours (tailles " + sizes + ", " + repeat + " répétitions, " + warmup
                + " tours de chauffe)...");
        BenchmarkService.Report report = new BenchmarkService().run(sizes, repeat, warmup, seed);
        String markdown = report.toMarkdown();
        out.print(markdown);
        a.option("export").ifPresent(path -> {
            try {
                Path target = Path.of(path);
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                Files.writeString(target, markdown, StandardCharsets.UTF_8);
                out.println("Rapport exporté : " + target.toAbsolutePath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return report.allConsistent() ? EXIT_OK : EXIT_VERIFICATION_FAILED;
    }

    private int examples() {
        out.println("Exemples préchargés (également disponibles dans l'interface graphique) :");
        for (ExampleLibrary.Example e : ExampleLibrary.load()) {
            out.println("- " + e.title() + " : P(x) = " + toAscii(e.polynomial()) + ", x = " + formatNumber(e.x()));
            out.println("    java -jar target/horner-java-lab-1.0.0.jar steps --coeffs \""
                    + String.join(",", e.polynomial().coefficients().stream().map(c -> formatNumber(c)).toList())
                    + "\" --x " + formatNumber(e.x()));
        }
        return EXIT_OK;
    }

    private int help() {
        out.println("""
                HORNER JAVA LAB - Évaluation et vérification des polynômes

                Usage : java -jar target/horner-java-lab-1.0.0.jar [commande] [options]
                Sans argument, l'interface graphique s'ouvre (si un affichage est disponible).

                Commandes :
                  eval        Évalue P(x) par la méthode de Horner
                  steps       Affiche chaque étape du calcul de Horner
                  derivative  Calcule P(x) et P'(x) en un seul parcours
                  divide      Division synthétique de P(x) par (x - r)
                  verify      Compare Horner à un oracle indépendant (somme des termes)
                  selftest    Campagne de validation intégrée (cas de référence + cas aléatoires)
                  benchmark   Mesure Horner face à l'évaluation naïve
                  examples    Liste les exemples préchargés
                  gui         Ouvre l'interface graphique

                Options :
                  --coeffs "2,-6,2,-1"   coefficients par puissances DÉCROISSANTES, séparés par des virgules
                                         (le point est le séparateur décimal : "1.5,-2.25,0,3")
                  --coeffs-file <chemin> coefficients lus dans un fichier texte (même syntaxe)
                  --x <valeur>           point d'évaluation (ex. 3, -1, 0.5, 1e-3, --x=-2)
                  --root <valeur>        r pour la division par (x - r)
                  --mode exact|controle  mode de calcul (exact par défaut)
                  --precision <n>        chiffres significatifs du mode contrôlé (34 par défaut)
                  --tolerance <valeur>   tolérance pour qualifier une racine approximative (divide)
                  --seed <n>, --count <n>, --export <fichier>   (selftest)
                  --sizes 10,100,1000, --repeat <n>, --warmup <n>, --export <fichier>   (benchmark)

                Exemples :
                  java -jar target/horner-java-lab-1.0.0.jar eval --coeffs "2,-6,2,-1" --x 3
                  java -jar target/horner-java-lab-1.0.0.jar steps --coeffs "2,-6,2,-1" --x 3
                  java -jar target/horner-java-lab-1.0.0.jar derivative --coeffs "2,-6,2,-1" --x 3
                  java -jar target/horner-java-lab-1.0.0.jar divide --coeffs "2,-6,2,-1" --root 3

                Codes de sortie : 0 succès, 2 erreur d'utilisation, 3 entrée invalide,
                4 limite de calcul, 5 affichage indisponible, 6 vérification en échec, 1 erreur interne.""");
        return EXIT_OK;
    }

    private int version() {
        String v = CommandLineApp.class.getPackage().getImplementationVersion();
        out.println("Horner Java Lab " + (v == null ? "(version de développement)" : v));
        return EXIT_OK;
    }

    // ---------------------------------------------------------------- utilitaires

    private Polynomial polynomial(ArgumentParser.ParsedArguments a) throws UsageException {
        boolean inline = a.has("coeffs");
        boolean file = a.has("coeffs-file");
        if (inline == file) {
            throw new UsageException("indiquez les coefficients avec --coeffs \"2,-6,2,-1\" ou --coeffs-file <chemin> (une seule des deux options).");
        }
        if (inline) {
            return validator.parsePolynomial(a.required("coeffs"));
        }
        Path path = Path.of(a.required("coeffs-file"));
        try {
            if (Files.size(path) > MAX_FILE_BYTES) {
                throw new InvalidInputException("coeffs-file", "fichier trop volumineux (maximum " + MAX_FILE_BYTES + " octets).");
            }
            String content = Files.readString(path, StandardCharsets.UTF_8).replaceAll("\\s*\\R\\s*", "");
            return validator.parsePolynomial(content);
        } catch (IOException e) {
            throw new UncheckedIOException(new IOException("impossible de lire « " + path + " » (" + e.getClass().getSimpleName() + ")", e));
        }
    }

    private NumericSettings settings(ArgumentParser.ParsedArguments a) throws UsageException {
        String mode = a.option("mode").map(m -> m.strip().toLowerCase(Locale.ROOT)).orElse(null);
        boolean controlled;
        if (mode == null) {
            controlled = a.has("precision");
        } else {
            controlled = switch (mode) {
                case "exact", "exacte" -> false;
                case "controle", "contrôle", "controlee", "contrôlée", "controlled" -> true;
                default -> throw new UsageException("mode inconnu « " + mode + " » : utilisez exact ou controle.");
            };
        }
        if (!controlled) {
            if (a.has("precision")) {
                throw new UsageException("--precision n'a de sens qu'en mode contrôlé (--mode controle).");
            }
            return NumericSettings.exact();
        }
        int precision = a.option("precision")
                .map(s -> validator.parseInteger(s, "precision", 1, NumericSettings.MAX_PRECISION))
                .orElse(NumericSettings.DEFAULT_PRECISION);
        return NumericSettings.controlled(precision);
    }

    private void printPolynomial(Polynomial p) {
        if (p.size() <= MAX_FORMULA_COEFFICIENTS) {
            out.println("P(x) = " + toAscii(p));
        } else {
            out.println("P(x) : polynôme de degré " + p.degree() + " (" + p.size() + " coefficients)");
        }
    }

    private void printMode(NumericSettings settings) {
        if (!settings.isExact()) {
            out.println("Mode : " + settings.describe());
        }
    }
}
