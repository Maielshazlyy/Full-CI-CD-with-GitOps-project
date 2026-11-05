# Kubernetes — Lab 19: Securing Kubernetes with RBAC and Service Accounts

## Objective

Create a ServiceAccount for Jenkins in the `ivolve` namespace and grant it permission to create and update Deployments in that namespace. Verify that it cannot perform unrelated actions or access Deployments in another namespace.

## Kubernetes Resources

- **ServiceAccount — `jenkinssa`:** The identity Jenkins uses to authenticate to the Kubernetes API.
- **Role — `ivolvedeployrole`:** Grants `create` and `update` permissions on Deployments in the `ivolve` namespace.
- **RoleBinding — `ivolvedeployrolebinding`:** Connects the Role to the ServiceAccount.

Resource names are lowercase because Kubernetes requires lowercase names.

## Manifest Files

- `serviceaccount.yaml` — creates the `jenkinssa` ServiceAccount in `ivolve`.
- `role.yaml` — permits creating and updating Deployments in `ivolve`.
- `rolebinding.yaml` — binds the Role to the ServiceAccount.

## Apply the Manifests

Run these commands from the directory containing the manifest files:

```bash
kubectl apply --dry-run=server -f serviceaccount.yaml
kubectl apply --dry-run=server -f role.yaml
kubectl apply --dry-run=server -f rolebinding.yaml

kubectl apply -f serviceaccount.yaml
kubectl apply -f role.yaml
kubectl apply -f rolebinding.yaml
```

## Create a ServiceAccount Token

```bash
kubectl create token jenkinssa -n ivolve
```

The token is a credential. Do not commit it to GitHub, include it in this README, or show it in screenshots.

## Verify the Resources

```bash
kubectl get serviceaccount,role,rolebinding -n ivolve
kubectl describe role ivolvedeployrole -n ivolve
kubectl describe rolebinding ivolvedeployrolebinding -n ivolve
```

## Verify the Permissions

Check that the ServiceAccount can create and update Deployments in `ivolve`:

```bash
kubectl auth can-i create deployments.apps \
  --as=system:serviceaccount:ivolve:jenkinssa \
  -n ivolve

kubectl auth can-i update deployments.apps \
  --as=system:serviceaccount:ivolve:jenkinssa \
  -n ivolve
```

Both commands should return `yes`.

Check that its permissions are limited:

```bash
kubectl auth can-i delete deployments.apps \
  --as=system:serviceaccount:ivolve:jenkinssa \
  -n ivolve

kubectl auth can-i create configmaps \
  --as=system:serviceaccount:ivolve:jenkinssa \
  -n ivolve

kubectl auth can-i create deployments.apps \
  --as=system:serviceaccount:ivolve:jenkinssa \
  -n kube-system
```

These commands should return `no`.

## Expected Result

The `jenkinssa` ServiceAccount can create and update Deployments in the `ivolve` namespace. It cannot delete Deployments, create ConfigMaps, or create Deployments in the `kube-system` namespace.

## Evidence to Submit

Capture screenshots showing:

1. The ServiceAccount, Role, and RoleBinding in the `ivolve` namespace.
2. The Role rules and RoleBinding subject.
3. Permission checks showing `yes` for creating and updating Deployments in `ivolve`.
4. Permission checks showing `no` for actions outside the Role's scope.
<img width="1282" height="285" alt="image" src="https://github.com/user-attachments/assets/ca5cc8ee-e706-4a0d-bf7f-abea90459e2d" />
<img width="1260" height="477" alt="image" src="https://github.com/user-attachments/assets/65914cbd-5a50-4694-bf82-f6af477d6a19" />
<img width="1600" height="279" alt="WhatsApp Image 2026-11-02 at 6 49 54 PM" src="https://github.com/user-attachments/assets/2b7ed4a0-80bd-451c-affb-940da205e858" />

