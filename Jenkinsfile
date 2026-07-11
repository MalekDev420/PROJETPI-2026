pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                // Compilation du projet pour générer le JAR
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                // Construction de l'image Docker
                // Assurez-vous que Docker est bien lancé sur votre machine
                bat 'docker build -t mon-backend-spring .'
            }
        }
    }

    post {
        success {
            echo 'Pipeline réussi : Le JAR est généré et l\'image Docker a été construite.'
        }
        failure {
            echo 'Pipeline échoué. Vérifiez vos étapes.'
        }
    }
}