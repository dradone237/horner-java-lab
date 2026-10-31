# CLAUDE.md — Horner Java Lab

Projet universitaire (Vérification, Validation et Tests Logiciels) : algorithme de Horner en Java 21 / Maven.

## Commandes

- Construire et tester : `mvn clean verify` (203 tests, contrôle JaCoCo sur `org.hornerlab.core` : ≥ 95 % des instructions, ≥ 90 % des branches)
- Tests lourds : `mvn verify -Pheavy` (`@Tag("heavy")`, exclus par défaut)
- Lancer : `java -jar target/horner-java-lab-1.0.0.jar` (GUI) ou avec une commande CLI (`eval`, `steps`, `derivative`, `divide`, `verify`, `selftest`, `benchmark`, `examples`)
- Sur ce poste, Maven est en portable : `%USERPROFILE%\.maven\apache-maven-3.9.16\bin\mvn.cmd` ; JDK : `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`

## Règles du projet

- Coefficients **toujours en puissances décroissantes** ; zéros dominants normalisés ; polynôme nul = `[0]`, degré −1.
- Le moteur (`core`) n'affiche rien et ne dépend ni de la CLI ni de la GUI.
- L'oracle (`ReferenceEvaluator`) ne doit **jamais** appeler `HornerEvaluator`.
- Créer les `BigDecimal` à partir de chaînes ; comparer avec `compareTo`.
- Aucune valeur invalide n'est convertie silencieusement en 0 ; messages d'erreur en français.
- Les tests tournent en `java.awt.headless=true` : aucune fenêtre pendant `mvn test`.
- Ne jamais modifier un résultat attendu pour faire passer un test sans justification mathématique ; documenter tout défaut dans `docs/RAPPORT_DE_TESTS.md`.
- Ne pas toucher aux projets voisins `huffman-python` et `quadratic-testing-lab`.
- Le dossier `capture/` (captures personnelles) n'est pas versionné.
