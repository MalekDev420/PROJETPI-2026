# Étape 1 : Construction (Build)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Étape 2 : Exécution (Run)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# On expose le port 3000 (celui défini dans votre application.properties)
EXPOSE 3000

# Lancement de l'application
ENTRYPOINT ["java", "-jar", "app.jar"]