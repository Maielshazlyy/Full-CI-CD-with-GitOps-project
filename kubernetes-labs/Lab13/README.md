# Kubernetes Lab 13: Persistent Storage for Application Logs

## Objective

Create a PersistentVolume (PV) and PersistentVolumeClaim (PVC) for application logs in the `ivolve` namespace. The PV uses the node directory `/mnt/app-logs`, and the claim can be mounted in the application container at `/app/logs`.

## Resources

- **PV:** `ivolvpv`, with `1Gi` capacity, `ReadWriteMany` access mode, and `Retain` reclaim policy.
- **PVC:** `ivolvepvc` in the `ivolve` namespace, requesting `1Gi`.
- **StorageClass:** Not used.
- **Node path:** `/mnt/app-logs`.
- **Application mount path:** `/app/logs`.


## Prepare the Node Directory

The PV uses `hostPath`, which refers to a directory on a specific Kubernetes node. Create that directory on the worker node:

```bash
sudo mkdir -p /mnt/app-logs
sudo chmod 777 /mnt/app-logs
```

The PV manifest uses node affinity to associate the volume with the worker node that contains this directory.

## Create and Apply the Manifests

Validate and apply the PV and PVC:

```bash
kubectl apply --dry-run=server -f pv.yaml
kubectl apply --dry-run=server -f pvc.yaml

kubectl apply -f pv.yaml
kubectl apply -f pvc.yaml
```

## Verify the Binding

```bash
kubectl get pv ivolvpv
kubectl get pvc ivolvepvc -n ivolve
```

Both resources should show `Bound`. The claim should reference `ivolvpv`.

## Verify the Application Log Mount

A test Pod can verify that the claim is writable at the application log path. The test Pod manifest mounts `ivolvepvc` at `/app/logs`:

```bash
kubectl apply -f storage-test-pod.yaml
kubectl get pod storage-test -n ivolve -o wide
kubectl exec -n ivolve storage-test -- cat /app/logs/storage-test.txt
```

The test file content confirms that the Pod can write to and read from the mounted volume.

## Files

- `pv.yaml` — PersistentVolume manifest.
- `pvc.yaml` — PersistentVolumeClaim manifest.
- `storage-test-pod.yaml` — optional test Pod manifest used to verify the mount.

## Storage Note

`hostPath` stores data on one node’s local filesystem; it is not shared storage across multiple nodes. Although the PV and PVC declare `ReadWriteMany` as required by the lab, this local storage setup does not provide true multi-node shared access. The Docker Compose `app_logs` volume is separate; Kubernetes uses the PVC to mount storage into a Pod at `/app/logs`.
<img width="1457" height="195" alt="image" src="https://github.com/user-attachments/assets/ac821237-4f18-4fad-91e6-20bb4bc403f6" />
<img width="1360" height="86" alt="image" src="https://github.com/user-attachments/assets/1c088dfa-615d-479f-8811-9fd055fba81e" />

