package org.hornerlab.validation;

/** Saisie utilisateur refusée ; le message, en français, indique le champ concerné et la correction attendue. */
public class InvalidInputException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidInputException(String field, String reason) {
        super(field + " : " + reason);
        this.field = field;
    }

    /** Nom du champ fautif (par exemple « coefficient n°2 » ou « x »). */
    public String field() {
        return field;
    }
}
