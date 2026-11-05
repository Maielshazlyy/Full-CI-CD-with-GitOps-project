# 02 — Kubernetes cluster setup with kubeadm

## Cluster shape

The lab used two Ubuntu EC2 machines in the same VPC: one control-plane node and one worker. Containerd is the container runtime. `kubeadm` bootstraps the cluster, kubelet runs and supervises Pods on each node, and Calico provides Pod networking.

Use current instance private IPs and DNS names; EC2 public IPs can change when instances stop and start. This file documents the flow used in the lab, not the AWS console's current pricing or a production HA design.

## 1. AWS network and instance preparation

- Launch two compatible Ubuntu instances in one VPC with routes between them.
- Keep SSH (TCP 22) restricted to your current trusted public IP (`/32`).
- Permit the Kubernetes API (TCP 6443) only from trusted admin machines and cluster components that need it.
- Permit kubelet/control-plane traffic and the Calico transport required by the selected Calico mode between the two nodes. Avoid exposing these ports to the entire Internet.
- Attach enough disk and memory for the selected components. The lab's `t3.small` nodes have limited resources; Argo CD plus monitoring can overwhelm them.
- Connect to each machine with the key pair, e.g. `ssh -i <key.pem> ubuntu@<current-control-public-ip>`.

## 2. Prepare both Ubuntu nodes

Run on both nodes. Use the same Kubernetes minor version on both; this lab used v1.37.x.

```bash
sudo swapoff -a
sudo sed -i '/\sswap\s/s/^/#/' /etc/fstab

sudo modprobe overlay
sudo modprobe br_netfilter
sudo tee /etc/modules-load.d/k8s.conf >/dev/null <<'EOF'
overlay
br_netfilter
EOF

sudo tee /etc/sysctl.d/k8s.conf >/dev/null <<'EOF'
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF
sudo sysctl --system
```



Install and configure containerd on both nodes:

```bash
sudo apt-get update
sudo apt-get install -y containerd
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml >/dev/null
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
sudo systemctl enable --now containerd
sudo systemctl restart containerd
sudo systemctl is-active containerd
```

Confirm the `SystemdCgroup` setting was changed under the runc options. If your installed containerd uses a different configuration layout, follow that containerd version's documentation instead of blindly applying the `sed` command.

## 3. Install Kubernetes tools on both nodes

The package repository path is specific to the Kubernetes minor version. This example follows the lab's v1.37 line; use a matching supported package repository/version if rebuilding the lab later.

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gpg
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.37/deb/Release.key \
  | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
sudo chmod 644 /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.37/deb/ /' \
  | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```

Check that all nodes have matching versions:

```bash
kubeadm version
kubelet --version
kubectl version --client
```

## 4. Initialize the control plane

On the control-plane only, determine its private address with `ip -4 addr` or `hostname -I`. Initialize with that private address and a Pod CIDR compatible with Calico. The lab's Calico pool used the `192.168.0.0/16` range; the CNI and kubeadm CIDR must agree.

```bash
CONTROL_PRIVATE_IP=<control-plane-private-ip>
sudo kubeadm init \
  --apiserver-advertise-address="$CONTROL_PRIVATE_IP" \
  --pod-network-cidr=192.168.0.0/16
```

Save the generated `kubeadm join ...` command privately for the worker setup. It contains a bootstrap token; do not publish it.

Set up kubectl for the normal Ubuntu user:

```bash
mkdir -p "$HOME/.kube"
sudo cp /etc/kubernetes/admin.conf "$HOME/.kube/config"
sudo chown "$(id -u):$(id -g)" "$HOME/.kube/config"
kubectl get nodes
```

The control plane may initially show `NotReady`; install the CNI before expecting CoreDNS and worker Pods to run.

## 5. Install Calico and join the worker

Install Calico using its official operator/manifest instructions and a version compatible with the cluster. Ensure its IP pool matches the Pod CIDR selected in `kubeadm init`. Wait for Calico components and CoreDNS to become Ready.

Then, on the worker, run the exact join command printed by `kubeadm init`, prefixed with `sudo`. Do not reuse an expired token; on the control-plane a fresh command can be produced with:

```bash
kubeadm token create --print-join-command
```

Verify from the control-plane:

```bash
kubectl get nodes -o wide
kubectl get pods -A
```

Both nodes should become `Ready`; CoreDNS and CNI Pods should be `Running`.

## 6. Labels and taints used by the application labs

The worker is labeled so application Pods can select it, and tainted to repel Pods without a matching toleration:

```bash
kubectl label node <worker-node-name> project=ivolve
kubectl taint node <worker-node-name> workload=ivolveApp:NoSchedule
kubectl get nodes --show-labels
kubectl describe node <worker-node-name> | grep -i taints
```

The app and MySQL workload manifests include a matching `nodeSelector` and toleration. This does not create capacity; check free CPU/memory and Pod resource requests before increasing replicas.

## 7. Verify and troubleshoot

```bash
kubectl get nodes -o wide
kubectl get pods -A
free -h
uptime
sudo systemctl is-active kubelet containerd
sudo ss -lntp | grep ':6443'
```

If API requests time out on a small node, inspect memory, disk I/O pressure, CPU load, kubelet/containerd, and `/readyz` before reinstalling the cluster. Avoid starting Argo CD and a full monitoring stack together on undersized nodes. See official kubeadm installation/create-cluster links in the main README.
