# 04 — Jenkins CI, Shared Library, and inbound agent

## Components

- **Controller:** Jenkins web UI, job configuration, scheduling, and build history.
- **Agent:** a separate execution node labelled `lab22-agent`; the pipeline runs on this node.
- **Jenkinsfile:** declares the pipeline stages.
- **Global Shared Library:** reusable Groovy functions under `vars/`. Here it is named `ivolve-gitops`.
- **Credentials:** Jenkins-managed secrets injected only for the step that needs them.

## Build the agent image

From the repository root, the agent Dockerfile packages the inbound-agent base, Node.js/npm, Docker tooling, and Trivy:

```powershell
docker build -t ivolve-jenkins-agent:lab23 -f agent/Dockerfile agent
```

This only builds the image; it does not connect an agent. In Jenkins, create an inbound node named `lab22-agent` with a remote work directory, obtain its connection secret from the node page, and start an agent container/process using the Jenkins URL, exact node name, secret, and WebSocket/TCP mode selected in Jenkins. Keep the secret out of shell history, Git, and screenshots. Confirm the node page says **Online**.

The agent image intentionally does not include `kubectl` and does not need a kubeconfig. Jenkins publishes the desired manifest change to GitHub; Argo CD detects and applies it from inside the cluster.

The agent needs access to the Docker daemon. In a local lab, that is often a mounted Docker socket and matching permissions. Access to that socket grants powerful control over the Docker host, so use this setup only in the isolated lab environment.

## Configure the Shared Library

In Jenkins **Manage Jenkins → System → Global Pipeline Libraries**:

1. Add a library called `ivolve-gitops` (must match `@Library('ivolve-gitops')` in the Jenkinsfile).
2. Select Git, enter this project's Git URL, and select `main`.
3. Add read credentials if the repository is private.
4. Save.

For a global library, Jenkins finds the reusable steps in the repository's root `vars/` directory.

## Credentials

In Jenkins Credentials create:

- `dockerhub-creds`: username/password credential, using a Docker Hub access token as the password.
- `github-write-creds`: username/password credential with a GitHub fine-grained token as the password. Grant Contents read/write only on this project repository.

The GitOps version does not require Jenkins to have a kubeconfig: it pushes a Git change and Argo CD applies it from inside the cluster. Do not add a kubeconfig unless a separate direct-kubectl task requires it.

## Pipeline behavior

1. `Build App` calls `BuildApp()`: installs npm dependencies, runs `npm test`, and checks `server.js` syntax.
2. `Build, Scan, Push Image` calls `BuildImage()`: builds an image tagged with `BUILD_NUMBER`, runs a Trivy vulnerability scan, logs in using `dockerhub-creds`, pushes the tag, logs out, and removes the local tag.
3. `Update Deployment` calls `UpdateDeployment()`: updates the image tag in `k8s/deployment.yaml`, commits it, and pushes `HEAD:main` using `github-write-creds`.

`--exit-code 0` means Trivy reports findings without failing the pipeline. To make high/critical findings block a release, change the exit-code policy after discussing the lab requirement.

## Configure and run a Jenkins job

1. Ensure the repository `Jenkinsfile` is the script path.
2. Use a Pipeline-from-SCM job or Multibranch Pipeline, with repository URL and branch `main`.
3. Set the job to run on agent label `lab22-agent` (already specified in the Jenkinsfile).
4. Click **Build Now**.
5. In Stage View/Console Output, verify build, scan, Docker push, and Git push succeeded.
6. If Jenkins says the node has no executor or build is waiting, check agent Online state and executors. If GitHub push is 403, check PAT scope, repository, and credential ID; do not put the token in the Jenkinsfile.

## Test result

The `test/` folder contains Node's built-in test-runner tests for `db-check.js`. On a developer machine with Node installed:

```powershell
npm install
npm test
```

No MySQL server is needed for these isolated unit tests; they use a small mock connection.
