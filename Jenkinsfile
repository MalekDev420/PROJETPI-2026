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
                bat 'mvn clean package'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('SonarQube') {
                        bat 'mvn sonar:sonar'
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    docker.withRegistry('', 'ocker-hub-credentials') {
                        def customImage = docker.build("malekdev80/mon-backend-spring")
                        customImage.push("latest")
                    }
                }
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                script {
                    // Cette commande va arrêter les conteneurs, 
                    // tirer la nouvelle image et relancer le service
                    bat 'docker-compose down' // Ajoute cette ligne pour nettoyer avant
                    bat 'docker-compose pull'
                    bat 'docker-compose up -d'
                }
            }
        }
    }
}