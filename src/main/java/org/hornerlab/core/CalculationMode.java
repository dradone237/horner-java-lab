package org.hornerlab.core;

/** Mode de calcul numérique. */
public enum CalculationMode {
    /** Additions et multiplications sans arrondi intermédiaire. */
    EXACT,
    /** Chaque opération est arrondie selon un {@link java.math.MathContext}. */
    CONTROLLED
}
