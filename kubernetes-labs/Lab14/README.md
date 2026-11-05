# Kubernetes Lab 14: MySQL StatefulSet with a Headless Service

## Objective

Deploy a single-replica MySQL StatefulSet in the `ivolve` namespace. The Pod uses the Lab 12 ConfigMap and Secret, runs on the labeled worker node, stores database files persistently, and is reachable through a headless Service and Kubernetes DNS.

## Prerequisites

- The `ivolve` namespace created in Lab 11.
- ConfigMap `ivolvcm` and Secret `ivolvesecret` from Lab 12.
- Worker node labeled `project=ivolve`.
- Worker node tainted `workload=ivolveApp:NoSchedule`.

The real password values are stored in the Kubernetes Secret and are not included in this repository.

## Storage

This cluster has no StorageClass, and the PV from Lab 13 is already bound to the application logs claim. Lab 14 therefore uses a separate static PV named `ivolvemysqlpv` for MySQL data.

The PV uses the worker's local directory `/mnt/mysql-data`. The StatefulSet's `volumeClaimTemplates` creates a claim for its Pod and mounts the storage at `/var/lib/mysql`.

Because this is a single-node `hostPath` volume, it is local to the worker and is not shared storage across nodes.

## Headless Service

The headless Service is named `ivolvedbsvc` and listens on port `3306`. It provides DNS records for the MySQL Pod.

Kubernetes resource names must be lowercase, so the manifests use `ivolvedb` and `ivolvedbsvc`.

## StatefulSet

The StatefulSet is named `ivolvedb` and has one replica. Its Pod:

- Uses the `mysql:8.0` image.
- Gets `MYSQL_USER` from `DB_USER` in ConfigMap `ivolvcm`.
- Gets `MYSQL_ROOT_PASSWORD` and `MYSQL_PASSWORD` from Secret `ivolvesecret`.
- Creates the `ivolve` database.
- Selects nodes labeled `project=ivolve`.
- Tolerates the worker's `workload=ivolveApp:NoSchedule` taint.
- Mounts its persistent volume at `/var/lib/mysql`.

## Apply the Manifests

```bash
kubectl apply --dry-run=server -f mysql-pv.yaml
kubectl apply --dry-run=server -f mysql-headless-service.yaml
kubectl apply --dry-run=server -f mysql-statefulset.yaml

kubectl apply -f mysql-pv.yaml
kubectl apply -f mysql-headless-service.yaml
kubectl apply -f mysql-statefulset.yaml
```

## Verify the Deployment and Storage

```bash
kubectl get statefulset,service -n ivolve
kubectl get pods,pvc -n ivolve
kubectl get pv ivolvemysqlpv
```

The expected Pod name is `ivolvedb-0`. Its status should become `Running`, and its PVC should become `Bound`.

## Verify DNS

The StatefulSet Pod's fully qualified DNS name is:

```text
ivolvedb-0.ivolvedbsvc.ivolve.svc.cluster.local
```

Check that it resolves from inside the cluster:

```bash
kubectl exec -n ivolve ivolvedb-0 -- \
  getent hosts ivolvedb-0.ivolvedbsvc.ivolve.svc.cluster.local
```

## Connect to MySQL

Connect through the headless Service using the MySQL client in the Pod:

```bash
kubectl exec -it -n ivolve ivolvedb-0 -- \
  mysql -h ivolvedbsvc.ivolve.svc.cluster.local -uivolve_user -p ivolve
```

Enter the `DB_PASSWORD` value when prompted. Then run:

```sql
SELECT 1;
SHOW DATABASES;
```

The `ivolve` database should appear in the results. Exit the MySQL client with:

```sql
exit
```

## Application Connection Note

The headless Service hostname for this lab is `ivolvedbsvc`. If an application later uses `DB_HOST` from the Lab 12 ConfigMap to connect to this database, set that value to `ivolvedbsvc` (or its full DNS name). The `DB_HOST` variable is a client connection setting; the MySQL server itself does not use it.

## Files

- `mysql-pv.yaml` — static PersistentVolume for MySQL data.
- `mysql-headless-service.yaml` — headless Service for MySQL DNS and port 3306.
- `mysql-statefulset.yaml` — one-replica MySQL StatefulSet with persistent storage and Lab 12 configuration references.
- <img width="1470" height="382" alt="image" src="https://github.com/user-attachments/assets/adda9fe1-b18b-4bf8-93cb-bc71c8a898a6" />
<img width="672" height="261" alt="image" src="https://github.com/user-attachments/assets/021ea127-fa4d-4fbe-93d0-a8fd2e85c6cf" />

