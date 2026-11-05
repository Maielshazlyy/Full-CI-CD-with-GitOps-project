# 05 — Argo CD GitOps continuous delivery

## GitOps idea

Git stores the intended cluster state. Argo CD continuously compares that state with live Kubernetes objects and reconciles differences. Jenkins builds and pushes a new app image, then commits the new image reference to Git. Argo CD notices the commit and syncs it. The cluster does not rely on a Jenkins `kubectl apply` step in this design.

## Install Argo CD

On a healthy cluster with sufficient capacity:

```bash
kubectl create namespace argocd
kubectl apply --server-side --force-conflicts -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl get pods -n argocd -o wide
```

Wait for the required controller/server/repo-server/Redis pods to become Ready. On the small t3.small lab nodes, stop optional monitoring workloads if necessary and check `free -h`, `uptime`, and `kubectl describe pod` events. Do not remove taints cluster-wide as a shortcut; configure tolerations deliberately if the task requires system components on those nodes.

To access the UI securely from the control-plane:

```bash
kubectl port-forward -n argocd svc/argocd-server 8081:443
```

Browse from the same machine/tunnel context to `https://localhost:8081`. If Kubernetes returns Forbidden, the kubeconfig identity lacks permission to read the Service; use an authorized identity rather than broadening RBAC without need.

## Create the `ivolve` Application

The `argocd/application.yaml` template is already set to the project repository; verify its URL, or fill the same values in the UI:

- Name: `ivolve`
- Project: `default`
- Repository: this project's Git URL
- Revision: `main`
- Path: `k8s`
- Destination: in-cluster (`https://kubernetes.default.svc`)
- Namespace: `ivolve`
- Sync policy: automatic; self-heal enabled; prune disabled in this template

For a private repository, configure read access in Argo CD repository settings. Do not store a token in the Application manifest.

Do not apply a second Application with name `ivolve` if the live one already exists. Either update the existing Application source to this new repository or delete/recreate it only when you have confirmed that is intended.

## Verify the deployment loop

1. Trigger Jenkins and wait for every stage to complete.
2. Inspect Git: a new commit should update `k8s/deployment.yaml` to the new image tag.
3. Inspect Argo CD: the app should detect a new revision, synchronize, and become **Healthy** and **Synced**.
4. Inspect Kubernetes:

```bash
kubectl get applications.argoproj.io ivolve -n argocd
kubectl get deployment,pods,service -n ivolve -o wide
kubectl get deployment ivolveapp -n ivolve \
  -o jsonpath='{.spec.template.spec.containers[?(@.name=="ivolve-app")].image}{"\n"}'
```

The image shown by the last command should match the build tag Jenkins pushed.

## Troubleshooting

- **Argo app is OutOfSync:** verify repository URL, branch, path, and that Jenkins really pushed the manifest commit.
- **Argo app is Synced but unhealthy:** inspect Pod events, readiness, image pull errors, required Secret/ConfigMap/PVC, and database connectivity.
- **Jenkins image push passed but Argo did not change:** the later Git commit/push likely failed. Check for 403 in Jenkins logs and check the new commit on GitHub.
- **App stuck Progressing:** inspect the underlying Deployment and Pod rollout; synchronization can finish before all replicas become Ready.
- **Argo UI loading/API timeouts:** check API-server readiness, node memory, CPU, and I/O pressure; small lab nodes may be overloaded.

```bash
kubectl describe deployment ivolveapp -n ivolve
kubectl get events -n ivolve --sort-by=.lastTimestamp | tail -20
kubectl logs -n ivolve -l app=ivolveapp -c ivolve-app --tail=50
```
