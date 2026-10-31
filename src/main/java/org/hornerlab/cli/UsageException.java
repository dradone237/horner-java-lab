package org.hornerlab.cli;

/** Erreur de syntaxe dans la ligne de commande (commande ou option incorrecte). */
public class UsageException extends Exception {

    private static final long serialVersionUID = 1L;

    public UsageException(String message) {
        super(message);
    }
}
