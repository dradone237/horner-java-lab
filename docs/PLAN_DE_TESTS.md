# Plan de tests

Statut = résultat de l'exécution réelle du 2026-10-31 (`mvn clean verify` : 203 tests ; `mvn verify -Pheavy` : 4 tests). Classes de test dans `src/test/java/org/hornerlab/`.

Types : **N** = normal, **P** = particulier, **L** = limite, **I** = invalide, **O** = oracle / propriété, **INT** = intégration. Approche : **BN** = boîte noire, **BB** = boîte blanche, **BG** = boîte grise.

## 1. Évaluation de Horner (`HornerEvaluatorTest`)

| ID | Objectif | Entrée | Résultat attendu | Type | Statut |
|---|---|---|---|---|---|
| H-01 | Exemple de référence | [2,−6,2,−1], x = 3 | 5 | N / BN | Réussi |
| H-02 | Étapes exactes | idem | accumulateurs 2, 0, 2, 5 ; produits 6, 0, 6 ; 3 mult. + 3 add. | N / BB | Réussi |
| H-03 | Racine connue | [1,−6,11,−6], x = 2 | 0 | N / BN | Réussi |
| H-04 | Ordre des opérations | idem H-01 | × 3, + (−6), × 3, + 2, × 3, + (−1) | N / BB | Réussi |
| H-05 | Quadratique | [1,−3,2], x = 5 | 12 | N / BN | Réussi |
| H-06 | Linéaire | [4,−8], x = 2 | 0 | N / BN | Réussi |
| H-07 | Constant | [7], x = 123 | 7, 0 opération | P / BN | Réussi |
| H-08 | Décimaux | [1.5,−2.25,0,3], x = 0.5 | 2.625 | N / BN | Réussi |
| H-09 | Polynôme nul | [0] et [0,0,0] | 0 | P / BN | Réussi |
| H-10 | x = 0 | [2,−6,2,−1] | −1 (terme constant) | L / BN | Réussi |
| H-11 | x = 1 | idem | −3 (somme des coefficients) | L / BN | Réussi |
| H-12 | x = −1 | idem | −11 (somme alternée) | L / BN | Réussi |
| H-13 | Zéros intermédiaires | [3,0,0,0,2], x = −1 | 5 | P / BN | Réussi |
| H-14 | Zéros dominants | [0,0,2,−6,2,−1], x = 3 | 5 | P / BN | Réussi |
| H-15 | Alternance de signes | [1,−1,1,−1,1,−1,1], x = 2 | 43 | L / BN | Réussi |
| H-16 | Nombreux zéros | x¹⁰, x = 2 | 1024 | L / BN | Réussi |
| H-17 | Grands coefficients | [1e30,−2e30,1e30], x = 1.000001 | 1E+18 | L / BN | Réussi |
| H-18 | Très petites valeurs | [1e−20,1e−20], x = 1e−10 | 1.0000000001E−20 | L / BN | Réussi |
| H-19 | Forte amplitude | x⁵, x = 1000 | 1E+15 | L / BN | Réussi |
| H-20 | Résultat proche de zéro (exact) | (x−1)², x = 1+10⁻²⁰ | 1E−40, non nul | L / BB | Réussi |
| H-21 | Même cas, mode contrôlé 34 | idem | 0 (annulation documentée) | L / BB | Réussi |
| H-22 | Arrondi contrôlé | x², x = 1.23456789, p = 5 | 1.5242 (5 chiffres) | L / BB | Réussi |
| H-23 | Degré 1 000 | 1 001 coefficients égaux à 1, x = 1 | 1001, 1000 mult. | L / BN | Réussi |
| H-24 | Interface flottante | NaN, ±∞ (x ou coefficient) | exception | I / BN | Réussi |
| H-25 | Arguments null, liste vide | — | exceptions | I / BN | Réussi |
| H-26 | Limite du mode exact | 200 coeff., x = 123.456, limite 50 chiffres | `PrecisionLimitExceededException` | L / BB | Réussi |

## 2. Dérivée (`DerivativeEvaluatorTest`)

| ID | Objectif | Entrée | Attendu | Type | Statut |
|---|---|---|---|---|---|
| D-01 | Référence | [2,−6,2,−1], x = 3 | P = 5, P' = 20, 6 mult. | N / BN | Réussi |
| D-02 | Ordre des mises à jour | x², x = 3 | P' = 6 (et non 18) | P / BB | Réussi |
| D-03 | Étapes b et d | référence | b : 2,0,2,5 ; d : 0,2,6,20 | N / BB | Réussi |
| D-04 | Constant, nul, linéaire, racine, décimaux, x = 0 | 8 cas paramétrés | valeurs exactes | N/P/L | Réussi |
| D-05 | Mode contrôlé | référence, p = 10 | 5 et 20 | N / BB | Réussi |

## 3. Division synthétique (`SyntheticDivisionTest`)

| ID | Objectif | Entrée | Attendu | Type | Statut |
|---|---|---|---|---|---|
| S-01 | Référence | [2,−6,2,−1] ÷ (x − 3) | Q = [2,0,2], R = 5, identité vraie | N / BN | Réussi |
| S-02 | Racine exacte | [1,−6,11,−6] ÷ (x − 2) | Q = [1,−4,3], R = 0, racine exacte | N / BN | Réussi |
| S-03 | Constant | [7] ÷ (x − 4) | Q = [0], R = 7 | P / BN | Réussi |
| S-04 | Polynôme nul | [0] ÷ (x − 4) | Q = [0], R = 0, racine | P / BN | Réussi |
| S-05 | r = 0, r < 0, r décimal | 7 cas paramétrés | Q, R, identité, R = P(r) | N/L | Réussi |
| S-06 | Racine approximative | 3x − 1, r = 0.3333333333 | exact : R = −1E−10, non racine ; contrôlé : approximative si tol ≥ 1E−9 | L / BB | Réussi |
| S-07 | Écart d'identité en mode contrôlé | p = 5 | écart > 0 mesuré, non masqué | L / BB | Réussi |

## 4. Validation des entrées (`InputValidatorTest`)

| ID | Objectif | Entrée | Attendu | Type | Statut |
|---|---|---|---|---|---|
| V-01 | Nombres valides | 3, −2.5, +4, .5, 5., 1e−3, 2.5E+4, « − » Unicode | valeurs exactes | N / BN | Réussi |
| V-02 | Nombres invalides | abc, 1,5, NaN, Infinity, ∞, 1e, −−1, 1.2.3, 0x10, 1_000, 1 000, 1e5000… (19 cas) | `InvalidInputException` | I / BN | Réussi |
| V-03 | Valeurs absentes | null, "", espaces | exception | I / BN | Réussi |
| V-04 | Listes valides | « 2,-6,2,-1 », espaces, crochets, notation scientifique | coefficients exacts | N / BN | Réussi |
| V-05 | Listes invalides | "", [], 1,,2 ; ,1 ; 1, ; 1;2 ; 1 2 3 ; a,b… | exception, champ fautif nommé | I / BN | Réussi |
| V-06 | Taille maximale | 10 001 coefficients acceptés, 10 002 refusés | limite respectée | L / BN | Réussi |
| V-07 | Champs de la GUI | « x » en position a₁ | champ « coefficient a1 » signalé | I / BG | Réussi |

## 5. Oracle et propriétés (`ReferenceEvaluatorTest`, `HornerPropertyTest`)

| ID | Objectif | Entrée | Attendu | Type | Statut |
|---|---|---|---|---|---|
| O-01 | Oracle indépendant | somme Σ aₖxᵏ | 5 pour la référence | O / BN | Réussi |
| O-02 | Trois verdicts | valeurs fabriquées | Identique / Proche / Incohérent | O / BB | Réussi |
| O-03 | Vérification complète, mode exact | référence | 7 contrôles identiques | O / BG | Réussi |
| O-04 | Mode contrôlé sous borne | 5 coefficients décimaux, p = 8 | tous réussis, au moins un « proche » | O / BG | Réussi |
| P-01 | Horner = oracle | 500 cas aléatoires (graine 20261031) | égalité exacte | O | Réussi |
| P-02 | P(0), P(1), P(−1) | 500 cas | a₀, Σ aᵢ, somme alternée | O | Réussi |
| P-03 | Linéarité k·P et P + Q | 500 cas | égalité exacte | O | Réussi |
| P-04 | Reste = P(r), identité, degré du quotient | 500 cas | égalités exactes | O | Réussi |
| P-05 | Dérivée = dérivée symbolique | 500 cas | égalité exacte | O | Réussi |
| P-06 | Mode contrôlé (16 chiffres) | 500 cas | écart ≤ borne de Higham | O | Réussi |
| P-07 | Reproductibilité | graine 42 | séquences identiques | O | Réussi |

## 6. Intégration, CLI et GUI

| ID | Objectif | Entrée | Attendu | Type | Statut |
|---|---|---|---|---|---|
| I-01 | Chaîne complète | « 2,-6,2,-1 », « 3 » | affichage « P(3) = 5 » | INT | Réussi |
| I-02 | Chaîne de division | « [2, -6, 2, -1] », r = 3 | « [2, 0, 2] », « 5 », vérifications OK | INT | Réussi |
| I-03 | Exemples préchargés | 8 exemples | tous identiques à l'oracle | INT | Réussi |
| I-04 | Campagne intégrée et export | 100 cas | 0 échec, Markdown écrit | INT | Réussi |
| I-05 | Lecture des rapports Surefire et JaCoCo | fichiers réels ou absents | sommes exactes | INT | Réussi |
| C-01 | `eval` | référence | « P(3) = 5 », code 0 | INT / BN | Réussi |
| C-02 | `steps`, `derivative`, `divide` | référence | lignes attendues, code 0 | INT / BN | Réussi |
| C-03 | Options négatives et scientifiques | --x −1, --x=−1, 1e2 | P(−1) = −11, P(100) = 100 | L / BN | Réussi |
| C-04 | Erreurs d'utilisation | 13 lignes de commande fautives | code 2 + message | I / BN | Réussi |
| C-05 | Entrées invalides | 6 listes fautives, x = « 1,5 » | code 3 + message en français | I / BN | Réussi |
| C-06 | Limite de calcul | 2 001 coeff., x à 190 chiffres | code 4 + conseil | L / BN | Réussi |
| C-07 | Fichier de coefficients | 1 000 coefficients | P(1) = 1000 | L / BN | Réussi |
| G-01 | Logique de la GUI sans affichage | référence | P = 5, P' = 20, notifications | INT / BG | Réussi |
| G-02 | Tableau des étapes et schéma de division | référence | lignes exactes | INT / BG | Réussi |
| G-03 | Mode soutenance | script | 8 étapes, valeurs du moteur, aucun « ÉCHEC » | INT / BG | Réussi |
| G-04 | Panneaux Swing (headless) | boutons Suivant, Précédent, Recommencer | 1 → 3 → 2 → 1 lignes | INT / BG | Réussi |

## 7. Tests lourds (`HornerHeavyTest`, profil `-Pheavy`)

| ID | Objectif | Entrée | Attendu | Statut |
|---|---|---|---|---|
| L-01 | 10 001 coefficients, mode contrôlé | x = 0.999 | 10 000 mult., sous la borne | Réussi |
| L-02 | Degré 2 000 exact | x = 3 | égalité stricte avec l'oracle | Réussi |
| L-03 | Campagne de 5 000 cas | graine 20261031 | 0 échec | Réussi |
| L-04 | Benchmark 10 à 10 000 | — | résultats cohérents | Réussi |

## 8. Exemples par approche

- **Boîte noire** : H-01, H-10 à H-19, V-01 à V-06, C-01 à C-07. On ne regarde que les entrées et les sorties.
- **Boîte blanche** : H-02 et H-04 (contenu exact de la trace), H-20 à H-22 (branches exact / contrôlé de `NumericSettings`), H-26 (branche de dépassement de taille), D-02 (ordre des affectations), S-06 (branche « tolérance ignorée en mode exact »), O-02 (les trois branches du comparateur).
- **Boîte grise** : V-07 (on sait que les champs sont nommés d'après leur puissance), G-01 à G-04 (on connaît le contrat entre le contrôleur et les panneaux), O-03 et O-04 (on connaît la stratégie de tolérance du vérificateur).
