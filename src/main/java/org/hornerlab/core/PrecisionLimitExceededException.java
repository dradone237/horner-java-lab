package org.hornerlab.core;

/** Signale qu'un calcul exact dépasserait la limite de taille configurée. */
public class PrecisionLimitExceededException extends ArithmeticException {

    private static final long serialVersionUID = 1L;

    private final long limitDigits;

    public PrecisionLimitExceededException(String message, long limitDigits) {
        super(message);
        this.limitDigits = limitDigits;
    }

    /** Limite configurée, en chiffres décimaux. */
    public long limitDigits() {
        return limitDigits;
    }
}
