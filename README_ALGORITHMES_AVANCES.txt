GESTION MISSIONS - BACKEND SPRING BOOT AVANCÉ

Cette version ajoute des fonctionnalités algorithmiques visibles pour la validation PI :

1) Analyse mission IA
Endpoint: POST /api/advanced/analyze-mission
Calculs: nettoyage de données, extraction NLP, classification, complexité, risque, niveau requis, faisabilité.

2) Top développeurs recommandés
Endpoint: POST /api/advanced/top-developers
Calculs: similarité cosinus, réputation, disponibilité, historique, budget, score global.

3) Prédiction réussite
Endpoint: POST /api/advanced/predict-success
Calcule la probabilité qu'un développeur réussisse une mission.

4) Comparaison développeurs
Endpoint: POST /api/advanced/compare-developers
Compare plusieurs développeurs sur les scores techniques et métier.

5) Détection anomalies
Endpoint: GET /api/advanced/scan-anomalies
Détecte budgets faibles, descriptions floues, missions risquées, développeurs occupés/refus fréquents.

6) Optimisation budget
Endpoint: POST /api/advanced/optimize-budget
Propose une fourchette et un budget recommandé.

7) Dashboard prédictif
Endpoint: GET /api/advanced/predictive-dashboard
Calcule revenu potentiel, revenu prédit, alertes, meilleurs développeurs.

8) IA explicable
Endpoint: POST /api/advanced/explain-matching
Explique la formule de matching et les raisons du classement.

9) Génération cahier des charges
Endpoint: POST /api/advanced/generate-spec
Génère un cahier des charges structuré à partir de la mission.

10) Amélioration description mission
Endpoint: POST /api/advanced/improve-description
Transforme une description faible en description professionnelle.

Phrase de soutenance:
Le backend Spring Boot ne se limite pas au CRUD. Il contient un moteur décisionnel avec nettoyage de données, NLP, feature engineering, similarité cosinus, scoring multicritère, ranking, prédiction et IA explicable.
