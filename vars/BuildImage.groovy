def call(Map config = [:]) {
    def imageRepo = config.imageRepo ?: "maielshazly/ivolve-app"
    def credentialsId = config.credentialsId ?: "dockerhub-creds"
    def imageTag = "${imageRepo}:${env.BUILD_NUMBER}"

    sh "docker build -t ${imageTag} ."

    sh """
        trivy image \
          --db-repository ghcr.io/aquasecurity/trivy-db:2 \
          --scanners vuln \
          --severity HIGH,CRITICAL \
          --exit-code 0 \
          ${imageTag}
    """

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: "DOCKERHUB_USER",
            passwordVariable: "DOCKERHUB_TOKEN"
        )
    ]) {
        sh """
            set +x
            echo "\$DOCKERHUB_TOKEN" | docker login -u "\$DOCKERHUB_USER" --password-stdin
            docker push ${imageTag}
            docker logout
        """
    }

    sh "docker rmi ${imageTag}"
}
