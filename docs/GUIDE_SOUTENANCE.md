# Guide de soutenance (5 minutes)

## Avant de commencer (1 minute de préparation)

1. Double-cliquer sur `Demarrer_Horner.bat` (ou lancer `java -jar target/horner-java-lab-1.0.0.jar`).
2. Cliquer sur **MODE SOUTENANCE**, puis appuyer sur **F11** pour passer en plein écran. Si le vidéoprojecteur est peu lisible, utiliser **A+**.
3. Garder un terminal ouvert pour montrer la ligne de commande.

## Déroulé (étapes 1 à 8 de l'application)

| Temps | Étape | Ce que je dis |
|---|---|---|
| 0:00 | 1. Le polynôme | « On veut calculer P(x) = 2x³ − 6x² + 2x − 1 en x = 3. Le programme range les coefficients du plus haut degré au plus bas : [2, −6, 2, −1]. » |
| 0:30 | 2. Le principe | « Horner factorise x : ((2x − 6)x + 2)x − 1. À chaque étape : on multiplie par x et on ajoute le coefficient suivant. Aucune puissance n'est calculée. » |
| 1:00 | 3. Pas à pas | Appuyer sur → à chaque ligne : 2 ; 2×3 − 6 = 0 ; 0×3 + 2 = 2 ; 2×3 − 1 = **5**. « Ces lignes sont calculées par le vrai moteur Java, pas écrites à l'avance. » |
| 1:45 | 4. Comparaison | « La méthode classique fait 54 − 54 + 6 − 1 = 5 : l'oracle indépendant donne le même résultat. Horner fait 3 multiplications, la méthode naïve 10. » |
| 2:15 | 5. Division | « Les accumulateurs 2, 0, 2 forment le quotient 2x² + 2, et le dernier, 5, est le reste. Le reste est égal à P(3). Avec x³ − 6x² + 11x − 6 et r = 2, le reste vaut 0 : 2 est une racine. » |
| 2:45 | 6. Dérivée | « En mettant à jour d avant b, on obtient P'(3) = 20 dans le même parcours. » |
| 3:15 | 7. Tests | « Cas normaux, limites (polynôme nul, x = 0, x = −1, très grands coefficients), entrée invalide refusée avec un message, et le piège de l'arrondi : 10⁻⁴⁰ exact contre 0 en précision limitée. » |
| 4:00 | 8. Conclusion | « Horner est en O(n) contre O(n²) pour la méthode naïve. 203 tests automatiques passent, la couverture du moteur est de 99,6 %. Mais des tests réussis ne prouvent pas l'absence de défaut. » |
| 4:30 | Démonstration CLI | `java -jar target/horner-java-lab-1.0.0.jar divide --coeffs "2,-6,2,-1" --root 3` |

## Explication en français facile

> Pour calculer un polynôme, au lieu de calculer x³, x², x puis de tout additionner, on part du premier coefficient. À chaque étape, on le multiplie par x et on ajoute le coefficient suivant. Après n étapes, on a le résultat, avec n multiplications seulement.

## Exemple manuel à savoir refaire au tableau

P(x) = 2x³ − 6x² + 2x − 1, x = 3 :

```
 2   -6    2   -1
      6    0    6      (× 3)
 2    0    2    5      → P(3) = 5, quotient [2, 0, 2], reste 5
```

## Questions possibles et réponses

**Pourquoi Horner est-il plus rapide ?**
Il fait n multiplications au lieu d'environ n²/2 avec la méthode naïve. Mesure réelle à 1 000 coefficients : 0,45 ms contre 35 ms.

**Comment savez-vous que le résultat est juste ?**
Par un **oracle indépendant**, qui calcule la somme des aᵢ·xⁱ sans jamais appeler Horner, et par des propriétés vérifiées sur 500 cas aléatoires reproductibles : P(0) = a₀, P(1) = somme des coefficients, linéarité, reste = P(r), dérivée symbolique.

**Pourquoi BigDecimal et pas double ?**
Un `double` ne représente pas 0.1 exactement. `BigDecimal` permet un mode **exact** sans arrondi, et un mode **contrôlé** avec un nombre de chiffres choisi.

**Que se passe-t-il si on tape « abc » ou « 1,5 » ?**
L'entrée est refusée avec un message en français qui indique le champ fautif. Une valeur invalide n'est jamais transformée en 0.

**Et si le reste vaut 0,0000000001 ?**
En mode exact, ce n'est **pas** une racine : une très petite valeur n'est jamais confondue avec zéro. En mode contrôlé, on parle de racine *approximative* sous une tolérance calculée (borne d'erreur de Higham).

**Pourquoi le polynôme nul a-t-il un degré −1 ?**
Son degré n'est pas défini en mathématiques (on écrit parfois −∞). −1 est la convention choisie et documentée.

**Que veut dire 99,6 % de couverture ?**
99,6 % des instructions du moteur ont été exécutées par les tests. Cela ne prouve pas l'absence d'erreur : un test peut passer sur une ligne sans essayer toutes les valeurs.

**Pourquoi la méthode naïve est-elle plus rapide à 10 coefficients dans le benchmark ?**
À cette taille, les durées (quelques centièmes de milliseconde) sont dominées par le bruit de mesure et le compilateur JIT. Le résultat a été laissé tel quel, car on ne modifie pas une mesure qui dérange.

**Quelle est la différence entre boîte noire, blanche et grise ?**
- Boîte noire : seulement les entrées et les sorties (P(3) = 5).
- Boîte blanche : on teste l'intérieur du code (le contenu de chaque étape, l'ordre des mises à jour de la dérivée).
- Boîte grise : on teste par l'interface publique en connaissant le fonctionnement interne (le contrôleur de la GUI, la tolérance du vérificateur).
