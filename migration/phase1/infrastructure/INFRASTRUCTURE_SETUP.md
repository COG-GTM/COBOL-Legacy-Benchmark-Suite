# Infrastructure Setup Guide

## Overview

This guide describes the infrastructure setup for the modernized Investment Portfolio Management System. The infrastructure is designed to run on Kubernetes and includes monitoring, logging, and CI/CD components.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Kubernetes Cluster                               │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                    portfolio-system namespace                        ││
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐           ││
│  │  │   Portfolio   │  │  PostgreSQL   │  │     Redis     │           ││
│  │  │   Service     │  │  StatefulSet  │  │  StatefulSet  │           ││
│  │  │  (3-10 pods)  │  │   (1 pod)     │  │   (1 pod)     │           ││
│  │  └───────────────┘  └───────────────┘  └───────────────┘           ││
│  │         │                   │                  │                    ││
│  │         └───────────────────┴──────────────────┘                    ││
│  │                             │                                        ││
│  │  ┌───────────────────────────────────────────────────────────────┐  ││
│  │  │                    Monitoring Stack                            │  ││
│  │  │  ┌─────────────┐  ┌─────────────┐                             │  ││
│  │  │  │ Prometheus  │  │   Grafana   │                             │  ││
│  │  │  └─────────────┘  └─────────────┘                             │  ││
│  │  └───────────────────────────────────────────────────────────────┘  ││
│  │                                                                      ││
│  │  ┌───────────────────────────────────────────────────────────────┐  ││
│  │  │                     Logging Stack                              │  ││
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │  ││
│  │  │  │Elasticsearch│  │  Logstash   │  │   Kibana    │           │  ││
│  │  │  └─────────────┘  └─────────────┘  └─────────────┘           │  ││
│  │  │  ┌─────────────────────────────────────────────────────────┐ │  ││
│  │  │  │              Filebeat (DaemonSet)                       │ │  ││
│  │  │  └─────────────────────────────────────────────────────────┘ │  ││
│  │  └───────────────────────────────────────────────────────────────┘  ││
│  └─────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                         Ingress Controller                           ││
│  │  portfolio.example.com  │  grafana.portfolio.example.com            ││
│  │  kibana.portfolio.example.com                                        ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
```

## Prerequisites

- Kubernetes cluster 1.28+
- kubectl configured with cluster access
- Helm 3.x (optional, for some components)
- Storage class supporting dynamic provisioning
- Ingress controller (NGINX recommended)

## Namespace Setup

The `portfolio-system` namespace includes resource quotas and limit ranges:

```yaml
Resource Quotas:
  - CPU: 20 cores (requests), 40 cores (limits)
  - Memory: 40Gi (requests), 80Gi (limits)
  - Pods: 50 max
  - Services: 20 max
  - Secrets: 50 max
  - ConfigMaps: 50 max
  - PVCs: 20 max

Limit Ranges:
  - Container CPU: 100m min, 4 cores max, 500m default
  - Container Memory: 128Mi min, 8Gi max, 512Mi default
```

### Deployment

```bash
kubectl apply -f kubernetes/namespace.yaml
```

## PostgreSQL

### Configuration

- **Version**: PostgreSQL 15
- **Storage**: 50Gi persistent volume
- **Resources**: 500m-2 CPU, 1-4Gi memory
- **Connection Pool**: 100 max connections

### Key Settings

```
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 128MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 4MB
min_wal_size = 1GB
max_wal_size = 4GB
```

### Deployment

```bash
kubectl apply -f kubernetes/configmap.yaml
kubectl apply -f kubernetes/secrets.yaml
kubectl apply -f kubernetes/postgresql-deployment.yaml
```

### Verification

```bash
kubectl get pods -n portfolio-system -l app=postgresql
kubectl exec -it postgresql-0 -n portfolio-system -- psql -U portfolio_app -d portfolio_db -c "SELECT version();"
```

## Redis

### Configuration

- **Version**: Redis 7
- **Storage**: 10Gi persistent volume
- **Resources**: 250m-1 CPU, 512Mi-1Gi memory
- **Max Memory**: 512MB with LRU eviction

### Key Settings

```
maxmemory 512mb
maxmemory-policy allkeys-lru
appendonly yes
appendfsync everysec
```

### Deployment

```bash
kubectl apply -f kubernetes/redis-deployment.yaml
```

### Verification

```bash
kubectl get pods -n portfolio-system -l app=redis
kubectl exec -it redis-0 -n portfolio-system -- redis-cli ping
```

## Portfolio Service

### Configuration

- **Replicas**: 3 (min) to 10 (max) via HPA
- **Resources**: 500m-2 CPU, 1-2Gi memory
- **Health Checks**: Liveness and readiness probes via actuator

### Horizontal Pod Autoscaler

```yaml
Metrics:
  - CPU utilization: 70% target
  - Memory utilization: 80% target
Scale:
  - Min replicas: 3
  - Max replicas: 10
  - Scale down stabilization: 300 seconds
```

### Deployment

```bash
kubectl apply -f kubernetes/portfolio-service-deployment.yaml
```

### Verification

```bash
kubectl get pods -n portfolio-system -l app=portfolio-service
kubectl get hpa -n portfolio-system
```

## Monitoring Stack

### Prometheus

- **Retention**: 30 days
- **Storage**: 50Gi persistent volume
- **Scrape Interval**: 15 seconds

#### Scrape Targets

1. **portfolio-service**: Application metrics (port 8081)
2. **postgresql**: Database metrics (port 9187)
3. **redis**: Cache metrics (port 9121)
4. **kubernetes-nodes**: Node metrics
5. **kubernetes-pods**: Pod metrics

#### Alert Rules

| Alert | Condition | Severity |
|-------|-----------|----------|
| HighErrorRate | Error rate > 5% for 5m | critical |
| HighLatency | p95 latency > 2s for 5m | warning |
| ServiceDown | Service down for 1m | critical |
| HighMemoryUsage | JVM heap > 85% for 5m | warning |
| DatabaseConnectionPoolExhausted | Pool > 90% for 5m | critical |
| PostgreSQLDown | PostgreSQL down for 1m | critical |
| PostgreSQLHighConnections | Connections > 80% for 5m | warning |
| PostgreSQLSlowQueries | Slow queries > 10 for 5m | warning |
| RedisDown | Redis down for 1m | critical |
| RedisHighMemoryUsage | Memory > 80% for 5m | warning |

### Grafana

- **Storage**: 10Gi persistent volume
- **Datasources**: Prometheus, Elasticsearch

#### Pre-configured Dashboards

1. **Portfolio Service Overview**
   - Request rate
   - Response latency (p50, p95, p99)
   - Error rate
   - JVM heap usage

### Deployment

```bash
kubectl apply -f monitoring/prometheus-config.yaml
kubectl apply -f monitoring/prometheus-deployment.yaml
kubectl apply -f monitoring/grafana-deployment.yaml
```

### Access

- Prometheus: http://prometheus.portfolio.example.com
- Grafana: http://grafana.portfolio.example.com (admin/admin)

## Logging Stack

### Elasticsearch

- **Version**: 8.x
- **Storage**: 100Gi persistent volume
- **Resources**: 500m-2 CPU, 2-4Gi memory
- **Heap**: 1GB

### Logstash

- **Inputs**: Beats (5044), TCP (5000)
- **Resources**: 250m-1 CPU, 1-2Gi memory

#### Log Parsing

Logstash is configured to parse logs from:
- Portfolio Service (JSON format)
- PostgreSQL (standard format)
- Batch Jobs (structured format)
- ETL Pipelines (structured format)

#### Index Patterns

- `portfolio-logs-YYYY.MM.dd` - Application logs
- `portfolio-errors-YYYY.MM.dd` - Error logs only

### Kibana

- **Resources**: 250m-1 CPU, 512Mi-1Gi memory

### Filebeat

- **Deployment**: DaemonSet on all nodes
- **Collection**: Container logs, application logs
- **Enrichment**: Kubernetes metadata

### Deployment

```bash
kubectl apply -f logging/elasticsearch-deployment.yaml
kubectl apply -f logging/logstash-deployment.yaml
kubectl apply -f logging/kibana-deployment.yaml
kubectl apply -f logging/filebeat-daemonset.yaml
```

### Access

- Kibana: http://kibana.portfolio.example.com

## CI/CD Pipeline

### Jenkins Pipeline

The Jenkinsfile defines a complete CI/CD pipeline:

1. **Checkout** - Clone repository
2. **Build** - Maven compile
3. **Unit Tests** - Run unit tests with JaCoCo coverage
4. **Integration Tests** - Run integration tests
5. **Code Quality** - SonarQube analysis
6. **Quality Gate** - Wait for SonarQube gate
7. **Package** - Maven package
8. **Build Docker Image** - Build and tag image
9. **Security Scan** - Trivy and OWASP dependency check
10. **Push Docker Image** - Push to registry
11. **Deploy to Staging** - Deploy on develop branch
12. **Deploy to Production** - Manual approval for main branch

### GitLab CI

Alternative pipeline configuration in `.gitlab-ci.yml` with equivalent stages.

### Docker Image

Multi-stage build based on Eclipse Temurin JRE 21 Alpine:

```dockerfile
Features:
- Non-root user (portfolio, UID 1000)
- JVM container support enabled
- G1GC garbage collector
- Heap dump on OOM
- Health check via actuator
- Ports: 8080 (http), 8081 (management)
```

## Security Considerations

### Secrets Management

All sensitive data is stored in Kubernetes Secrets:
- Database credentials
- Redis password
- JWT signing key
- API keys

**Note**: In production, use a secrets management solution like HashiCorp Vault or AWS Secrets Manager.

### Network Policies

Consider implementing network policies to restrict traffic:
- Allow ingress only from ingress controller
- Allow database access only from portfolio-service
- Restrict egress to required endpoints

### TLS

Configure TLS termination at the ingress level:
- Use cert-manager for automatic certificate management
- Enable HTTPS redirect
- Configure appropriate cipher suites

## Scaling Guidelines

### Horizontal Scaling

- **Portfolio Service**: Automatically scales 3-10 pods based on CPU/memory
- **PostgreSQL**: Consider read replicas for read-heavy workloads
- **Redis**: Consider Redis Cluster for high availability

### Vertical Scaling

Adjust resource limits based on observed usage:
- Monitor CPU and memory utilization
- Adjust JVM heap size accordingly
- Consider dedicated node pools for databases

## Troubleshooting

### Common Issues

1. **Pod not starting**
   ```bash
   kubectl describe pod <pod-name> -n portfolio-system
   kubectl logs <pod-name> -n portfolio-system
   ```

2. **Database connection issues**
   ```bash
   kubectl exec -it postgresql-0 -n portfolio-system -- pg_isready
   ```

3. **Redis connection issues**
   ```bash
   kubectl exec -it redis-0 -n portfolio-system -- redis-cli ping
   ```

4. **Service not accessible**
   ```bash
   kubectl get svc -n portfolio-system
   kubectl get ingress -n portfolio-system
   ```

### Useful Commands

```bash
# View all resources in namespace
kubectl get all -n portfolio-system

# View resource usage
kubectl top pods -n portfolio-system

# View HPA status
kubectl get hpa -n portfolio-system

# View persistent volumes
kubectl get pvc -n portfolio-system

# View logs
kubectl logs -f deployment/portfolio-service -n portfolio-system
```
