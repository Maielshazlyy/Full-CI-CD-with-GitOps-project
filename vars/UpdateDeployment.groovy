def call(Map config = [:]) {
    def imageRepo = config.imageRepo ?: "maielshazly/ivolve-app"
    def githubCredentialsId = config.githubCredentialsId ?: "github-write-creds"
    def imageTag = "${imageRepo}:${env.BUILD_NUMBER}"

    withEnv([
        "IMAGE_REPO=${imageRepo}",
        "IMAGE_TAG=${imageTag}"
    ]) {
        withCredentials([
            usernamePassword(
                credentialsId: githubCredentialsId,
                usernameVariable: "GITHUB_USER",
                passwordVariable: "GITHUB_TOKEN"
            )
        ]) {
            sh '''
                set +x
                set -eu

                sed -i "s|image: ${IMAGE_REPO}:.*|image: ${IMAGE_TAG}|" k8s/deployment.yaml
                grep -F "image: ${IMAGE_TAG}" k8s/deployment.yaml

                git config user.name "Jenkins"
                git config user.email "jenkins@users.noreply.github.com"
                git add k8s/deployment.yaml

                if git diff --cached --quiet; then
                    echo "No manifest change to commit."
                    exit 0
                fi

                git commit -m "Deploy ${IMAGE_TAG}"

                credentials_file="$(mktemp)"
                trap 'rm -f "$credentials_file"' EXIT
                chmod 600 "$credentials_file"
                printf 'https://%s:%s@github.com\\n' "$GITHUB_USER" "$GITHUB_TOKEN" > "$credentials_file"

                git -c "credential.helper=store --file=$credentials_file" push origin HEAD:main
            '''
        }
    }
}