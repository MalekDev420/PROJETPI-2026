SMTP Gmail est déjà configuré dans src/main/resources/application.properties.

Commandes:
1) mvn clean install
2) mvn spring-boot:run

Test statut:
http://localhost:3000/api/email/status

Test envoi avec PowerShell:
Invoke-RestMethod -Uri http://localhost:3000/api/email/test -Method POST -ContentType "application/json" -Body '{"to":"malekkassem80@gmail.com"}'

Remarque sécurité: après la soutenance, régénérez le mot de passe d’application Google car il a été partagé dans le chat.
