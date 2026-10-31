package org.hornerlab.examples;

import org.hornerlab.core.Polynomial;
import org.hornerlab.validation.InputValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Exemples préchargés, lus dans la ressource {@code /examples/examples.csv}. */
public final class ExampleLibrary {

    static final String RESOURCE = "/examples/examples.csv";

    private ExampleLibrary() {
    }

    /** Un exemple : polynôme, valeur de x et explication. */
    public record Example(String id, String title, Polynomial polynomial, BigDecimal x, String description) {

        @Override
        public String toString() {
            return title;
        }
    }

    /** Charge tous les exemples ; une ligne mal formée provoque une erreur explicite. */
    public static List<Example> load() {
        try (InputStream in = ExampleLibrary.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Ressource introuvable : " + RESOURCE);
            }
            return parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<Example> parse(List<String> lines) {
        InputValidator validator = new InputValidator();
        List<Example> examples = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split(";", -1);
            if (f.length != 5) {
                throw new IllegalStateException("Ligne d'exemple mal formée : " + line);
            }
            examples.add(new Example(f[0].strip(), f[1].strip(), validator.parsePolynomial(f[2]),
                    validator.parseNumber(f[3], "x"), f[4].strip()));
        }
        return List.copyOf(examples);
    }
}
