# Kubernetes Lab 10: Node Isolation Using Taints and Labels

## Objective

Apply a taint and a label to the worker node, then verify that both are configured correctly.

## Concepts

- **Taint:** Marks a node so that Pods without a matching toleration are not scheduled there.
- **Toleration:** Allows a Pod to be scheduled on a node with a matching taint. A toleration allows placement but does not select the node by itself.
- **`NoSchedule`:** Prevents new Pods without a matching toleration from being scheduled on the node. Existing Pods are not evicted.
- **Label:** A key-value tag used to identify and select nodes.
- **Node selector and node affinity:** Pod scheduling rules that can select nodes based on their labels. They are concepts related to this lab; this lab does not require creating or scheduling a Pod.

## Worker Node

The worker node used for this lab is:

```text
ip-172-31-9-53
```

To check the current node names:

```bash
kubectl get nodes -o wide
```

## Steps

### 1. Apply the taint

```bash
kubectl taint node ip-172-31-9-53 workload=ivolveApp:NoSchedule
```

This taint prevents new Pods without a matching toleration from being scheduled on the worker.

### 2. Apply the label

```bash
kubectl label node ip-172-31-9-53 project=ivolve --overwrite
```

The label identifies the worker as part of the `ivolve` project. It can be used by Pod scheduling rules such as `nodeSelector` or node affinity.

### 3. Verify the taint and label

```bash
kubectl describe node ip-172-31-9-53
kubectl get nodes --show-labels
```

In the `describe` output, confirm that the taint appears as:

```text
workload=ivolveApp:NoSchedule
```

In the node labels, confirm that the worker has:

```text
project=ivolve
```


## Result

The worker node has the required `workload=ivolveApp:NoSchedule` taint and `project=ivolve` label.
<img width="1600" height="419" alt="image" src="https://github.com/user-attachments/assets/a3eb965f-4ade-4512-9fe2-86e83dfd1df5" />
<img width="1600" height="143" alt="image" src="https://github.com/user-attachments/assets/fa571b98-d327-4b9c-939f-8fb5dec618b4" />

