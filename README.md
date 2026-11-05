# Full CI/CD with GitOps Project

A single end-to-end learning project connecting the Node.js/MySQL app from Docker Compose Lab 8 to a kubeadm Kubernetes cluster, Jenkins CI with a dedicated agent and Shared Library, and Argo CD continuous delivery.

## Learning path

1. **Docker Compose:** run the Node.js app and MySQL as two services; understand networking, health checks, and persistent named volumes.
2. **Kubernetes cluster:** prepare two Ubuntu EC2 machines, configure containerd and kubeadm, initialize a control plane, install Calico, and join the worker.
3. **Kubernetes YAML:** define namespace/quota, ConfigMap and Secret, PV/PVC, MySQL StatefulSet and Service, app Deployment and Service, resource limits, NetworkPolicy, monitoring, and RBAC.
4. **Jenkins CI:** use a Jenkins controller, an online `lab22-agent`, a declarative Jenkinsfile, and reusable functions in the `vars/` Shared Library.
5. **Argo CD GitOps:** Jenkins pushes the newly built image tag into the Git manifest; Argo CD watches `k8s/` and automatically synchronizes the change to the cluster.

## Architecture

```text
Git commit -> Jenkins controller -> lab22-agent -> test/build -> Trivy scan
                                                    |               |
                                                    |               +-> Docker Hub image tag
                                                    +-> commit image tag to Git
                                                                     |
Argo CD watches Git path k8s/ <--------------------------------------+
       |
       +-> reconciles Kubernetes Deployment + Service -> app Pods -> MySQL
```

Jenkins builds and publishes images and updates the desired manifest in Git. Argo CD is the component that applies GitOps changes to Kubernetes. The database, ConfigMap, Secret, and persistent storage are prerequisites created by the earlier Kubernetes labs.

## Repository contents

- `Dockerfile`, `docker-compose.yml`, `server.js`, `db.js`, `frontend/`: Lab 8 Node.js/MySQL application and local container setup.
- `.env.example`: placeholder template for local-only Compose values. Copy it to `.env`; `.env` is ignored by Git.
- `kubernetes-labs/Lab11`–`Lab19`: safe historical/reference manifests and README files. The real `secret.yaml` is deliberately excluded.
- `k8s/`: the final app Deployment, ClusterIP Service, and MySQL NetworkPolicy watched by Argo CD. Keep unrelated lab manifests outside this directory.
- `Jenkinsfile`, `vars/`, `agent/Dockerfile`: pipeline, reusable Shared Library functions, and agent image definition.
- `argocd/application.yaml`: template for the Argo CD Application. Replace the repository placeholders before applying it.
- `docs/`: detailed guides for each phase.

## Quick start: Docker Compose

Run from the project root in PowerShell:

```powershell
Copy-Item .env.example .env
notepad .env
# Set local DB_PASSWORD and MYSQL_ROOT_PASSWORD values; save and close.
docker compose config
docker compose up -d --build
docker compose ps
docker compose logs -f app
```

Open `http://localhost:3000` and `http://localhost:3000/health`. Stop containers while preserving named volume data with `docker compose down`. `docker compose down -v` deletes the named volumes and their data.

## Kubernetes and kubeadm

The original lab uses one EC2 control plane and one worker. Keep both nodes on a compatible Ubuntu/Kubernetes/containerd setup. Use the current private IPs and security-group rules, because EC2 public IPs can change after stop/start. Restrict SSH and the Kubernetes API to trusted sources.

At a high level: disable swap; configure kernel modules and sysctl; install/configure containerd; install matching `kubelet`, `kubeadm`, and `kubectl`; run `kubeadm init` on the control plane with the selected Pod CIDR; configure kubeconfig; install Calico; run the generated `kubeadm join` on the worker; then verify both nodes are Ready. The Pod CIDR must match the CNI configuration. Detailed commands and rationale are in [`docs/02-kubeadm-cluster.md`](docs/02-kubeadm-cluster.md).

## Jenkins setup

1. Build or prepare the agent using `agent/Dockerfile`. The agent needs Git, Node.js/npm, Docker CLI with access to the Docker daemon, Trivy, and required shell tools.
2. Create the Jenkins inbound agent named `lab22-agent`, launch it with its connection secret, and verify Jenkins shows it **Online**. Never save the secret in Git.
3. Configure a Global Pipeline Library named `ivolve-gitops` pointing at this repository's `main` branch. The `vars/` folder must be at the repository root.
4. Add Jenkins credentials: `dockerhub-creds` (Docker Hub username/token) and `github-write-creds` (GitHub write token for this repository).
5. Create a Pipeline job using this repository and `Jenkinsfile`, then run it.
6. Confirm the tests, image build/scan/push, and manifest commit/push all succeed. A GitHub 403 means the GitOps manifest did not update even if the image was pushed.

See [`docs/04-jenkins-ci.md`](docs/04-jenkins-ci.md).

## Argo CD setup

Install Argo CD in the `argocd` namespace and create the Application using [`argocd/application.yaml`](argocd/application.yaml) or the Argo CD UI. Set the repository to this new repository, revision `main`, path `k8s`, destination cluster `in-cluster`, and destination namespace `ivolve`. Enable automated synchronization and self-heal. Replace `<GITHUB_OWNER>/<REPOSITORY_NAME>` before applying the template. Do not create a second `ivolve` Application if one already exists.

After Jenkins commits an updated image tag, Argo CD should detect the new Git revision and report **Synced** and **Healthy** after Kubernetes rolls out Ready Pods. See [`docs/05-argocd-gitops.md`](docs/05-argocd-gitops.md).

## Security

Never commit `.env`, populated Kubernetes Secrets, AWS private keys, kubeconfig files, kubeadm join tokens, Docker Hub/GitHub tokens, or Jenkins agent secrets. Only placeholder secret examples belong in a public repository. The previous EC2 instances are small and incur charges while running; stop them when you have finished the lab and no longer need the cluster.

## Detailed documentation

- [01 — Docker Compose and app/DB flow](docs/01-docker-compose.md)
- [02 — kubeadm cluster setup](docs/02-kubeadm-cluster.md)
- [03 — Kubernetes YAML labs](docs/03-kubernetes-yaml.md)
- [04 — Jenkins Shared Library and agent](docs/04-jenkins-ci.md)
- [05 — Argo CD GitOps](docs/05-argocd-gitops.md)

## Official references

- [Docker Compose](https://docs.docker.com/compose/)
- [Kubernetes kubeadm installation](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/install-kubeadm/)
- [Create a cluster using kubeadm](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/create-cluster-kubeadm/)
- [Jenkins agents](https://www.jenkins.io/doc/book/using/using-agents/)
- [Jenkins Shared Libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)
- [Argo CD Getting Started](https://argo-cd.readthedocs.io/en/stable/getting_started/)
