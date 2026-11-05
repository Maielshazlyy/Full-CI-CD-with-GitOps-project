# Kubernetes — Lab 18: Node-Wide Pod Management with DaemonSet

## Objective

Deploy a monitoring stack in the `monitoring` namespace. Run Prometheus, Alertmanager, and Grafana, and use a Node Exporter DaemonSet to expose metrics from every Kubernetes node.

## Components

- **Node Exporter** collects host-level metrics and runs on every node through a DaemonSet.
- **Prometheus** scrapes metrics from Node Exporter.
- **Alertmanager** receives alerts from Prometheus.
- **Grafana** connects to Prometheus and displays metrics in a dashboard.

## Manifest Files

- `namespace.yaml` — creates the `monitoring` namespace.
- `node-exporter-daemonset.yaml` — runs Node Exporter on each node and exposes metrics on port `9100`.
- `prometheus-config.yaml` — configures Prometheus to scrape Node Exporter and connect to Alertmanager.
- `prometheus.yaml` — defines the Prometheus Deployment.
- `prometheus-service.yaml` — exposes Prometheus inside the cluster.
- `alertmanager-config.yaml` — defines the Alertmanager configuration.
- `alertmanager.yaml` — defines the Alertmanager Deployment.
- `alertmanager-service.yaml` — exposes Alertmanager inside the cluster.
- `grafana.yaml` — defines the Grafana Deployment.
- `grafana-service.yaml` — exposes Grafana inside the cluster.

## Deploy the Manifests

Run these commands from the directory containing the YAML files:

```bash
kubectl apply -f namespace.yaml
kubectl apply -f node-exporter-daemonset.yaml

kubectl apply -f prometheus-config.yaml
kubectl apply -f prometheus.yaml
kubectl apply -f prometheus-service.yaml

kubectl apply -f alertmanager-config.yaml
kubectl apply -f alertmanager.yaml
kubectl apply -f alertmanager-service.yaml

kubectl apply -f grafana.yaml
kubectl apply -f grafana-service.yaml
```

## Verify the Deployments

Check that the monitoring Pods and Services are running:

```bash
kubectl get pods,services -n monitoring -o wide
```

Check that the Node Exporter DaemonSet has a Pod on each node:

```bash
kubectl get daemonset node-exporter -n monitoring
kubectl get pods -n monitoring -l app=node-exporter -o wide
```

The number of ready Node Exporter Pods should match the number of cluster nodes.

## Verify Node Exporter Metrics

Check that each node exposes metrics on port `9100`. Replace the IPs below if your node IPs change:

```bash
curl -s http://172.31.12.222:9100/metrics | grep '^node_' | head
curl -s http://172.31.9.53:9100/metrics | grep '^node_' | head
```

The metrics response should include entries beginning with `node_`.

## Verify Prometheus Targets

Forward Prometheus's service port to the local machine:

```bash
kubectl port-forward -n monitoring service/prometheus 9090:9090
```

Open `http://localhost:9090/targets` in a browser and confirm the Node Exporter targets are **UP**.

## Open Grafana and Import the Dashboard

Forward Grafana's service port:

```bash
kubectl port-forward -n monitoring service/grafana 3000:3000
```

Open `http://localhost:3000` in a browser. Configure the Prometheus data source with this in-cluster URL:

```text
http://prometheus.monitoring.svc.cluster.local:9090
```

Import the **Node Exporter Full** dashboard using Grafana dashboard ID `1860`. Select each node in the dashboard to view its metrics.

## Verification Commands

```bash
kubectl get pods,services -n monitoring -o wide
kubectl get daemonset node-exporter -n monitoring
kubectl get pods -n monitoring -l app=node-exporter -o wide
kubectl get configmap prometheus-config -n monitoring -o yaml
```

## Evidence

Include screenshots showing:

1. The monitoring Pods and Services.
2. Node Exporter Pods running on every node.
3. Prometheus targets showing both Node Exporter targets as **UP**.
4. The Grafana Node Exporter Full dashboard displaying metrics for each node.

 <img width="1600" height="286" alt="image" src="https://github.com/user-attachments/assets/6518c01f-d208-41f2-a266-ce1fda2934aa" />
 <img width="1600" height="757" alt="image" src="https://github.com/user-attachments/assets/3c811d1e-2ada-417b-b53c-0f8ac7edc667" />
 <img width="1600" height="764" alt="image" src="https://github.com/user-attachments/assets/b78ee259-2e92-47dd-8a6a-6024d366248a" />
 <img width="1600" height="737" alt="WhatsApp Image 2026-11-02 at 6 20 52 PM" src="https://github.com/user-attachments/assets/ae0dcb75-1cc3-47fd-89c4-51d3742ca4b6" />


 



 
