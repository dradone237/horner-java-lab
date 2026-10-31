package org.hornerlab.core;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat du schéma de Horner étendu.
 *
 * @param polynomial      polynôme évalué
 * @param x               point d'évaluation
 * @param value           P(x)
 * @param derivative      P'(x)
 * @param steps           étapes exécutées (la première est l'initialisation)
 * @param multiplications nombre de multiplications effectuées (2n)
 * @param additions       nombre d'additions effectuées (2n)
 * @param settings        politique de calcul utilisée
 */
public record DerivativeResult(Polynomial polynomial, BigDecimal x, BigDecimal value, BigDecimal derivative,
                               List<DerivativeStep> steps, int multiplications, int additions,
                               NumericSettings settings) {

    public DerivativeResult {
        steps = List.copyOf(steps);
    }
}
