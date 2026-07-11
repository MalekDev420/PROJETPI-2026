@echo off
cls
echo =============================================
echo  Gestion Missions Spring - Lancement SMTP
 echo =============================================
echo.
set /p SMTP_EMAIL=Adresse Gmail SMTP: 
set /p SMTP_PASSWORD=Mot de passe application Gmail: 
echo.
echo Lancement du backend Spring Boot...
mvn spring-boot:run
pause
