# Rapport de tests

Toutes les valeurs ci-dessous proviennent d'exécutions réelles du **2026-10-31** sur la machine de développement.

## 1. Environnement

| Élément | Version |
|---|---|
| Système | Windows 11 Pro 10.0.26200, x64, 16 cœurs logiques |
| JDK | Eclipse Temurin 21.0.13+9 (LTS), HotSpot 64 bits |
| Maven | 3.9.16 |
| JUnit | 6.1.3 (Jupiter, tests paramétrés) |
| JaCoCo | 0.8.16 |
| Surefire / Compiler / Jar | 3.6.1 / 3.16.0 / 3.5.1 |

Toutes les versions ont été relevées sur Maven Central avant d'écrire le `pom.xml`.

## 2. Résultats de la campagne rapide (`mvn clean verify`)

| Classe de test | Tests | Réussis | Échecs | Erreurs | Ignorés |
|---|---:|---:|---:|---:|---:|
| HornerEvaluatorTest | 34 | 34 | 0 | 0 | 0 |
| DerivativeEvaluatorTest | 13 | 13 | 0 | 0 | 0 |
| SyntheticDivisionTest | 13 | 13 | 0 | 0 | 0 |
| PolynomialTest | 18 | 18 | 0 | 0 | 0 |
| InputValidatorTest | 59 | 59 | 0 | 0 | 0 |
| ReferenceEvaluatorTest | 10 | 10 | 0 | 0 | 0 |
| HornerPropertyTest (6 × 500 cas + reproductibilité) | 7 | 7 | 0 | 0 | 0 |
| HornerIntegrationTest | 6 | 6 | 0 | 0 | 0 |
| CommandLineAppTest | 33 | 33 | 0 | 0 | 0 |
| GuiControllerTest (sans affichage) | 10 | 10 | 0 | 0 | 0 |
| **Total** | **203** | **203** | **0** | **0** | **0** |

`BUILD SUCCESS`. Contrôle JaCoCo : « All coverage checks have been met ».

## 3. Tests lourds (`mvn verify -Pheavy`)

| Test | Durée | Résultat |
|---|---:|---|
| 10 001 coefficients, mode contrôlé, sous la borne d'erreur | 0,306 s | Réussi |
| Degré 2 000 en mode exact = oracle | 0,004 s | Réussi |
| Campagne intégrée de 5 000 cas par propriété | 0,274 s | Réussi |
| Benchmark 10 → 10 000 coefficients, résultats cohérents | 0,529 s | Réussi |
| **Total : 4 tests, 0 échec** | | |

## 4. Campagne intégrée à l'application (`selftest`)

`java -jar target/horner-java-lab-1.0.0.jar selftest --count 500` donne **24 contrôles, 24 réussis, 0 échoué** (graine 20261031, 91 ms). Rapport : `reports/selftest.md`.

## 5. Couverture mesurée (JaCoCo, campagne rapide)

| Paquet | Instructions | Branches | Méthodes | Classes |
|---|---:|---:|---:|---:|
| `core` (moteur mathématique) | **99,6 %** (1 836/1 844) | **98,6 %** (205/208) | 98,8 % | 15 |
| `validation` (saisie, oracle, vérificateur) | 98,7 % (1 435/1 454) | 95,0 % (133/140) | 97,8 % | 9 |
| `cli` | 96,4 % (1 380/1 432) | 86,4 % (89/103) | 97,1 % | 4 |
| `reporting` | 95,4 % (1 877/1 967) | 62,7 % (89/142) | 100 % | 8 |
| `examples` | 86,5 % (109/126) | 70,0 % (7/10) | 100 % | 2 |
| `gui` (mesurée séparément) | 75,5 % (3 990/5 285) | 54,0 % (94/174) | 61,0 % | 23 |
| `org.hornerlab` (`Main`) | 0 % (0/56) | 0 % (0/12) | 0 % | 1 |
| **Total** | **87,4 %** | **78,2 %** | 83,5 % | |

Les seuils imposés par `pom.xml` portent sur le paquet `core` (≥ 95 % des instructions, ≥ 90 % des branches) : **ils sont atteints**. Aucune classe n'a été exclue de la mesure.

**Couverture résiduelle expliquée :**
- `Main` (0 %) : le point d'entrée appelle `System.exit` ou ouvre une fenêtre ; il a été vérifié **manuellement** par l'exécution réelle du JAR (section 7), pas par JUnit.
- `HornerSwingApp` (0 %) : la fenêtre (`JFrame`) ne peut pas être créée en mode sans affichage. Les panneaux, eux, sont testés sans affichage, et la fenêtre a été lancée et contrôlée réellement (section 7).
- `ValidationPanel` (55 %) : le `SwingWorker` et l'export ne sont pas déclenchés par les tests automatiques.
- `reporting` (62,7 % des branches) : ce sont surtout des branches d'erreur d'entrée/sortie et de formatage du benchmark.

## 6. Défauts détectés et corrections

| N° | Constat | Cause | Correction |
|---|---|---|---|
| 1 | `InputValidatorTest.polynomialFromFields` : attendu « coefficient a2 », obtenu « coefficient a1 » | **Erreur dans le test** : dans [1, x, 3], le champ fautif « x » est bien le coefficient de x¹. Le code était correct. | Attente corrigée en a1, avec un commentaire explicatif. |
| 2 | `CommandLineAppTest.calculationLimit` : attendu le code 4, obtenu le code 0 | **Hypothèse de test fausse** : 10 001 coefficients × 18 chiffres produisent environ 180 000 chiffres, sous la limite de 200 000. Le calcul exact a donc réussi (8 s). | Nouveau scénario qui franchit réellement la limite (x à 190 chiffres, 2 001 coefficients) ; la limite est bien détectée (code 4). |
| 3 | Accueil de la GUI : boutons étirés et textes tronqués, paragraphe coupé à droite | Mise en page `BoxLayout` + `GridLayout(1, 5)` sans hauteur maximale | Grille 2 × 3, hauteur limitée, largeur du texte réduite ; vérifié sur une nouvelle capture. |
| 4 | Accents illisibles (« � ») dans la console Git Bash | Le tube de Git Bash ne suit pas la page de code de la console Windows | Les lanceurs forcent `chcp 65001` et `-Dstdout.encoding=UTF-8`. |
| 5 | `Demarrer_Horner.bat` refusait Java 21 (« Java  detecte : Java 21 ou plus recent est requis ») | Avec un chemin contenant des espaces (`C:\Program Files\…`), `cmd` retire les guillemets dans `for /f ('"…" -version …')` ; la version n'était jamais lue | Commande préfixée par `call` ; retesté : `eval`, `divide`, entrée invalide (code 3) et ouverture de la GUI par `javaw` fonctionnent. |

Aucun résultat attendu n'a été modifié pour faire passer un test sans justification mathématique.

## 7. Vérifications manuelles réellement effectuées

- `java -jar target/horner-java-lab-1.0.0.jar eval --coeffs "2,-6,2,-1" --x 3` donne `P(3) = 5` (code 0).
- `steps`, `derivative` (P'(3) = 20), `divide` (`Quotient : [2, 0, 2]`, `Reste : 5`) : sorties conformes.
- `eval --coeffs "2,abc" --x 3` : message en français, code 3.
- `java -Djava.awt.headless=true -jar …` : aucune exception graphique, l'aide CLI s'affiche.
- Lancement de l'interface réelle : fenêtre « HORNER JAVA LAB — Évaluation et vérification des polynômes » (1280 × 820), aucune erreur sur la sortie d'erreur. Navigation par clics vers Évaluation, Étapes, Division, Vérification et Mode soutenance, avec captures dans `reports/gui-*.png`.
- Manifeste du JAR : `Main-Class: org.hornerlab.Main`, `Implementation-Version: 1.0.0`.
- `Demarrer_Horner.bat` sans argument : la fenêtre s'ouvre via `javaw`. Avec arguments : sorties CLI correctes, accents compris. `run.ps1 derivative …` donne P'(3) = 20 (code 0), et `run.ps1 eval --coeffs "2,abc"` renvoie le code 3.

## 8. Limites

- Les tests réussis **augmentent la confiance mais ne prouvent pas** l'absence de défaut ; une couverture de 99,6 % ne garantit pas que toutes les combinaisons de valeurs ont été essayées.
- L'oracle utilise lui aussi `BigDecimal` : une erreur commune dans `BigDecimal` ne serait pas détectée (risque jugé très faible pour une bibliothèque standard du JDK).
- Les tests de la GUI sont faits sans affichage ; le rendu visuel, le plein écran et le minuteur d'exécution automatique ont été vérifiés manuellement, pas automatiquement.
- Les durées du benchmark sont indicatives et dépendent de la machine.
- Le programme ne calcule pas toutes les racines d'un polynôme (ce n'est pas un objectif du sujet).
