# Kubernetes Lab 11: Namespace Management and Resource Quota

## Objective

Create a namespace named `ivolve`, apply a ResourceQuota that allows up to three Pods in that namespace, and verify that Kubernetes rejects a fourth Pod.

## Concepts

- A **namespace** provides a logical boundary for organizing Kubernetes resources.
- A **ResourceQuota** limits how many resources of a specified type can exist in a namespace.
- The `pods` quota counts non-terminal Pods in that namespace, including Pods in the `Pending` state.

## 1. Create the namespace manifest

Generate the YAML without creating the namespace yet:

```bash
kubectl create namespace ivolve \
  --dry-run=client -o yaml > namespace.yaml
```

Review and validate the manifest, then apply it:

```bash
cat namespace.yaml
kubectl apply --dry-run=server -f namespace.yaml
kubectl apply -f namespace.yaml
kubectl get namespace ivolve
```

## 2. Create the ResourceQuota manifest

Kubernetes resource names must be lowercase. The lab sheet calls the quota `ivolveQuota`, but Kubernetes rejects uppercase letters in resource names, so this manifest uses `ivolvequota`.

Generate the YAML:

```bash
kubectl create quota ivolvequota \
  --hard=pods=3 \
  --namespace=ivolve \
  --dry-run=client -o yaml > resourcequota.yaml
```

Review, validate, and apply it:

```bash
cat resourcequota.yaml
kubectl apply --dry-run=server -f resourcequota.yaml
kubectl apply -f resourcequota.yaml
```

## 3. Verify the quota

```bash
kubectl get resourcequota ivolvequota -n ivolve
kubectl describe resourcequota ivolvequota -n ivolve
```

The quota should show a hard limit of three Pods.

## 4. Test the limit

First, make sure there are no existing Pods in the namespace:

```bash
kubectl get pods -n ivolve
```

Create three test Pods:

```bash
for i in 1 2 3; do
  kubectl run "quota-test-$i" \
    --image=registry.k8s.io/pause:3.10 \
    --restart=Never \
    --namespace=ivolve
done
```

Check the Pods and quota usage:

```bash
kubectl get pods -n ivolve
kubectl describe resourcequota ivolvequota -n ivolve
```

After the quota reports three Pods used, try to create a fourth:

```bash
kubectl run quota-test-4 \
  --image=registry.k8s.io/pause:3.10 \
  --restart=Never \
  --namespace=ivolve
```

Kubernetes should reject this request with an `exceeded quota` message. This confirms that the limit is enforced.

Pods may remain `Pending` because of node scheduling rules from Lab 10. Pending Pods still count toward the quota while they are non-terminal.

## 5. Remove the test Pods

Remove only the test Pods after recording the result:

```bash
kubectl delete pod quota-test-1 quota-test-2 quota-test-3 -n ivolve
```

Keep the `ivolve` namespace and `ivolvequota` ResourceQuota because Lab 12 uses the same namespace.

## Files

- `namespace.yaml` — creates the `ivolve` namespace.
- `resourcequota.yaml` — limits the namespace to three Pods.
- `README.md` — describes the steps and verification.

## Results

Record the namespace and quota verification, plus the rejection message from the fourth Pod creation attempt. Screenshots or sanitized terminal output can be saved under a `results/` directory.
<img width="1195" height="210" alt="image" src="https://github.com/user-attachments/assets/2e9dbfbd-741c-400f-9328-048ebf40dda5" />
<img width="1600" height="136" alt="image" src="https://github.com/user-attachments/assets/db9510ea-da4d-4750-b354-27c2c9b696f8" />

