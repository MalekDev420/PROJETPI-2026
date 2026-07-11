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
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    // 1. Connexion à Docker Hub
                    docker.withRegistry('', 'docker-hub-credentials') {
                        // 2. Construire l'image
                        def customImage = docker.build("malekdev80/mon-backend-spring")
                        
                        // 3. Pousser l'image
                        customImage.push("latest")
                    }
                }
            }
        }
    }
}