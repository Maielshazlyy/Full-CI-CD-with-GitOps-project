# Kubernetes Lab 15: Node.js Application Deployment

## Objective

Deploy two replicas of the Node.js application in the `ivolve` namespace. The application uses the custom Docker Hub image from Lab 8, connects to the MySQL database from Lab 14, and stores its logs using the PersistentVolumeClaim from Lab 13.

## Prerequisites

The following resources from previous labs are used:

- Namespace: `ivolve`
- ConfigMap: `ivolvcm`
- Secret: `ivolvesecret`
- MySQL StatefulSet: `ivolvedb`
- MySQL headless Service: `ivolvedbsvc`
- Application PVC: `ivolvepvc`
- Docker Hub image: `maielshazly/ivolve-app:lab8`

The ConfigMap contains `DB_HOST=ivolvedbsvc` and `DB_USER=ivolve_user`. The Secret contains the MySQL passwords. Secret values are not included in this repository.

The worker node must have the label `project=ivolve` and the taint `workload=ivolveApp:NoSchedule`.

## Files

- `deployment.yaml` — Deployment, init container, scheduling rules, environment variables, and persistent log mount.
- `service.yaml` — Internal ClusterIP Service for the application.
- `README.md` — Lab steps and verification commands.

## Deployment Configuration

The Deployment is named `ivolveapp` and runs two replicas using `maielshazly/ivolve-app:lab8`.

The Pods:

- Read database connection settings from the ConfigMap and Secret.
- Use `nodeSelector` to run on nodes labeled `project=ivolve`.
- Use a toleration to run on the worker tainted `workload=ivolveApp:NoSchedule`.
- Mount the Lab 13 PVC, `ivolvepvc`, at `/app/logs`.

An init container uses the `mysql:8.0` image to wait for MySQL, create the `ivolve` database if needed, create or update `ivolve_user`, and grant the user privileges on the database. The MySQL StatefulSet does not set `MYSQL_DATABASE`; database setup is handled by this init container.

## Apply the Manifests

Run these commands from the directory containing `deployment.yaml` and `service.yaml`:

```bash
kubectl apply --dry-run=server -f deployment.yaml -f service.yaml
kubectl apply -f deployment.yaml -f service.yaml
kubectl rollout status deployment/ivolveapp -n ivolve --timeout=180s
```

## Verify the Deployment

Check the Deployment, Pods, Service, and PVC:

```bash
kubectl get deployment,pods,service,pvc -n ivolve
```

Check where the application Pods are running:

```bash
kubectl get pods -n ivolve -l app=ivolveapp -o wide
```

Check that the Service has endpoints for both application Pods:

```bash
kubectl get endpointslice -n ivolve \
  -l kubernetes.io/service-name=ivolveappsvc
```

Review the init container and application logs:

```bash
kubectl logs -n ivolve -l app=ivolveapp \
  --all-containers=true --prefix=true
```

The application logs should confirm that it connected to MySQL, found the `ivolve` database, and started its server on port `3000`.

## Verify the Database

Connect to MySQL from a terminal with access to the cluster:

```bash
kubectl exec -it -n ivolve ivolvedb-0 -- \
  mysql -h ivolvedbsvc -uivolve_user -p ivolve
```

Enter the `DB_PASSWORD` value when prompted. Then run:

```sql
SHOW DATABASES;
SHOW GRANTS FOR 'ivolve_user'@'%';
exit
```

The database list should include `ivolve`, and the application user should have privileges on `ivolve.*`.

## Results

- The `ivolveapp` Deployment reached `2/2` available replicas.
- Both application Pods ran on the worker selected by `project=ivolve`.
- The `ivolveappsvc` ClusterIP Service had endpoints for both Pods.
- The init container completed the database setup.
- Application logs confirmed a connection to the `ivolve` database.
- The `ivolvepvc` claim was mounted at `/app/logs`.

`ClusterIP` makes the Service available inside the Kubernetes cluster; it does not expose the application directly to the public internet.
<img width="1902" height="330" alt="image" src="https://github.com/user-attachments/assets/0b262eff-1d43-45ad-b1e7-a18cd2282ce0" />
<img width="1910" height="290" alt="image" src="https://github.com/user-attachments/assets/0a8e4798-1d10-455c-858e-d438e961df40" />
<img width="1112" height="482" alt="image" src="https://github.com/user-attachments/assets/4575260f-8b7b-4196-aefa-5df9308a5c5a" />


