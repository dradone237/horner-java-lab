# Rapport de la campagne de validation intégrée

- Date : 2026-10-31T12:09:22.2452969
- Graine aléatoire : 20261031
- Cas aléatoires par propriété : 500
- Durée : 91 ms
- Bilan : 24 contrôles, 24 réussis, 0 échoués

| Catégorie | Contrôle | Résultat | Détails |
|---|---|---|---|
| Référence | P(3) = 5 pour 2x³ − 6x² + 2x − 1 | OK | valeur attendue 5 |
| Référence | Étapes 2, 0, 2, 5 | OK | accumulateurs successifs et 3 multiplications + 3 additions |
| Référence | P'(3) = 20 | OK | Horner étendu |
| Référence | Division par (x − 3) : Q = [2, 0, 2], R = 5 | OK | quotient, reste et identité P = (x − r)Q + R |
| Référence | x³ − 6x² + 11x − 6 : 2 est une racine exacte | OK | reste exactement nul |
| Cas particulier | Polynôme nul | OK | P(10) = 0, degré conventionnel −1 |
| Cas particulier | Polynôme constant | OK | P(123) = 7 |
| Cas particulier | Zéros intermédiaires 3x⁴ + 2 en x = −1 | OK | P(−1) = 5 |
| Cas particulier | Coefficients décimaux | OK | P(0.5) = 2.625 |
| Précision | Annulation : exact 1E-40, contrôlé 0, jamais confondus | OK | le mode exact refuse de qualifier x de racine |
| Entrée invalide | « abc » refusé | OK | message explicite, aucune conversion en 0 |
| Entrée invalide | « 1,5;2 » refusé | OK | séparateur « ; » signalé |
| Entrée invalide | « NaN » refusé | OK | NaN refusé |
| Entrée invalide | liste vide refusée | OK | liste vide signalée |
| Propriété | Horner = oracle Σ a_ix^i (exact) | OK | 500/500 cas réussis (graine 20261031) |
| Propriété | P(0) = a₀ | OK | 500/500 cas réussis (graine 20261031) |
| Propriété | P(1) = Σ a_i | OK | 500/500 cas réussis (graine 20261031) |
| Propriété | P(−1) = Σ (−1)^i a_i | OK | 500/500 cas réussis (graine 20261031) |
| Propriété | P_k·(x) = k·P(x) | OK | 500/500 cas réussis (graine 20261031) |
| Propriété | (P + Q)(x) = P(x) + Q(x) | OK | 500/500 cas réussis (graine 20261031) |
| Propriété | Reste de la division = P(r) | OK | 500/500 cas réussis (graine 20261031) |
| Propriété | Identité P = (x − r)Q + R | OK | 500/500 cas réussis (graine 20261031) |
| Propriété | P'(x) Horner = dérivée symbolique | OK | 500/500 cas réussis (graine 20261031) |
| Propriété | Mode contrôlé dans la borne d'erreur | OK | 500/500 cas réussis (graine 20261031) |
