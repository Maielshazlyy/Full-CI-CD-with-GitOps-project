# Kubernetes — Lab 9: Deploy a Cluster Using kubeadm

## Objective

Set up a Kubernetes cluster on two AWS EC2 instances: one control-plane node and one worker node. The cluster uses `containerd` as its container runtime and Calico for Pod networking.

## Environment

- Two Ubuntu Server 24.04 EC2 instances
- Instance type: `t3.small`
- Kubernetes: `v1.37.1`
- Container runtime: `containerd`
- Pod network: Calico `v3.33.0`

## AWS Network Setup

Both instances were launched in the same VPC and associated with the same security group.

- SSH access was restricted to my IP address.
- Internal traffic was allowed between instances in the security group.
- Source/destination checks were disabled on both instances for Pod network traffic.
- The instances communicated using their private IP addresses.

## 1. Install containerd

Run these commands on both instances:

```bash
sudo apt-get update
sudo apt-get install -y containerd ca-certificates curl gpg
```

Create a default containerd configuration and enable the `systemd` cgroup driver:

```bash
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml >/dev/null
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
```

Load the required kernel modules and network settings:

```bash
cat <<'EOF' | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

cat <<'EOF' | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF

sudo sysctl --system
sudo systemctl restart containerd
sudo systemctl enable containerd
```

## 2. Install Kubernetes tools

Run these commands on both instances to install `kubeadm`, `kubelet`, and `kubectl` from the Kubernetes v1.37 package repository:

```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gpg
sudo mkdir -p -m 755 /etc/apt/keyrings

curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.37/deb/Release.key |
  sudo gpg --dearmor --yes -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg

echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.37/deb/ /' |
  sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```

## 3. Initialize the control plane

Run this on the control-plane instance, using its private IP address:

```bash
sudo kubeadm init \
  --apiserver-advertise-address=<CONTROL_PLANE_PRIVATE_IP> \
  --pod-network-cidr=192.168.0.0/16 \
  --cri-socket=unix:///run/containerd/containerd.sock
```

Configure `kubectl` for the Ubuntu user:

```bash
mkdir -p "$HOME/.kube"
sudo cp -i /etc/kubernetes/admin.conf "$HOME/.kube/config"
sudo chown "$(id -u):$(id -g)" "$HOME/.kube/config"
```

## 4. Install Calico networking

Run these commands on the control plane. Calico provides networking between Pods. Its installation uses the same Pod CIDR configured during `kubeadm init`.

```bash
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.33.0/manifests/v3_projectcalico_org.yaml
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.33.0/manifests/tigera-operator.yaml
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.33.0/manifests/custom-resources.yaml
```

## 5. Join the worker node

Generate a fresh join command on the control plane:

```bash
sudo kubeadm token create --print-join-command
```

Run the generated command with `sudo` on the worker node. Do not add the token or full join command to this repository because it is a credential.

## 6. Verify the cluster

Run these commands on the control plane:

```bash
kubectl get nodes -o wide
kubectl get pods -A
kubectl cluster-info
```

Both nodes reached the `Ready` state. The Kubernetes system and Calico Pods showed `Running`.

## Results

<img width="1467" height="207" alt="WhatsApp Image 2026-10-24 at 5 17 48 PM" src="https://github.com/user-attachments/assets/252c0398-59b7-4938-874e-df73921a6358" />
<img width="1350" height="85" alt="image" src="https://github.com/user-attachments/assets/05e6142c-c1d7-456e-b322-9cb94c53c5ea" />
<img width="1366" height="507" alt="image" src="https://github.com/user-attachments/assets/d6d16d71-cb75-49e0-abd5-c6a81ba0a7ac" />


