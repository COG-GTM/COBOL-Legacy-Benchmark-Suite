# Phase 1: Foundation Setup and Data Migration

## Overview

This directory contains all deliverables for Phase 1 of the COBOL to Modern Architecture Migration for the Investment Portfolio Management System. Phase 1 focuses on establishing the foundational infrastructure and migrating data from the legacy VSAM/DB2 storage to PostgreSQL.

## Phase 1 Objectives

1. **Infrastructure Setup** - Kubernetes cluster, CI/CD pipeline, monitoring, and logging
2. **Database Migration** - PostgreSQL schema design and ETL pipelines for VSAM/DB2 migration
3. **Core Services Framework** - Spring Boot 3.x application skeleton with JPA, Security, and Batch

## Directory Structure

```
migration/phase1/
├── infrastructure/
│   ├── kubernetes/          # Kubernetes manifests and configurations
│   ├── monitoring/          # Prometheus and Grafana configurations
│   ├── logging/             # ELK stack (Elasticsearch, Logstash, Kibana)
│   └── ci-cd/               # Jenkins and GitLab CI configurations
├── database/
│   ├── postgresql/          # PostgreSQL schema and seed data
│   ├── redis/               # Redis cache configuration
│   └── etl/                 # ETL pipeline scripts for data migration
├── application/
│   └── portfolio-service/   # Spring Boot 3.x application
└── README.md                # This file
```

## Data Migration Mapping

### VSAM to PostgreSQL

| Source (VSAM) | Target (PostgreSQL) | Description |
|---------------|---------------------|-------------|
| POSFILE (PORTFOLIO.POSITION.VSAM) | `positions` | Current portfolio positions |
| TRANHIST | `transactions` | Transaction history |
| PORTMSTR | `portfolios` | Portfolio master records |
| BCHCTL | `batch_control` | Batch control records |

### DB2 to PostgreSQL

| Source (DB2) | Target (PostgreSQL) | Description |
|--------------|---------------------|-------------|
| POSHIST | `position_history` | Position history for reporting |
| ERRLOG | `error_log` | Error logging |
| AUTHFILE | `users`, `user_authorizations` | User authentication and authorization |
| AUDITLOG | `audit_log` | Audit trail |

## Quick Start

### Prerequisites

- Kubernetes cluster (1.28+)
- kubectl configured
- Docker
- Java 21
- Maven 3.9+
- Python 3.11+
- PostgreSQL 15+
- Redis 7+

### Infrastructure Deployment

```bash
# Create namespace and apply configurations
kubectl apply -f infrastructure/kubernetes/namespace.yaml
kubectl apply -f infrastructure/kubernetes/configmap.yaml
kubectl apply -f infrastructure/kubernetes/secrets.yaml

# Deploy PostgreSQL
kubectl apply -f infrastructure/kubernetes/postgresql-deployment.yaml

# Deploy Redis
kubectl apply -f infrastructure/kubernetes/redis-deployment.yaml

# Deploy monitoring stack
kubectl apply -f infrastructure/monitoring/

# Deploy logging stack
kubectl apply -f infrastructure/logging/
```

### Database Setup

```bash
# Connect to PostgreSQL and run schema migration
psql -h localhost -U portfolio_app -d portfolio_db -f database/postgresql/001_create_schema.sql
psql -h localhost -U portfolio_app -d portfolio_db -f database/postgresql/002_seed_data.sql
```

### ETL Pipeline

```bash
cd database/etl
pip install -r requirements.txt

# Validate sources and targets
python run_migration.py --validate-only

# Run full migration
python run_migration.py
```

### Application Build

```bash
cd application/portfolio-service
mvn clean package -DskipTests

# Run locally
java -jar target/portfolio-service-1.0.0-SNAPSHOT.jar
```

## Component Details

### Kubernetes Infrastructure

- **Namespace**: `portfolio-system` with resource quotas and limits
- **PostgreSQL**: StatefulSet with persistent storage (50Gi)
- **Redis**: StatefulSet with persistent storage (10Gi)
- **Portfolio Service**: Deployment with HPA (3-10 replicas)
- **Ingress**: NGINX ingress with TLS termination

### Monitoring Stack

- **Prometheus**: Metrics collection with 30-day retention
- **Grafana**: Dashboards for service, database, and cache metrics
- **Alert Rules**: High error rate, latency, memory usage, connection pool exhaustion

### Logging Stack

- **Elasticsearch**: Single-node cluster with 100Gi storage
- **Logstash**: Log processing with filters for application, database, and batch logs
- **Kibana**: Log visualization and search
- **Filebeat**: DaemonSet for log collection from all pods

### CI/CD Pipeline

- **Jenkins**: Full pipeline with build, test, security scan, and deployment stages
- **GitLab CI**: Alternative pipeline configuration
- **Docker**: Multi-stage build with JRE 21 Alpine base

### Database Schema

The PostgreSQL schema includes:

- `portfolios` - Portfolio master records
- `positions` - Current portfolio positions
- `transactions` - Transaction records
- `position_history` - Historical position data
- `error_log` - Error logging
- `users` - User accounts
- `user_authorizations` - User permissions
- `audit_log` - Audit trail
- `batch_control` - Batch job control
- `checkpoints` - Checkpoint/restart records

### Spring Boot Application

- **Spring Boot 3.2.0** with Java 21
- **Spring Data JPA** for database access
- **Spring Security** with JWT authentication
- **Spring Batch** for batch processing
- **Spring Cache** with Redis
- **Actuator** for health checks and metrics
- **OpenAPI/Swagger** for API documentation

## COBOL to Java Mapping

| COBOL Component | Java Equivalent |
|-----------------|-----------------|
| INQPORT.cbl | PortfolioController, PortfolioService |
| INQHIST.cbl | TransactionController, TransactionService |
| SECMGR.cbl | SecurityConfig, JwtService, AuditService |
| TRNVAL00.cbl | TransactionProcessingJob (Spring Batch) |
| POSUPD00.cbl | PositionService |
| POSFILE (VSAM) | PositionRepository (JPA) |
| POSHIST (DB2) | PositionHistoryRepository (JPA) |

## Next Steps (Phase 2)

1. Implement remaining business logic in services
2. Add comprehensive unit and integration tests
3. Implement API versioning strategy
4. Add circuit breaker patterns (Resilience4j)
5. Implement distributed tracing (Micrometer Tracing)
6. Set up blue-green deployment strategy

## References

- [System Architecture](../../documentation/system-architecture.md)
- [Data Dictionary](../../documentation/technical/data-dictionary.md)
- [CICS Resource Definitions](../../src/cics/PORTDFN.csd)
- [COBOL Copybooks](../../src/copybook/)
