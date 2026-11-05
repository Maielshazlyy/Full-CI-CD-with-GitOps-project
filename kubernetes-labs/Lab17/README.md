# Kubernetes — Lab 17: Control Pod-to-Pod Traffic with NetworkPolicy

## Objective

Create a NetworkPolicy in the `ivolve` namespace that selects the MySQL Pod and allows incoming traffic only from the application Pods on TCP port `3306`.

## Existing Pod Labels

- MySQL: `app=ivolvedb`
- Application: `app=ivolveapp`

The policy uses these labels to identify the destination and permitted source Pods.

## NetworkPolicy Manifest

The manifest is saved in [`network-policy.yaml`](network-policy.yaml).

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: ivolvenp
  namespace: ivolve
spec:
  podSelector:
    matchLabels:
      app: ivolvedb
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: ivolveapp
      ports:
        - protocol: TCP
          port: 3306
```

## Apply the Policy

Validate the manifest with a server-side dry run, then apply it:

```bash
kubectl apply --dry-run=server -f network-policy.yaml
kubectl apply -f network-policy.yaml
```

## Verify the Policy

Check that the policy exists and inspect its rules:

```bash
kubectl get networkpolicy -n ivolve
kubectl describe networkpolicy ivolvenp -n ivolve
```

Check the Pod labels and application logs:

```bash
kubectl get pods -n ivolve --show-labels
kubectl logs -n ivolve -l app=ivolveapp -c ivolve-app --tail=20
```

The policy selects MySQL Pods labeled `app=ivolvedb`. It permits ingress from application Pods labeled `app=ivolveapp` on TCP port `3306`.

## Notes

- `policyTypes: Ingress` applies this policy to incoming traffic to the selected MySQL Pod. It does not configure egress restrictions.
- NetworkPolicy enforcement depends on the cluster network plugin. This cluster uses Calico.
- During verification, one application Pod was `Running` and another was `Pending` due to the resource capacity constraints from Lab 16. The policy was configured for Pods matching the application label.
 <img width="1572" height="356" alt="WhatsApp Image 2026-11-02 at 11 46 01 AM" src="https://github.com/user-attachments/assets/f42fe682-7e44-4855-a5da-97f97b637d5f" />
<img width="1250" height="242" alt="WhatsApp Image 2026-11-02 at 11 46 33 AM" src="https://github.com/user-attachments/assets/015d6a67-b107-4d30-936f-bee7b278799c" />
