pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
        // 'sonar-scanner' est le nom que vous avez donné dans Manage Jenkins > Tools
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

        stage('SonarQube Analysis') {
            steps {
                script {
                    // Utilise la configuration 'SonarQube' définie dans Manage Jenkins > System
                    withSonarQubeEnv('SonarQube') {
                        // Utilise le scanner installé automatiquement
                        def scannerHome = tool 'sonar-scanner'
                        bat "${scannerHome}/bin/sonar-scanner " +
                            "-Dsonar.projectKey=Pipeline-Backend " +
                            "-Dsonar.sources=src " +
                            "-Dsonar.java.binaries=target/classes " +
                            "-Dsonar.host.url=http://localhost:9000 "
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    // Connexion à Docker Hub (ID corrigé)
                    docker.withRegistry('', 'ocker-hub-credentials') {
                        // Construction
                        def customImage = docker.build("malekdev80/mon-backend-spring")
                        // Push
                        customImage.push("latest")
                    }
                }
            }
        }
    }
}