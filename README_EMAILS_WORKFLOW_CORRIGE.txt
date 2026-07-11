MISE A JOUR EMAILS WORKFLOW

Les emails SMTP sont maintenant envoyés aussi dans les étapes suivantes :

1) Le développeur livre le travail :
- email au client avec description + lien Git/Drive/Démo

2) Le client valide ou refuse le travail :
- email au développeur
- email de confirmation au client
- notification interne enregistrée

3) Le client effectue le paiement simulé :
- email au développeur
- email au client
- notification interne enregistrée
- réputation développeur mise à jour

La configuration SMTP reste dans :
src/main/resources/application.properties
