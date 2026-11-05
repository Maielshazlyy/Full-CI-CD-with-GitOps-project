# Kubernetes Lab 12: ConfigMap and Secret

## Objective

Create a ConfigMap for non-sensitive application settings and a Secret for database credentials in the existing `ivolve` namespace.

## Kubernetes Objects

- **ConfigMap — `ivolvcm`:** stores `DB_HOST` and `DB_USER`.
- **Secret — `ivolvesecret`:** stores `MYSQL_ROOT_PASSWORD` and `DB_PASSWORD`.
- **Namespace — `ivolve`:** the namespace created in Lab 11.

## Configuration Values

The ConfigMap uses the Lab 8 application settings:

- `DB_HOST`: `db`
- `DB_USER`: `ivolve_user`

The Secret contains the database passwords used for the lab. The real password values are not included in this repository.

## Create the ConfigMap

Generate the YAML file, validate it, and apply it:

```bash
kubectl create configmap ivolvcm \
  --namespace=ivolve \
  --from-literal=DB_HOST=db \
  --from-literal=DB_USER=ivolve_user \
  --dry-run=client -o yaml > configmap.yaml

kubectl apply --dry-run=server -f configmap.yaml
kubectl apply -f configmap.yaml
```

## Create the Secret

Enter the Lab 8 password values when prompted. Input is hidden as you type:

```bash
read -rsp 'MYSQL_ROOT_PASSWORD from Lab 8: ' MYSQL_ROOT_PASSWORD
echo
read -rsp 'DB_PASSWORD from Lab 8: ' DB_PASSWORD
echo

kubectl create secret generic ivolvesecret \
  --namespace=ivolve \
  --from-literal=MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
  --from-literal=DB_PASSWORD="$DB_PASSWORD" \
  --dry-run=client -o yaml > secret.yaml

unset MYSQL_ROOT_PASSWORD DB_PASSWORD
```

Validate and apply the Secret:

```bash
kubectl apply --dry-run=server -f secret.yaml
kubectl apply -f secret.yaml
```

## Verify

Check that both objects exist:

```bash
kubectl get configmap ivolvcm -n ivolve
kubectl get secret ivolvesecret -n ivolve
```

Inspect their metadata and keys:

```bash
kubectl describe configmap ivolvcm -n ivolve
kubectl describe secret ivolvesecret -n ivolve
```

## Repository Files

- `configmap.yaml` — ConfigMap manifest with the non-sensitive settings.
- `secret.example.yaml` — example Secret manifest with placeholder values.
- `secret.yaml` — generated locally with the real credentials; do not commit or push it.

Kubernetes Secret data is Base64-encoded, not encrypted by Base64 itself. The example file uses placeholders so real credentials are not published.
<img width="1241" height="487" alt="image" src="https://github.com/user-attachments/assets/b1e495fd-f035-42b8-9272-00da13765b99" />
<img width="1080" height="322" alt="image" src="https://github.com/user-attachments/assets/acb9dadf-4e8c-4899-a71d-c117f887edb9" />


