package org.hornerlab.validation;

/**
 * Limites appliquées aux saisies de la CLI et de la GUI.
 *
 * @param maxCoefficients   nombre maximal de coefficients (degré maximal + 1)
 * @param maxNumberLength   longueur maximale d'un nombre saisi, en caractères
 * @param maxScaleMagnitude valeur absolue maximale de l'échelle décimale (≈ exposant)
 * @param maxInputLength    longueur maximale d'une liste de coefficients, en caractères
 */
public record InputLimits(int maxCoefficients, int maxNumberLength, int maxScaleMagnitude, int maxInputLength) {

    /** Limites de la CLI : jusqu'à 10 001 coefficients (degré 10 000). */
    public static final InputLimits DEFAULT = new InputLimits(10_001, 200, 1_000, 2_500_000);

    /** Limites de la GUI : grille de saisie jusqu'au degré 30, lisible au vidéoprojecteur. */
    public static final InputLimits GUI = new InputLimits(31, 100, 1_000, 5_000);

    public InputLimits {
        if (maxCoefficients < 1 || maxNumberLength < 1 || maxScaleMagnitude < 0 || maxInputLength < 1) {
            throw new IllegalArgumentException("Limites de saisie invalides.");
        }
    }
}
