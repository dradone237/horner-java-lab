# Guide utilisateur

## 1. Prérequis

- **Java 21** ou plus récent : `winget install EclipseAdoptium.Temurin.21.JDK`
- **Maven 3.9** ou plus récent, uniquement pour reconstruire le projet.

## 2. Construire le projet

```
mvn clean verify      # compile, exécute les 203 tests, mesure la couverture, crée le JAR
mvn clean package     # crée le JAR (les tests sont exécutés)
mvn verify -Pheavy    # tests lourds uniquement (10 000 coefficients, 5 000 cas aléatoires)
```

Le JAR est créé dans `target/horner-java-lab-1.0.0.jar`. Le rapport de couverture se trouve dans `target/site/jacoco/index.html`.

## 3. Lancer l'application

| Méthode | Commande |
|---|---|
| Double-clic | `Demarrer_Horner.bat` |
| PowerShell | `.\run.ps1` (ajouter `-Build` pour reconstruire) |
| Java directement | `java -jar target/horner-java-lab-1.0.0.jar` |

Sans argument, l'**interface graphique** s'ouvre. Sur une machine sans affichage, l'aide de la ligne de commande s'affiche à la place, sans erreur.

## 4. Interface graphique

1. **Accueil** : présentation et accès aux cinq parcours.
2. **Évaluation** :
   - choisir un exemple ou un degré (0 à 30) ;
   - saisir les coefficients du plus haut degré au terme constant, ainsi que x ;
   - choisir le mode *Exact* ou *Précision contrôlée* ;
   - cliquer sur **Calculer**.

   La formule P(x) se met à jour pendant la saisie.
3. **Étapes** : le tableau Étape | Coefficient | Accumulateur avant | Multiplication | Addition | Accumulateur après, avec les boutons Précédent, Suivant, Recommencer et Exécution automatique.
4. **Division synthétique** : choisir r. L'écran affiche le schéma de Horner, le quotient, le reste, la vérification de l'identité et le statut de r (racine exacte, approximative ou non racine).
5. **Vérification** : comparaison de Horner avec l'oracle indépendant. Les verdicts sont *Identique* (vert), *Proche sous tolérance* (orange) ou *Incohérent* (rouge).
6. **Tests et validation** : lance la campagne intégrée (graine et nombre de cas réglables), permet de l'exporter en Markdown vers `reports/` et affiche les derniers résultats Maven, s'ils existent.
7. **MODE SOUTENANCE** : 8 étapes pour une présentation de 5 minutes.
   - Navigation : → ou Espace (suivant), ← (précédent), Début (recommencer).
   - **F11** passe en plein écran, **Échap** en sort, et **A+ / A−** agrandissent ou réduisent le texte.

Règle de saisie : le **point** est le séparateur décimal (`1.5`). La notation scientifique est acceptée (`1e-3`).

## 5. Ligne de commande

```
java -jar target/horner-java-lab-1.0.0.jar eval       --coeffs "2,-6,2,-1" --x 3
java -jar target/horner-java-lab-1.0.0.jar steps      --coeffs "2,-6,2,-1" --x 3
java -jar target/horner-java-lab-1.0.0.jar derivative --coeffs "2,-6,2,-1" --x 3
java -jar target/horner-java-lab-1.0.0.jar divide     --coeffs "2,-6,2,-1" --root 3
java -jar target/horner-java-lab-1.0.0.jar verify     --coeffs "2,-6,2,-1" --x 3
java -jar target/horner-java-lab-1.0.0.jar selftest   --count 500 --export reports/selftest.md
java -jar target/horner-java-lab-1.0.0.jar benchmark  --sizes 10,100,1000,10000 --repeat 10
java -jar target/horner-java-lab-1.0.0.jar examples
java -jar target/horner-java-lab-1.0.0.jar --help
```

- Les coefficients sont séparés par des **virgules**, en puissances **décroissantes**.
- Valeurs négatives : `--x -1` ou `--x=-1`.
- Mode contrôlé : `--mode controle --precision 34`.
- Gros polynômes : `--coeffs-file coefficients.txt`.

| Code de sortie | Signification |
|---|---|
| 0 | succès |
| 1 | erreur interne |
| 2 | erreur d'utilisation (commande ou option) |
| 3 | entrée invalide |
| 4 | limite de calcul atteinte |
| 5 | affichage indisponible (`gui`) |
| 6 | vérification en échec |

## 6. Limites de saisie

| Interface | Limite |
|---|---|
| CLI | jusqu'à 10 001 coefficients (degré 10 000) et 200 caractères par nombre |
| GUI | degré 30 au maximum (lisibilité) |
| Les deux | exposant décimal compris entre −1000 et +1000 |
| Mode exact | 200 000 chiffres au maximum ; au-delà, utiliser la précision contrôlée |
