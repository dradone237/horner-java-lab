package org.hornerlab.core;

/** Signale une liste de coefficients qui ne définit pas un polynôme valide. */
public class InvalidPolynomialException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public InvalidPolynomialException(String message) {
        super(message);
    }
}
