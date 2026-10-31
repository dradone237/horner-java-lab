# HORNER JAVA LAB

Implémentation, vérification et validation de l'algorithme de Horner en Java
(cours de Vérification, Validation et Tests Logiciels).

> **État : travail en cours.** Le moteur mathématique et les tests de référence sont écrits,
> mais n'ont pas encore été compilés ni exécutés (JDK 21 en cours d'installation).
> Aucun résultat de test n'est donc annoncé pour l'instant.

## Convention

Les coefficients sont rangés par **puissances décroissantes** :
`[2, -6, 2, -1]` représente P(x) = 2x³ − 6x² + 2x − 1.

## Contenu actuel

| Élément | Fichier |
|---|---|
| Polynôme immuable (normalisation des zéros dominants, polynôme nul = `[0]`) | `core/Polynomial.java` |
| Évaluation de Horner, simple et détaillée (étapes, nombre d'opérations) | `core/HornerEvaluator.java` |
| P(x) et P'(x) en un seul parcours | `core/DerivativeEvaluator.java` |
| Division synthétique par (x − r), identité P = (x − r)Q + R | `core/SyntheticDivision.java` |
| Mode exact / précision contrôlée (`MathContext`, 34 chiffres, HALF_EVEN) | `core/NumericSettings.java` |
| Tests de référence JUnit : P(3) = 5, P'(3) = 20, Q = [2, 0, 2], R = 5 | `src/test/java/org/hornerlab/` |

## Construction (une fois Java 21 installé)

```
mvn clean verify
```

## À venir

Oracle indépendant et tests par propriétés, interface en ligne de commande, interface Swing
avec mode soutenance, couverture JaCoCo, JAR exécutable, documentation académique.
