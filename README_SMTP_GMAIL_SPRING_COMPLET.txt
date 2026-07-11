CONFIGURATION GMAIL SMTP - BACKEND SPRING BOOT
==============================================

Objectif :
Activer les vrais emails automatiques dans le backend Spring Boot :
- acceptation/refus d'une mission par un développeur ;
- livraison du travail avec lien Git/GitHub/Drive ;
- validation/refus du travail par le client ;
- mot de passe oublié ;
- emails de test.

IMPORTANT
---------
Gmail n'accepte pas votre mot de passe normal dans une application externe.
Il faut utiliser un MOT DE PASSE D'APPLICATION Google.

ETAPE 1 - Activer la validation 2 étapes Google
-----------------------------------------------
1. Ouvrir votre compte Google.
2. Aller dans Sécurité.
3. Activer Validation en 2 étapes.

ETAPE 2 - Créer un mot de passe d'application
---------------------------------------------
1. Aller dans : https://myaccount.google.com/apppasswords
2. Choisir une application : Mail.
3. Choisir un appareil : Windows Computer.
4. Google donne un mot de passe de 16 caractères.
5. Copier ce mot de passe.

Exemple de format :
abcd efgh ijkl mnop

Vous pouvez garder les espaces ou les supprimer.

ETAPE 3 - Méthode recommandée : variables d'environnement CMD
-------------------------------------------------------------
Dans CMD, dans le dossier backend :

set SMTP_EMAIL=votre.email@gmail.com
set SMTP_PASSWORD=votre_mot_de_passe_application
mvn spring-boot:run

Exemple :

set SMTP_EMAIL=malekkassem80@gmail.com
set SMTP_PASSWORD=abcd efgh ijkl mnop
mvn spring-boot:run

ATTENTION : ces variables restent seulement dans cette fenêtre CMD.
Si vous fermez CMD, il faut les retaper.

ETAPE 4 - Alternative : modifier application.properties
------------------------------------------------------
Fichier :
src/main/resources/application.properties

Remplacer :

spring.mail.username=${SMTP_EMAIL:}
spring.mail.password=${SMTP_PASSWORD:}

Par :

spring.mail.username=votre.email@gmail.com
spring.mail.password=votre_mot_de_passe_application

Puis lancer :

mvn spring-boot:run

ETAPE 5 - Tester SMTP dans le navigateur
----------------------------------------
Après lancement du backend :

http://localhost:3000/api/email/status

Il faut voir :
configured: true

Pour envoyer un test, utilisez Postman ou Thunder Client :

POST http://localhost:3000/api/email/test
Content-Type: application/json

{
  "to": "votre.email@gmail.com"
}

Le résultat est aussi sauvegardé dans la table :
email_logs

ETAPE 6 - Tester via phpMyAdmin
-------------------------------
Ouvrir :
http://localhost/phpmyadmin

Base : gestion_missions_spring
Table : email_logs

Vérifier le champ status :
- sent : email envoyé ;
- logged_only : SMTP non configuré ;
- error: ... : problème Gmail ou mot de passe.

ERREURS FREQUENTES
------------------
1. Username and Password not accepted
=> Vous avez utilisé le mot de passe Gmail normal au lieu du mot de passe d'application.

2. SMTP non configuré
=> SMTP_EMAIL ou SMTP_PASSWORD vide.

3. Port bloqué
=> Vérifier connexion Internet et antivirus/firewall.

4. Email dans spam
=> Vérifier dossier Spam/Promotions.

CE QU'IL FAUT DIRE A LA PROF
----------------------------
"Dans la version Spring Boot, j'ai intégré JavaMailSender avec Gmail SMTP.
Les emails sont envoyés automatiquement selon le workflow métier : acceptation,
refus, livraison, validation, mot de passe oublié. Chaque envoi est aussi tracé
dans la table email_logs pour garder un historique vérifiable en base MySQL."

ARCHITECTURE
------------
Angular -> Spring Boot REST API -> MySQL
                        |
                        -> JavaMailSender Gmail SMTP
                        -> Ollama Assistant IA

