package org.hornerlab;

import org.hornerlab.cli.CommandLineApp;
import org.hornerlab.gui.HornerSwingApp;

/**
 * Point d'entrée du JAR.
 * <ul>
 *   <li>sans argument (ou {@code gui}) : ouvre l'interface graphique si un affichage est disponible,
 *       sinon affiche l'aide de la ligne de commande ;</li>
 *   <li>avec une commande : exécute la CLI et termine avec son code de sortie.</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean guiRequested = args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("gui"));
        if (guiRequested) {
            if (HornerSwingApp.launchIfPossible()) {
                return;
            }
            System.out.println("Interface graphique indisponible (environnement sans affichage). Aide de la ligne de commande :");
            System.out.println();
            new CommandLineApp(System.out, System.err).run("--help");
            if (args.length == 1) {
                System.exit(CommandLineApp.EXIT_NO_DISPLAY);
            }
            return;
        }
        System.exit(new CommandLineApp(System.out, System.err).run(args));
    }
}
