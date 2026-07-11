pipeline {
    agent any

    tools {
        // Assurez-vous que les noms correspondent à votre configuration Jenkins (Global Tool Configuration)
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    stages {
        stage('Checkout') {
            steps {
                // Récupère le code depuis votre dépôt GitHub
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                // Compile et lance les tests unitaires
                bat 'mvn clean package'
            }
        }
    }

    post {
        success {
            echo 'Le build et les tests ont réussi !'
        }
        failure {
            echo 'Le build a échoué. Veuillez vérifier les logs.'
        }
    }
}