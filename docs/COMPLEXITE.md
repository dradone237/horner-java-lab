# Complexité : Horner face à l'évaluation naïve

## 1. Coût arithmétique (nombre d'opérations, degré n)

| Méthode | Multiplications | Additions | Ordre |
|---|---|---|---|
| **Horner** | n | n | **O(n)** |
| Horner étendu (P et P') | 2n | 2n | O(n) |
| Naïve, puissances recalculées (xᵏ par k multiplications) | n(n+1)/2 + (n+1) | n | **O(n²)** |
| Puissances incrémentales (xᵏ = xᵏ⁻¹·x) | 2n | n | O(n) |
| Somme avec `BigDecimal.pow` (oracle) | O(n log n) en interne | n | O(n log n) |

Pour n = 3 : 3 multiplications avec Horner contre 10 avec la méthode naïve. Ces nombres sont comptés par le programme lui-même et vérifiés par les tests.

## 2. Mémoire de travail

- **Horner** : un accumulateur, donc O(1) en plus des coefficients (hors trace).
- `evaluateWithSteps` conserve n + 1 étapes : O(n) objets, uniquement pour l'affichage pédagogique.
- `BigDecimal` **n'a pas une taille constante** : en mode exact, l'accumulateur grossit à chaque tour.

## 3. Incidence de `BigDecimal`

- **Mode contrôlé** (34 chiffres) : chaque opération porte sur des nombres de taille bornée, donc chaque opération coûte O(1) et l'algorithme O(n) au total.
- **Mode exact** : si x possède s chiffres, l'accumulateur atteint environ n·s chiffres. Le coût du k-ième tour est proportionnel à la taille de l'accumulateur, d'où un coût total d'environ **O(n²·s)** opérations sur les chiffres, bien que le nombre d'opérations arithmétiques reste n.
- Garde-fou : au-delà de 200 000 chiffres, le calcul exact s'arrête avec un message qui conseille la précision contrôlée (test `calculationLimit`).

## 4. Mesures réelles

Commande : `java -jar target/horner-java-lab-1.0.0.jar benchmark --sizes 10,100,1000,10000 --repeat 10 --warmup 5` (rapport brut : `reports/benchmark.md`).

Environnement : Java 21.0.13 (Temurin, HotSpot), 16 cœurs logiques, Windows 11, le 2026-10-31. Mode contrôlé 34 chiffres, x = 0.99, coefficients entiers aléatoires (graine 20261031).

| Coefficients | Méthode | Temps moyen (ms) | Multiplications | Octets alloués/appel |
|---:|---|---:|---:|---:|
| 10 | Horner | 0.0453 | 9 | 2 311 |
| 10 | Somme (pow) | 0.0407 | n/d | 3 183 |
| 10 | Naïve | 0.0244 | 55 | 4 155 |
| 100 | Horner | 0.0994 | 99 | 51 252 |
| 100 | Somme (pow) | 0.3019 | n/d | 181 037 |
| 100 | Naïve | 1.1528 | 5 050 | 1 194 199 |
| 1 000 | Horner | 0.4498 | 999 | 551 935 |
| 1 000 | Somme (pow) | 3.6095 | n/d | 3 605 740 |
| 1 000 | Naïve | 35.3414 | 500 500 | 144 945 215 |
| 10 000 | Horner | 1.5481 | 9 999 | 5 402 632 |
| 10 000 | Somme (pow) | 22.5502 | n/d | 57 326 911 |

La méthode naïve n'est pas mesurée à 10 000 coefficients (environ 5·10⁷ multiplications par appel, trop long à répéter).

### Lecture des résultats

- De 100 à 10 000 coefficients, le temps de Horner est multiplié par environ 16 pour une taille multipliée par 100 : la croissance est bien **au plus linéaire**. À petite taille, les coûts fixes (création d'objets, JIT) dominent.
- La méthode naïve passe de 1,15 ms à 35,3 ms quand n est multiplié par 10 : le temps est multiplié par environ 31, alors que le nombre de multiplications l'est par environ 99 (de 5 050 à 500 500). La croissance est nettement plus que linéaire. L'écart avec ×100 vient des coûts fixes encore visibles à n = 100 et du fait que les multiplications par 1 au début de chaque puissance sont peu coûteuses.
- **À 10 coefficients, la méthode naïve est mesurée plus rapide que Horner.** Ce résultat est conservé tel quel : à cette taille, les durées (quelques centièmes de milliseconde) sont dominées par le bruit de mesure et les effets du JIT. Ce n'est pas un défaut de Horner, mais cela montre qu'un micro-benchmark sur de très petites tailles n'est pas représentatif.

### Précautions prises

- Tours de chauffe pour la compilation JIT, puis 10 répétitions mesurées par `System.nanoTime`.
- Les résultats sont consommés, pour éviter l'élimination de code mort.
- **Vérification préalable** : chaque méthode est comparée à Horner sous la borne d'erreur avant d'être chronométrée (colonne « Résultats cohérents » = oui partout).
- La mémoire indiquée est le volume **alloué** par le thread (`ThreadMXBean`), pas la mémoire occupée : c'est une mesure approximative.
- Les durées dépendent de la machine et de sa charge : elles sont **indicatives**.
