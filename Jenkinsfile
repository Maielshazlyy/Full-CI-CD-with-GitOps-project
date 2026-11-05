@Library('ivolve-gitops') _

pipeline {
    agent { label 'lab22-agent' }
    options { timestamps() }
    environment { IMAGE_REPO = 'maielshazly/ivolve-app' }
    stages {
        stage('Build App') {
            steps { BuildApp() }
        }
        stage('Build, Scan, Push Image') {
            steps {
                BuildImage(imageRepo: env.IMAGE_REPO, credentialsId: 'dockerhub-creds')
            }
        }
        stage('Update GitOps Manifest') {
            steps {
                DeployOnK8s(imageRepo: env.IMAGE_REPO, githubCredentialsId: 'github-write-creds')
            }
        }
    }
}
