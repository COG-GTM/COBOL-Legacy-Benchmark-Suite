# AWS Infrastructure Setup Guide

## Overview

This document outlines the AWS infrastructure setup for the COBOL Legacy Benchmark Suite modernization project. The architecture is designed to be scalable, secure, and cost-effective while supporting the migration from mainframe to cloud-native technologies.

## Architecture Diagram

```
                                    ┌─────────────────────────────────────────────────────────────┐
                                    │                         AWS Cloud                            │
                                    │  ┌─────────────────────────────────────────────────────────┐ │
                                    │  │                        VPC                               │ │
                                    │  │  ┌──────────────────┐    ┌──────────────────┐          │ │
┌──────────┐    ┌──────────┐       │  │  │  Public Subnet   │    │  Public Subnet   │          │ │
│  Users   │───▶│   ALB    │───────┼──┼──│  (AZ-a)          │    │  (AZ-b)          │          │ │
└──────────┘    └──────────┘       │  │  │  ┌────────────┐  │    │  ┌────────────┐  │          │ │
                                    │  │  │  │   NAT GW   │  │    │  │   NAT GW   │  │          │ │
                                    │  │  │  └────────────┘  │    │  └────────────┘  │          │ │
                                    │  │  └──────────────────┘    └──────────────────┘          │ │
                                    │  │                                                         │ │
                                    │  │  ┌──────────────────┐    ┌──────────────────┐          │ │
                                    │  │  │  Private Subnet  │    │  Private Subnet  │          │ │
                                    │  │  │  (AZ-a)          │    │  (AZ-b)          │          │ │
                                    │  │  │  ┌────────────┐  │    │  ┌────────────┐  │          │ │
                                    │  │  │  │    EKS     │  │    │  │    EKS     │  │          │ │
                                    │  │  │  │   Nodes    │  │    │  │   Nodes    │  │          │ │
                                    │  │  │  └────────────┘  │    │  └────────────┘  │          │ │
                                    │  │  └──────────────────┘    └──────────────────┘          │ │
                                    │  │                                                         │ │
                                    │  │  ┌──────────────────┐    ┌──────────────────┐          │ │
                                    │  │  │  Database Subnet │    │  Database Subnet │          │ │
                                    │  │  │  (AZ-a)          │    │  (AZ-b)          │          │ │
                                    │  │  │  ┌────────────┐  │    │  ┌────────────┐  │          │ │
                                    │  │  │  │  RDS       │  │◀──▶│  │  RDS       │  │          │ │
                                    │  │  │  │  Primary   │  │    │  │  Standby   │  │          │ │
                                    │  │  │  └────────────┘  │    │  └────────────┘  │          │ │
                                    │  │  └──────────────────┘    └──────────────────┘          │ │
                                    │  └─────────────────────────────────────────────────────────┘ │
                                    └─────────────────────────────────────────────────────────────┘
```

## Prerequisites

Before setting up the infrastructure, ensure you have:

1. AWS CLI installed and configured with appropriate credentials
2. kubectl installed (v1.28+)
3. eksctl installed (v0.165+)
4. Terraform installed (v1.6+) - optional, for IaC approach
5. Helm installed (v3.13+)

## Infrastructure Components

### 1. Virtual Private Cloud (VPC)

Create a VPC with the following configuration:

```bash
# Create VPC using AWS CLI
aws ec2 create-vpc \
    --cidr-block 10.0.0.0/16 \
    --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=portfolio-modernization-vpc}]'
```

**Subnet Configuration:**
- Public Subnets: 10.0.1.0/24 (AZ-a), 10.0.2.0/24 (AZ-b)
- Private Subnets: 10.0.10.0/24 (AZ-a), 10.0.20.0/24 (AZ-b)
- Database Subnets: 10.0.100.0/24 (AZ-a), 10.0.200.0/24 (AZ-b)

### 2. Amazon EKS Cluster

Create an EKS cluster for running the modernized application:

```bash
# Create EKS cluster using eksctl
eksctl create cluster \
    --name portfolio-modernization \
    --version 1.28 \
    --region us-east-1 \
    --nodegroup-name standard-workers \
    --node-type t3.medium \
    --nodes 3 \
    --nodes-min 2 \
    --nodes-max 5 \
    --managed \
    --with-oidc \
    --ssh-access \
    --ssh-public-key my-key
```

### 3. Amazon RDS (PostgreSQL)

Create a PostgreSQL RDS instance to replace DB2:

```bash
# Create RDS PostgreSQL instance
aws rds create-db-instance \
    --db-instance-identifier portfolio-postgres \
    --db-instance-class db.r6g.large \
    --engine postgres \
    --engine-version 16.1 \
    --master-username portfolio_admin \
    --master-user-password <secure-password> \
    --allocated-storage 100 \
    --storage-type gp3 \
    --multi-az \
    --vpc-security-group-ids sg-xxxxxxxx \
    --db-subnet-group-name portfolio-db-subnet-group \
    --backup-retention-period 7 \
    --storage-encrypted \
    --kms-key-id alias/aws/rds
```

### 4. Amazon ECR (Container Registry)

Create an ECR repository for Docker images:

```bash
# Create ECR repository
aws ecr create-repository \
    --repository-name portfolio-modernization \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256
```

### 5. Application Load Balancer

The ALB is automatically provisioned by the AWS Load Balancer Controller when deploying the Kubernetes Ingress resource.

Install the AWS Load Balancer Controller:

```bash
# Add the EKS chart repo
helm repo add eks https://aws.github.io/eks-charts

# Install the AWS Load Balancer Controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
    -n kube-system \
    --set clusterName=portfolio-modernization \
    --set serviceAccount.create=false \
    --set serviceAccount.name=aws-load-balancer-controller
```

## Security Configuration

### IAM Roles and Policies

1. **EKS Node Role**: Allows EC2 instances to join the EKS cluster
2. **EKS Service Role**: Allows EKS to manage AWS resources
3. **RDS Access Role**: Allows applications to access RDS using IAM authentication

### Security Groups

| Security Group | Inbound Rules | Outbound Rules |
|----------------|---------------|----------------|
| ALB SG | 443 from 0.0.0.0/0 | All to VPC CIDR |
| EKS Node SG | All from ALB SG, All from self | All to 0.0.0.0/0 |
| RDS SG | 5432 from EKS Node SG | None |

### Secrets Management

Use AWS Secrets Manager for storing sensitive data:

```bash
# Create secret for database credentials
aws secretsmanager create-secret \
    --name portfolio/database/credentials \
    --secret-string '{"username":"portfolio_admin","password":"<secure-password>"}'
```

## Monitoring and Logging

### Amazon CloudWatch

Configure CloudWatch for centralized logging and monitoring:

1. **Container Insights**: Enable for EKS cluster
2. **RDS Performance Insights**: Enable for database monitoring
3. **Custom Metrics**: Application metrics via CloudWatch agent

### Prometheus and Grafana

Deploy Prometheus and Grafana for application-level monitoring:

```bash
# Install Prometheus using Helm
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack \
    --namespace monitoring \
    --create-namespace
```

## Cost Estimation

| Service | Configuration | Estimated Monthly Cost |
|---------|---------------|------------------------|
| EKS Cluster | 1 cluster | $73 |
| EC2 (EKS Nodes) | 3x t3.medium | $100 |
| RDS PostgreSQL | db.r6g.large, Multi-AZ | $400 |
| ALB | 1 load balancer | $25 |
| NAT Gateway | 2 gateways | $90 |
| ECR | 10GB storage | $1 |
| CloudWatch | Logs and metrics | $50 |
| **Total** | | **~$739/month** |

*Note: Costs are estimates and may vary based on usage patterns and region.*

## Deployment Steps

1. **Set up VPC and networking**
   ```bash
   # Apply Terraform configuration or use AWS CLI commands above
   ```

2. **Create EKS cluster**
   ```bash
   eksctl create cluster -f cluster-config.yaml
   ```

3. **Set up RDS PostgreSQL**
   ```bash
   # Create RDS instance and run Flyway migrations
   ```

4. **Configure ECR and push images**
   ```bash
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com
   docker push <account>.dkr.ecr.us-east-1.amazonaws.com/portfolio-modernization:latest
   ```

5. **Deploy application to EKS**
   ```bash
   kubectl apply -k k8s/overlays/prod
   ```

## Disaster Recovery

### Backup Strategy

- **RDS**: Automated daily backups with 7-day retention
- **EKS**: etcd backups via Velero
- **Application Data**: S3 cross-region replication

### Recovery Procedures

1. **Database Recovery**: Restore from RDS snapshot
2. **Application Recovery**: Redeploy from ECR images
3. **Full DR**: Failover to secondary region using Route 53

## Next Steps

1. Implement Infrastructure as Code using Terraform
2. Set up CI/CD pipeline integration with AWS CodePipeline
3. Configure AWS WAF for additional security
4. Implement cost optimization with Reserved Instances
