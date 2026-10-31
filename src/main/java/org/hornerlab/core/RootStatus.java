package org.hornerlab.core;

/** Statut d'une valeur r vis-à-vis d'un polynôme P, d'après le reste de la division par (x − r). */
public enum RootStatus {
    EXACT_ROOT("racine exacte vérifiée"),
    APPROXIMATE_ROOT("racine approximative selon la tolérance"),
    NOT_A_ROOT("n'est pas une racine");

    private final String label;

    RootStatus(String label) {
        this.label = label;
    }

    /** Libellé en français. */
    public String label() {
        return label;
    }
}
