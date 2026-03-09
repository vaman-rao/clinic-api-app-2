# Cloud-Native Monitoring and Alerting Bootcamp
**Prometheus + Grafana for Kubernetes & Microservices**

---

## 1. Details

| | |
|---|---|
| **Client** | Oracle |
| **Vendor** | The Skill Enhancers |
| **Trainer** | Vaman Rao Deshmukh |
| **Mode** | VILT |
| **Dates** | 09th March 2026 to 13th March 2026 |
| **Duration** | Five days, 9 AM – 5 PM |
| **Audience** | Freshers |
| **Approach** | Hands-on, industry-standard, enterprise-focused |

---

## 2. Prerequisites

Participants need working knowledge of:

- Linux command line (processes, networking, file system basics)
- Docker (images, containers, volumes, networking)
- Kubernetes (pods, deployments, services, namespaces)
- YAML configuration files
- Basic REST API understanding
- Java / Spring Boot fundamentals

---

## 3. Lab Setup Requirements

### Minimum Hardware

| Requirement | Minimum |
|---|---|
| Architecture | 64-bit (Intel i5 or equivalent) |
| RAM | 16 GB |
| Free Disk | 100 GB |
| OS | Windows 11 / macOS |
| Internet | ≥ 50 Mbps |

### Required Software

| Tool | Notes |
|---|---|
| Docker | Latest stable |
| Minikube or Kind | Either one |
| `kubectl` CLI | Matches your cluster version |
| Helm v3+ | |
| Git | |
| Java 17+ | |
| VS Code | With YAML & Kubernetes extensions |

> ⚠️ **Note:** Prometheus, Grafana, and Alertmanager will be **installed during labs** using Docker and Helm. Do **not** pre-install them manually.

### Permissions

- Access to GitHub repositories

---

## 4. Learning Objectives

By the end of this bootcamp, participants will be able to:

- Understand observability in cloud-native systems
- Install and configure Prometheus
- Write advanced PromQL queries
- Instrument Java microservices with Micrometer
- Monitor Kubernetes clusters
- Create production-grade dashboards in Grafana
- Configure alerting with Alertmanager
- Design monitoring architecture for production systems

---

## 5. Training Delivery Schedule

### Day 1 — Observability & Prometheus Fundamentals

**Topics:**
- Monitoring vs Observability
- Metrics, Logs, Traces (Observability Triad)
- Prometheus Architecture & Data Model
- Time-Series & Labels
- Metric Types: Counter, Gauge, Histogram, Summary
- Installing Prometheus (Docker-based)
- `prometheus.yml` deep dive
- Node Exporter setup
- Introduction to PromQL (basic queries)

**Lab:**
- Run Prometheus via Docker
- Scrape Node Exporter metrics
- Write PromQL for CPU, memory, disk

---

### Day 2 — Advanced PromQL & Microservice Instrumentation

**Topics:**
- PromQL advanced functions
- Aggregations & vector matching
- Recording Rules
- Service Discovery concepts
- Exporters ecosystem overview
- Monitoring Java using Spring Boot Actuator
- Micrometer integration
- Exposing custom metrics

**Lab:**
- Instrument Spring Boot app
- Configure Prometheus scraping
- Create recording rules

---

### Day 3 — Alerting & Alertmanager

**Topics:**
- Alerting philosophy & best practices
- SLI, SLO, SLA basics
- Writing alert rules
- Alert states (Pending, Firing, Resolved)
- Alertmanager architecture
- Routing, grouping, inhibition, silencing
- Email / Slack webhook integration

**Lab:**
- Create alert rules (CPU spike, service down)
- Configure Alertmanager
- Trigger and validate alerts

---

### Day 4 — Grafana & Kubernetes Monitoring

**Topics:**
- Grafana architecture
- Connecting Prometheus as data source
- Panels, dashboards, variables, templating
- Dashboard best practices
- Grafana Unified Alerting
- Kubernetes monitoring architecture (kube-state-metrics, cAdvisor)
- Deploying kube-prometheus-stack via Helm

**Lab:**
- Build infrastructure dashboard
- Build application dashboard
- Deploy monitoring stack in Kubernetes
- Explore cluster dashboards

---

### Day 5 — Production Architecture & Capstone

**Topics:**
- End-to-end monitoring flow
- Prometheus federation & remote write
- Introduction to Thanos / long-term storage
- Securing Prometheus & Grafana (RBAC basics)
- Monitoring best practices in production

**Capstone Project:**
- Deploy multi-service Spring Boot app in Kubernetes
- Configure full monitoring stack
- Create comprehensive Grafana dashboard
- Define and trigger alert rules
- Route alerts via Alertmanager
- Simulate failure scenario

---

## 6. Lab Architecture (Local Laptop Model)

```
Spring Boot Microservice → Prometheus → Alertmanager → Grafana
```

Deployed on:
- Local Kubernetes cluster (Minikube or Kind)
- Installed via Helm charts

---

## 7. Pre-Bootcamp Validation Checklist

Run each command before Day 1 and confirm no errors. Contact the trainer immediately if anything fails — lab time cannot be used for environment setup.

```bash
# Docker
docker run hello-world

# Kubernetes
minikube start          # OR: kind create cluster
kubectl get nodes

# Helm
helm repo add stable https://charts.helm.sh/stable
helm version

# Java
java -version           # should show 17.x

# Test pod
kubectl run nginx-test --image=nginx
kubectl get pod nginx-test
```
