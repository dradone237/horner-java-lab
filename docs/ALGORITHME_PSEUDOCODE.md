# Algorithmes : pseudo-code, invariants et terminaison

Notation : les coefficients sont `a[0..n]` en **puissances décroissantes** (`a[0]` = aₙ, `a[n]` = a₀).

## 1. Évaluation de Horner — `HornerEvaluator`

```
fonction HORNER(a[0..n], x)
    acc ← a[0]
    pour i de 1 à n faire
        acc ← acc × x + a[i]
    fin pour
    retourner acc
```

- **Invariant** (vrai après le tour i) : acc = a[0]·xⁱ + a[1]·xⁱ⁻¹ + … + a[i], c'est-à-dire la valeur en x du polynôme formé des i + 1 premiers coefficients.
  - Initialisation (i = 0) : acc = a[0]. ✔
  - Conservation : si acc = Qᵢ₋₁(x), alors acc·x + a[i] = Qᵢ(x), car Qᵢ(t) = Qᵢ₋₁(t)·t + a[i]. ✔
  - Sortie (i = n) : acc = Qₙ(x) = P(x). ✔
- **Terminaison** : la boucle `pour` fait exactement n tours.
- **Coût** : n multiplications et n additions.
- **Implémentation** : une seule boucle (`run`) sert à la fois au calcul et à la trace des étapes. La trace correspond donc toujours aux opérations réellement faites.

## 2. Horner étendu : P(x) et P'(x) — `DerivativeEvaluator`

```
fonction HORNER_DERIVEE(a[0..n], x)
    b ← a[0] ; d ← 0
    pour i de 1 à n faire
        d ← d × x + b        // utilise l'ANCIENNE valeur de b
        b ← b × x + a[i]
    fin pour
    retourner (b, d)          // b = P(x), d = P'(x)
```

- **Invariant** : b = Qᵢ(x) et d = Qᵢ'(x). En dérivant Qᵢ(t) = Qᵢ₋₁(t)·t + a[i], on obtient Qᵢ'(t) = Qᵢ₋₁'(t)·t + Qᵢ₋₁(t) : c'est exactement la mise à jour de d, à condition d'utiliser l'ancienne valeur de b.
- **Piège** : inverser les deux lignes donne un résultat faux. Pour x² en x = 3, on obtiendrait 18 au lieu de 6 ; le test `updateOrderMatters` détecte cette erreur.
- **Coût** : 2n multiplications et 2n additions, en un seul parcours.

## 3. Division synthétique par (x − r) — `SyntheticDivision`

```
fonction DIVISION_SYNTHETIQUE(a[0..n], r)
    si n = 0 alors                       // polynôme constant
        retourner (quotient = [0], reste = a[0])
    b[0] ← a[0]
    pour i de 1 à n faire
        b[i] ← b[i−1] × r + a[i]
    fin pour
    retourner (quotient = b[0..n−1], reste = b[n])
```

- C'est **exactement** le schéma de Horner en r : le programme réutilise la trace de `HornerEvaluator`.
- **Identité** : P(x) = (x − r)·Q(x) + R, vérifiée par une multiplication polynomiale exacte, indépendante de Horner.
- **Reste** : R = P(r), vérifié par l'oracle (somme des termes).
- **Quotient nul** : pour un polynôme constant, le quotient est le polynôme nul `[0]`.
- **Statut de racine** :
  - mode exact : R = 0 donne une *racine exacte vérifiée* ; sinon *non racine*, même si R est minuscule ;
  - mode contrôlé : |R| ≤ tolérance donne une *racine approximative* ; une racine n'est jamais certifiée exacte après des arrondis.

## 4. Oracle indépendant — `ReferenceEvaluator`

```
fonction ORACLE(P, x)
    s ← 0
    pour k de 0 à n faire
        s ← s + a_k × x^k        // x^k par BigDecimal.pow, jamais par Horner
    retourner s
```

## 5. Conditions d'arrêt et cas limites

| Cas | Comportement |
|---|---|
| Liste vide ou `null` | `InvalidPolynomialException` (aucun calcul) |
| Coefficient `null`, NaN, ±∞ | Exception explicite, jamais de conversion en 0 |
| Polynôme constant | 0 tour de boucle, résultat = la constante |
| Mode exact au-delà de 200 000 chiffres | `PrecisionLimitExceededException`, qui conseille le mode contrôlé |
