package org.hornerlab.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Analyse des arguments de la ligne de commande : {@code <commande> --option valeur ...}.
 *
 * <p>La valeur qui suit une option est toujours consommée, même si elle commence par « - » :
 * {@code --x -1} et {@code --coeffs "-1,2"} sont donc acceptés. La forme {@code --x=-1} l'est aussi.
 */
public final class ArgumentParser {

    private static final Set<String> POLYNOMIAL_OPTIONS = Set.of("coeffs", "coeffs-file", "mode", "precision");

    private static final Map<String, Set<String>> OPTIONS = Map.of(
            "eval", union(POLYNOMIAL_OPTIONS, "x"),
            "steps", union(POLYNOMIAL_OPTIONS, "x"),
            "derivative", union(POLYNOMIAL_OPTIONS, "x"),
            "divide", union(POLYNOMIAL_OPTIONS, "root", "tolerance"),
            "verify", union(POLYNOMIAL_OPTIONS, "x"),
            "selftest", Set.of("seed", "count", "export"),
            "benchmark", Set.of("sizes", "repeat", "warmup", "seed", "export"),
            "examples", Set.of(),
            "gui", Set.of(),
            "help", Set.of());

    /** Commandes reconnues, dans l'ordre de l'aide. */
    public static final List<String> COMMANDS =
            List.of("eval", "steps", "derivative", "divide", "verify", "selftest", "benchmark", "examples", "gui", "help");

    private ArgumentParser() {
    }

    /** Arguments analysés. */
    public record ParsedArguments(String command, Map<String, String> options) {

        public ParsedArguments {
            options = Map.copyOf(options);
        }

        public Optional<String> option(String name) {
            return Optional.ofNullable(options.get(name));
        }

        public boolean has(String name) {
            return options.containsKey(name);
        }

        public String required(String name) throws UsageException {
            String value = options.get(name);
            if (value == null) {
                throw new UsageException("l'option --" + name + " est obligatoire pour la commande " + command + ".");
            }
            return value;
        }
    }

    /** Analyse les arguments ; aucun argument ou {@code --help} donne la commande {@code help}. */
    public static ParsedArguments parse(String... args) throws UsageException {
        if (args == null || args.length == 0) {
            return new ParsedArguments("help", Map.of());
        }
        String first = args[0].strip().toLowerCase(Locale.ROOT);
        if (Set.of("--help", "-h", "/?", "help").contains(first)) {
            return new ParsedArguments("help", Map.of());
        }
        if (Set.of("--version", "-v", "version").contains(first)) {
            return new ParsedArguments("version", Map.of());
        }
        Set<String> allowed = OPTIONS.get(first);
        if (allowed == null) {
            throw new UsageException("commande inconnue « " + args[0] + " ». Commandes disponibles : "
                    + String.join(", ", COMMANDS) + ".");
        }
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 1; i < args.length; i++) {
            String token = args[i];
            if (token.equals("--help") || token.equals("-h")) {
                return new ParsedArguments("help", Map.of());
            }
            if (!token.startsWith("--") || token.length() == 2) {
                throw new UsageException("argument inattendu « " + token + " ». Les options s'écrivent --nom valeur.");
            }
            String name;
            String value;
            int eq = token.indexOf('=');
            if (eq > 0) {
                name = token.substring(2, eq);
                value = token.substring(eq + 1);
            } else {
                name = token.substring(2);
                if (i + 1 >= args.length) {
                    throw new UsageException("l'option --" + name + " attend une valeur.");
                }
                value = args[++i];
                if (value.startsWith("--")) {
                    throw new UsageException("l'option --" + name + " attend une valeur, mais « " + value + " » a été trouvé.");
                }
            }
            name = name.toLowerCase(Locale.ROOT);
            if (!allowed.contains(name)) {
                throw new UsageException("option inconnue --" + name + " pour la commande " + first
                        + (allowed.isEmpty() ? " (aucune option acceptée)." : ". Options acceptées : --"
                        + String.join(", --", allowed.stream().sorted().toList()) + "."));
            }
            if (options.containsKey(name)) {
                throw new UsageException("l'option --" + name + " est répétée.");
            }
            options.put(name, value);
        }
        return new ParsedArguments(first, options);
    }

    private static Set<String> union(Set<String> base, String... extra) {
        return java.util.stream.Stream.concat(base.stream(), java.util.Arrays.stream(extra))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
