# L'algorithme de Horner expliqué

## 1. Qu'est-ce qu'un polynôme ?

Un polynôme de degré n s'écrit :

P(x) = aₙ·xⁿ + aₙ₋₁·xⁿ⁻¹ + … + a₁·x + a₀, avec aₙ ≠ 0.

Les nombres aₙ, …, a₀ sont les **coefficients**. aₙ est le **coefficient dominant** et a₀ le **terme constant**.

## 2. Représentation des coefficients dans le programme

Les coefficients sont rangés **par puissances décroissantes** :

| Polynôme | Tableau |
|---|---|
| 2x³ − 6x² + 2x − 1 | `[2, -6, 2, -1]` |
| 3x⁴ + 2 | `[3, 0, 0, 0, 2]` (les zéros intermédiaires sont obligatoires) |
| 7 | `[7]` |
| polynôme nul | `[0]` |

Décisions de conception, documentées et testées :

- **Zéros dominants normalisés** : `[0, 0, 2, 1]` devient `[2, 1]`. Ils ne changent ni la valeur ni le degré, il est donc plus utile de les accepter que de les refuser.
- **Polynôme nul** : représenté par `[0]`, de degré conventionnel **−1** (le degré du polynôme nul n'est pas défini au sens usuel ; −1 joue le rôle de −∞).
- **Polynôme constant** non nul : degré 0, aucune opération nécessaire.

## 3. Le principe de Horner

On factorise x autant que possible :

2x³ − 6x² + 2x − 1 = ((2x − 6)x + 2)x − 1

On lit l'expression de l'intérieur vers l'extérieur : on part de 2, on multiplie par x, on ajoute −6, on multiplie par x, on ajoute 2, on multiplie par x, on ajoute −1.

**Règle :** accumulateur ← accumulateur × x + coefficient suivant.

On ne calcule **jamais** de puissance de x.

## 4. Exemple détaillé : P(3) pour P(x) = 2x³ − 6x² + 2x − 1

| Étape | Coefficient | Accumulateur avant | Multiplication | Addition | Accumulateur après |
|---|---|---|---|---|---|
| 1 | 2 | — | — | — | **2** |
| 2 | −6 | 2 | 2 × 3 = 6 | 6 + (−6) = 0 | **0** |
| 3 | 2 | 0 | 0 × 3 = 0 | 0 + 2 = 2 | **2** |
| 4 | −1 | 2 | 2 × 3 = 6 | 6 + (−1) = 5 | **5** |

**P(3) = 5.** Vérification par la définition : 2·27 − 6·9 + 2·3 − 1 = 54 − 54 + 6 − 1 = 5.

Ce tableau est produit par le programme (`steps`, onglet « Étapes ») : c'est une trace réelle de la boucle, pas un texte écrit à l'avance.

## 5. Deuxième exemple : une racine

P(x) = x³ − 6x² + 11x − 6, en x = 2 : accumulateurs 1, −4, 3, **0**.

P(2) = 0 démontre que **2 est une racine** de P. Le programme ne prétend pas avoir trouvé les autres racines (1 et 3) : il ne contient pas de solveur général.

## 6. Extensions

- **Dérivée** : en mettant à jour en même temps d ← d·x + b, on obtient P'(x) dans le même parcours (P'(3) = 20).
- **Division synthétique** : les accumulateurs successifs sont les coefficients du quotient de P(x) par (x − r), et le dernier est le reste R = P(r). Pour r = 3 : Q(x) = 2x² + 2, R = 5.

## 7. Avantages

- **n multiplications et n additions** pour un degré n : c'est le minimum pour un polynôme quelconque (résultat d'Ostrowski et Pan).
- Algorithme simple, itératif, avec une mémoire de travail constante (un accumulateur).
- Meilleure stabilité numérique que le calcul séparé des puissances, et une borne d'erreur connue en arithmétique arrondie.
- Il sert aussi à la division synthétique et au calcul de la dérivée.

## 8. Limites

- En arithmétique arrondie, une **annulation** peut faire perdre toute la précision : (x − 1)² en x = 1 + 10⁻²⁰ vaut exactement 10⁻⁴⁰, mais donne 0 avec 34 chiffres significatifs. Le programme le montre et ne qualifie jamais ce 0 de racine exacte.
- En arithmétique exacte (`BigDecimal` sans arrondi), la taille des nombres croît avec le degré : le coût réel dépasse alors O(n) (voir `COMPLEXITE.md`). Une limite de 200 000 chiffres protège le programme.
- Horner évalue un polynôme en **un** point ; pour beaucoup de points, d'autres méthodes (FFT, évaluation multipoint) deviennent intéressantes.
