package org.hornerlab;

import org.hornerlab.cli.ArgumentParser;
import org.hornerlab.cli.CommandLineApp;
import org.hornerlab.cli.UsageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests d'intégration de la ligne de commande : sorties, messages d'erreur et codes de sortie. */
class CommandLineAppTest {

    /** Sortie standard, sortie d'erreur et code de sortie d'une exécution. */
    record Run(int code, String out, String err) {
        List<String> lines() {
            return out.lines().toList();
        }
    }

    static Run run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = new CommandLineApp(new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8)).run(args);
        return new Run(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("eval --coeffs \"2,-6,2,-1\" --x 3 → P(3) = 5")
    void eval() {
        Run r = run("eval", "--coeffs", "2,-6,2,-1", "--x", "3");
        assertEquals(CommandLineApp.EXIT_OK, r.code(), r.err());
        assertTrue(r.lines().contains("P(3) = 5"), r.out());
        assertTrue(r.lines().contains("P(x) = 2x^3 - 6x^2 + 2x - 1"), r.out());
    }

    @Test
    @DisplayName("eval sur une racine : P(2) = 0 et remarque sur la racine")
    void evalRoot() {
        Run r = run("eval", "--coeffs", "1,-6,11,-6", "--x", "2");
        assertTrue(r.lines().contains("P(2) = 0"), r.out());
        assertTrue(r.out().contains("2 est une racine de P"), r.out());
    }

    @Test
    @DisplayName("steps : quatre étapes réelles et nombre d'opérations")
    void steps() {
        Run r = run("steps", "--coeffs", "2,-6,2,-1", "--x", "3");
        assertEquals(0, r.code());
        assertTrue(r.lines().contains("Étape 1 : accumulateur = a3 = 2 (coefficient dominant)"), r.out());
        assertTrue(r.lines().contains("Étape 2 : 2 * 3 + (-6) = 0"), r.out());
        assertTrue(r.lines().contains("Étape 3 : 0 * 3 + 2 = 2"), r.out());
        assertTrue(r.lines().contains("Étape 4 : 2 * 3 + (-1) = 5"), r.out());
        assertTrue(r.lines().contains("Résultat : P(3) = 5"), r.out());
        assertTrue(r.lines().contains("Opérations : 3 multiplications, 3 additions"), r.out());
    }

    @Test
    @DisplayName("derivative → P(3) = 5 et P'(3) = 20")
    void derivative() {
        Run r = run("derivative", "--coeffs", "2,-6,2,-1", "--x", "3");
        assertEquals(0, r.code());
        assertTrue(r.lines().contains("P(3) = 5"), r.out());
        assertTrue(r.lines().contains("P'(3) = 20"), r.out());
        assertTrue(r.lines().contains("P'(x) = 6x^2 - 12x + 2"), r.out());
    }

    @Test
    @DisplayName("divide --root 3 → Quotient : [2, 0, 2], Reste : 5")
    void divide() {
        Run r = run("divide", "--coeffs", "2,-6,2,-1", "--root", "3");
        assertEquals(0, r.code(), r.err());
        assertTrue(r.lines().contains("Quotient : [2, 0, 2]"), r.out());
        assertTrue(r.lines().contains("Reste : 5"), r.out());
        assertTrue(r.out().contains("Statut de r = 3 : n'est pas une racine"), r.out());
        Run root = run("divide", "--coeffs", "1,-6,11,-6", "--root", "2");
        assertTrue(root.out().contains("racine exacte vérifiée"), root.out());
        Run tol = run("divide", "--coeffs", "3,-1", "--root", "0.3333333333", "--mode", "controle", "--tolerance", "1e-9");
        assertTrue(tol.out().contains("racine approximative"), tol.out());
        assertEquals(CommandLineApp.EXIT_INVALID_INPUT, run("divide", "--coeffs", "3,-1", "--root", "1", "--mode", "controle", "--tolerance", "-1").code());
    }

    @Test
    @DisplayName("Options négatives et notation scientifique : --x -1, --x=-1, --x 1e2")
    void negativeAndScientificValues() {
        assertTrue(run("eval", "--coeffs", "2,-6,2,-1", "--x", "-1").lines().contains("P(-1) = -11"));
        assertTrue(run("eval", "--coeffs=2,-6,2,-1", "--x=-1").lines().contains("P(-1) = -11"));
        assertTrue(run("eval", "--coeffs", "1,0", "--x", "1e2").lines().contains("P(100) = 100"));
        assertTrue(run("eval", "--coeffs", "-1.5e1,2", "--x", "2").lines().contains("P(2) = -28"));
    }

    @Test
    @DisplayName("Mode contrôlé : précision affichée et appliquée")
    void controlledMode() {
        Run r = run("eval", "--coeffs", "1,-2,1", "--x", "1.00000000000000000001", "--mode", "controle");
        assertTrue(r.out().contains("précision contrôlée (34 chiffres"), r.out());
        assertTrue(r.lines().contains("P(1.00000000000000000001) = 0"), r.out());
        assertTrue(r.out().contains("mode exact pour certifier"), r.out());
        Run exact = run("eval", "--coeffs", "1,-2,1", "--x", "1.00000000000000000001");
        assertTrue(exact.lines().contains("P(1.00000000000000000001) = 1E-40"), exact.out());
        assertTrue(run("eval", "--coeffs", "1,1", "--x", "2", "--precision", "5").out().contains("5 chiffres"));
    }

    @Test
    @DisplayName("verify : 7 contrôles identiques en mode exact")
    void verify() {
        Run r = run("verify", "--coeffs", "2,-6,2,-1", "--x", "3");
        assertEquals(0, r.code());
        assertTrue(r.out().contains("Bilan : 7 contrôles - 7 identiques, 0 proches sous tolérance, 0 incohérents"), r.out());
        Run c = run("verify", "--coeffs", "1.1,-2.3,3.7", "--x", "1.234567", "--mode", "controle", "--precision", "6");
        assertEquals(0, c.code(), c.out());
    }

    @Test
    @DisplayName("--coeffs-file : lecture d'un fichier de 1 000 coefficients")
    void coefficientsFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("coeffs.txt");
        Files.writeString(file, String.join(",\n", Collections.nCopies(1000, "1")));
        Run r = run("eval", "--coeffs-file", file.toString(), "--x", "1");
        assertEquals(0, r.code(), r.err());
        assertTrue(r.lines().contains("P(1) = 1000"), r.out());
        assertTrue(r.out().contains("polynôme de degré 999"), r.out());
        assertEquals(CommandLineApp.EXIT_INVALID_INPUT, run("eval", "--coeffs-file", dir.resolve("absent.txt").toString(), "--x", "1").code());
    }

    @Test
    @DisplayName("Aide et version")
    void help() {
        for (String arg : new String[] {"--help", "-h", "help"}) {
            Run r = run(arg);
            assertEquals(0, r.code());
            assertTrue(r.out().contains("Usage : java -jar"), r.out());
        }
        assertEquals(0, run().code());
        assertEquals(0, run("eval", "--help").code());
        assertTrue(run("--version").out().contains("Horner Java Lab"));
        assertTrue(run("examples").out().contains("Exemple de référence"));
    }

    @ParameterizedTest(name = "erreur d'utilisation : {0}")
    @ValueSource(strings = {"inconnue", "eval --coeffs 1,2", "eval --x 3", "eval --coeffs 1 --coeffs 2 --x 1",
            "eval --coeffs 1 --x", "eval --coeffs 1 --x 1 --root 2", "eval coeffs 1", "eval --coeffs 1 --x --mode",
            "eval --coeffs 1 --x 1 --mode turbo", "eval --coeffs 1 --x 1 --mode exact --precision 5", "gui",
            "eval --coeffs 1 --coeffs-file f --x 1", "examples --x 1"})
    void usageErrors(String commandLine) {
        Run r = run(commandLine.split(" "));
        assertEquals(CommandLineApp.EXIT_USAGE, r.code(), r.out() + r.err());
        assertTrue(r.err().contains("Erreur d'utilisation") || r.err().contains("interface graphique"), r.err());
    }

    @ParameterizedTest(name = "entrée invalide : {0}")
    @ValueSource(strings = {"abc", "1,,2", "1;2", "1,NaN", "1,5e99999", " "})
    void invalidCoefficients(String coefficients) {
        Run r = run("eval", "--coeffs", coefficients, "--x", "3");
        assertEquals(CommandLineApp.EXIT_INVALID_INPUT, r.code(), r.err());
        assertTrue(r.err().startsWith("Entrée invalide"), r.err());
    }

    @Test
    @DisplayName("x invalide, précision hors bornes, trop de coefficients")
    void otherInvalidInputs() {
        assertEquals(CommandLineApp.EXIT_INVALID_INPUT, run("eval", "--coeffs", "1,2", "--x", "1,5").code());
        assertEquals(CommandLineApp.EXIT_INVALID_INPUT, run("eval", "--coeffs", "1,2", "--x", "abc").code());
        assertEquals(CommandLineApp.EXIT_INVALID_INPUT, run("eval", "--coeffs", "1,2", "--x", "1", "--precision", "0").code());
        String tooMany = String.join(",", Collections.nCopies(10_002, "1"));
        assertEquals(CommandLineApp.EXIT_INVALID_INPUT, run("eval", "--coeffs", tooMany, "--x", "1").code());
    }

    @Test
    @DisplayName("Limite de calcul exact dépassée → code 4 et conseil")
    void calculationLimit() {
        // x à 190 chiffres : environ 190 chiffres de plus par itération, la limite de 200 000 est franchie
        // vers la 1 050e itération (sur 2 000).
        String coefficients = String.join(",", Collections.nCopies(2_001, "1"));
        String x = "1." + "123456789".repeat(21);
        Run r = run("eval", "--coeffs", coefficients, "--x", x);
        assertEquals(CommandLineApp.EXIT_CALCULATION, r.code(), r.err());
        assertTrue(r.err().contains("précision contrôlée"), r.err());
    }

    @Test
    @DisplayName("selftest et benchmark exécutés réellement")
    void selftestAndBenchmark(@TempDir Path dir) {
        Run s = run("selftest", "--count", "50", "--seed", "1", "--export", dir.resolve("s.md").toString());
        assertEquals(0, s.code(), s.out());
        assertTrue(s.out().contains("0 échoués"), s.out());
        assertTrue(Files.exists(dir.resolve("s.md")));
        Run b = run("benchmark", "--sizes", "10,20", "--repeat", "2", "--warmup", "1", "--export", dir.resolve("b/b.md").toString());
        assertEquals(0, b.code(), b.out() + b.err());
        assertTrue(Files.exists(dir.resolve("b/b.md")));
        assertEquals(CommandLineApp.EXIT_INVALID_INPUT, run("benchmark", "--sizes", "0").code());
    }

    @Test
    @DisplayName("ArgumentParser : formes --nom valeur et --nom=valeur")
    void argumentParser() throws UsageException {
        ArgumentParser.ParsedArguments a = ArgumentParser.parse("divide", "--coeffs", "-1,2", "--root=-3");
        assertEquals("divide", a.command());
        assertEquals("-1,2", a.option("coeffs").orElseThrow());
        assertEquals("-3", a.required("root"));
        assertThrows(UsageException.class, () -> a.required("x"));
        assertEquals("help", ArgumentParser.parse((String[]) null).command());
        assertEquals("version", ArgumentParser.parse("-v").command());
        assertThrows(UsageException.class, () -> ArgumentParser.parse("eval", "--"));
    }
}
