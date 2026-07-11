MISE A JOUR SPRING BOOT - PRE-PUBLICATION INTELLIGENTE

Nouveaux endpoints:
POST /api/advanced/recommend-best-developer
POST /api/advanced/publish-optimized-mission

Objectif:
Avant que le client publie une mission, le backend Spring Boot effectue:
- data cleaning
- extraction NLP des compétences
- classification automatique
- estimation budget/délai
- score de risque
- amélioration de description
- ranking des développeurs
- recommandation au développeur optimal

Quand la mission est publiée via /publish-optimized-mission:
1) description améliorée automatiquement
2) budget recommandé appliqué
3) compétences extraites appliquées
4) mission créée dans MySQL
5) meilleur développeur calculé
6) notification + email de recommandation envoyés au développeur
7) notification créée pour le client
