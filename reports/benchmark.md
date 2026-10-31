# Benchmark Horner vs évaluation naïve

- Mode : précision contrôlée (34 chiffres significatifs, HALF_EVEN), x = 0.99
- Répétitions : 10, tours de chauffe : 5, graine : 20261031
- Java : 21.0.13 (OpenJDK 64-Bit Server VM), 16 cœurs logiques
- Durées indicatives ; « octets alloués » = allocations du thread par appel (approximatif).

| Coefficients | Méthode | Temps moyen (ms) | Temps min (ms) | Multiplications | Additions | Octets alloués/appel | Résultats cohérents |
|---:|---|---:|---:|---:|---:|---:|---|
| 10 | Horner | 0.0453 | 0.0360 | 9 | 9 | 2311 | oui |
| 10 | Somme des termes (BigDecimal.pow) | 0.0407 | 0.0306 | n/d | n/d | 3183 | oui |
| 10 | Naïve (puissances recalculées) | 0.0244 | 0.0174 | 55 | 9 | 4155 | oui |
| 100 | Horner | 0.0994 | 0.0680 | 99 | 99 | 51252 | oui |
| 100 | Somme des termes (BigDecimal.pow) | 0.3019 | 0.2635 | n/d | n/d | 181037 | oui |
| 100 | Naïve (puissances recalculées) | 1.1528 | 0.6092 | 5050 | 99 | 1194199 | oui |
| 1000 | Horner | 0.4498 | 0.4324 | 999 | 999 | 551935 | oui |
| 1000 | Somme des termes (BigDecimal.pow) | 3.6095 | 3.0555 | n/d | n/d | 3605740 | oui |
| 1000 | Naïve (puissances recalculées) | 35.3414 | 32.9707 | 500500 | 999 | 144945215 | oui |
| 10000 | Horner | 1.5481 | 1.4514 | 9999 | 9999 | 5402632 | oui |
| 10000 | Somme des termes (BigDecimal.pow) | 22.5502 | 21.7147 | n/d | n/d | 57326911 | oui |
