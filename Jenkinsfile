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
                // J'ai retiré le -DskipTests pour permettre à JaCoCo de calculer la couverture
                bat 'mvn clean package'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    // Utilise la configuration 'SonarQube' définie dans Manage Jenkins > System
                    withSonarQubeEnv('SonarQube') {
                        bat 'mvn sonar:sonar'
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    // Connexion à Docker Hub
                    docker.withRegistry('', 'ocker-hub-credentials') {
                        // Construction de l'image
                        def customImage = docker.build("malekdev80/mon-backend-spring")
                        // Envoi sur le Hub
                        customImage.push("latest")
                    }
                }
            }
        }
    }
}