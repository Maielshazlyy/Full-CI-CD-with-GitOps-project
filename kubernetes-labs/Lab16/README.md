# Kubernetes — Lab 16: Pod Resource Management

## Objective

Update the existing `ivolveapp` Deployment in the `ivolve` namespace with CPU and memory requests and limits, then verify the configuration and observe actual Pod resource usage.

## Resource Configuration

The `ivolve-app` container is configured with:

| Resource | Request | Limit |
|---|---:|---:|
| CPU | `1` core | `2` cores |
| Memory | `1Gi` | `2Gi` |

A **request** is the amount Kubernetes uses when deciding whether a node has enough capacity to schedule a Pod. A **limit** is the maximum resource amount the container can use.

## Apply the Deployment

Apply the updated manifest:

```bash
kubectl apply --dry-run=server -f ivolve-app.yaml
kubectl apply -f ivolve-app.yaml
```

Check the rollout and Pods:

```bash
kubectl rollout status deployment/ivolveapp -n ivolve
kubectl get deployment ivolveapp -n ivolve
kubectl get pods -n ivolve -o wide
```

## Verify Requests and Limits

Inspect a Pod belonging to the Deployment:

```bash
kubectl describe pod -n ivolve -l app=ivolveapp
```

The `ivolve-app` container should show the configured `Requests` and `Limits`. Its QoS class should be `Burstable`.

You can also check the resources saved on the Deployment:

```bash
kubectl get deployment ivolveapp -n ivolve \
  -o jsonpath='{.spec.template.spec.containers[?(@.name=="ivolve-app")].resources}{"\n"}'
```

## Monitor Resource Usage

Metrics Server must be running in the cluster for these commands:

```bash
kubectl top nodes
kubectl top pods -n ivolve
```

`kubectl top` displays current usage. These values can be lower than the configured requests; requests are scheduling values, not guaranteed minimum usage.

## Capacity Note

The namespace has a Pod quota of 3, already used by the MySQL Pod and the two application replicas. A rolling update may need a temporary extra Pod and can be blocked by that quota. Setting `maxSurge: 0` and `maxUnavailable: 1` lets Kubernetes replace one application Pod at a time without requesting a fourth Pod.

The worker node used for this lab has about `1.76Gi` of allocatable memory. Two application replicas each requesting `1Gi` need `2Gi` in total, so both updated replicas may not fit on that worker. If a Pod remains `Pending`, inspect its events:

```bash
kubectl describe pod -n ivolve <pending-pod-name>
kubectl get events -n ivolve --sort-by=.lastTimestamp
```

To run both replicas with the specified requests, the worker needs enough allocatable memory for both Pods.

## Files

- `ivolve-app.yaml` — Deployment and Service manifest, including the application container resource requests and limits.
<img width="1532" height="572" alt="image" src="https://github.com/user-attachments/assets/d936826b-dd4e-494f-ab0e-31a155db19d2" />
<img width="961" height="102" alt="image" src="https://github.com/user-attachments/assets/d87efc9f-f3dd-45a8-9294-4e6470a844cb" />


  
