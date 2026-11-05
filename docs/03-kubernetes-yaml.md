# 03 — Kubernetes YAML labs and apply order

## How declarative YAML works

A Kubernetes manifest describes the desired state. `kubectl apply` submits it to the API server; controllers reconcile actual resources toward the declared state. Keep one intentional, current manifest per resource in the final GitOps directory. The `kubernetes-labs/LabXX` folders are historical learning snapshots and contain repeated versions of some resources—do not apply every folder as one bundle.

Validate and inspect:

```bash
kubectl apply --dry-run=server -f <file.yaml>
kubectl apply -f <file.yaml>
kubectl get <kind> -n ivolve
kubectl describe <kind> <name> -n ivolve
```

## What each resource contributes

- **Namespace (`Lab11`)**: separates this team's objects into `ivolve`.
- **ResourceQuota (`Lab11`)**: caps namespace resource counts, such as Pods. A quota error means the requested object exceeds the remaining allowance; inspect and clean up old lab Pods first.
- **ConfigMap (`Lab12`)**: stores non-secret settings such as `DB_HOST` and `DB_USER`.
- **Secret (`Lab12`)**: stores database passwords, consumed through `secretKeyRef`. The real `secret.yaml` was intentionally excluded from this repository. The example contains fake placeholders only.
- **PV/PVC (`Lab13`)**: PV represents storage; PVC requests it. With `hostPath`, data lives on one node's filesystem and is not shared storage across nodes despite an RWX access-mode label. Use only as a lab demonstration.
- **MySQL PV, headless Service, StatefulSet (`Lab14`)**: gives MySQL a stable identity and persistent claim. StatefulSet creates `mysql-data-ivolvedb-0` from its claim template.
- **App Deployment and ClusterIP Service (`Lab15`)**: maintains app replicas and gives them a stable in-cluster endpoint.
- **Resource requests/limits (`Lab16`)**: requests affect scheduling; limits cap container use. A 1Gi request per app Pod cannot fit two replicas on a 2Gi worker once the OS/system Pods use memory.
- **NetworkPolicy (`Lab17`)**: allows MySQL ingress on TCP 3306 only from Pods with the app label; enforcement requires a compatible CNI such as Calico.
- **Monitoring (`Lab18`)**: node-exporter DaemonSet places one exporter on each eligible node; Prometheus scrapes metrics; Grafana displays them; Alertmanager handles alerts. This stack can consume substantial memory.
- **ServiceAccount/Role/RoleBinding (`Lab19`)**: gives a scoped identity permissions in a namespace. A Role is namespaced, and a RoleBinding connects that Role to a ServiceAccount.

## Safe ordering for first setup

1. Apply/verify `ivolve` namespace and quota.
2. Create ConfigMap and a real Secret securely. For example, interactively read passwords and create the Secret with `kubectl create secret generic ... --from-literal ... --dry-run=client -o yaml`, but do not commit the generated populated file. Applying directly with `kubectl create secret generic ...` is also an option.
3. Create PV/PVC and verify claims are `Bound`.
4. Create the MySQL PV, headless service, and StatefulSet. Wait until the MySQL Pod is Ready.
5. Apply the current app Deployment/Service and NetworkPolicy (in the final project these are under `k8s/`).
6. Verify app logs and MySQL connectivity.
7. Add optional monitoring and RBAC resources only when required for that lab and when the cluster has enough capacity.

## Final GitOps directory

Argo CD watches only the top-level `k8s/` directory. It contains the final app Deployment, its Service, and the MySQL NetworkPolicy. MySQL, namespace, Secret, ConfigMap, and storage are established separately by the earlier labs. Keeping historic examples in `kubernetes-labs/` prevents Argo CD from accidentally applying duplicate or unrelated resources.

Inspect final app state:

```bash
kubectl get deploy,pods,svc,networkpolicy -n ivolve
kubectl get pv,pvc -n ivolve
kubectl logs -n ivolve -l app=ivolveapp -c ivolve-app --tail=50
```
