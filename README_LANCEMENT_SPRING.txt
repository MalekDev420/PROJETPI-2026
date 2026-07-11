GESTION MISSIONS - BACKEND SPRING BOOT AVANCÉ

Objectif:
Remplacer le backend Node.js par un backend Spring Boot avec vraie logique algorithmique:
- Data Cleaning
- NLP / extraction de mots-clés
- Classification automatique des missions
- Similarité Cosinus développeur/mission
- Score multicritère de matching
- Prédiction de réussite
- Score de risque mission
- Estimation budget / délai
- Dashboards statistiques
- Gmail SMTP
- Ollama Assistant IA local

1) Prérequis:
- Java 17 ou plus
- Maven
- XAMPP MySQL démarré
- Ollama lancé si vous voulez tester l'assistant IA

2) Lancement:
cd gestionmissions-spring-backend-advanced
mvn spring-boot:run

Le backend démarre sur:
http://localhost:3000

3) Base de données:
La base MySQL est créée automatiquement:
gestion_missions_spring

Les tables principales:
- users
- missions
- ratings
- deliveries
- notifications
- email_logs
- workflow_events

4) Frontend Angular:
Lancer le frontend fourni:
npm install
npm start

Frontend:
http://localhost:4200
Admin:
http://localhost:4200/admin

5) Ollama:
Dans un CMD séparé:
ollama run mistral

Test statut:
http://localhost:3000/api/assistant/status

6) Gmail SMTP:
Vous pouvez configurer les variables d'environnement:
SMTP_EMAIL=votre_adresse@gmail.com
SMTP_PASSWORD=mot_de_passe_application_google

Sinon les emails seront journalisés dans la table email_logs sans bloquer le projet.

7) Comptes démo:
Client: client@demo.com / 1234
Développeur: ahmed@dev.com / 1234
Développeur: sarra@dev.com / 1234
Admin: accès par /admin

Phrase soutenance:
"J'ai migré le backend vers Spring Boot et ajouté un pipeline algorithmique: nettoyage de données, extraction NLP, classification, similarité cosinus, scoring multicritère et prédiction de réussite."

SMTP GMAIL
----------
Pour configurer les emails réels Gmail, lire :
README_SMTP_GMAIL_SPRING_COMPLET.txt

Test rapide après lancement :
http://localhost:3000/api/email/status
