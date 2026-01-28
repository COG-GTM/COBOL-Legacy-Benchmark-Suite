# Technology Stack Selection for COBOL to Python Migration

## Overview

This document specifies the technology stack selected for migrating the Investment Portfolio Management System from COBOL/z/OS to a modern Python-based architecture. Each selection includes justification based on the system requirements identified in the architecture analysis.

## Database Layer

### Primary Database: PostgreSQL

**Selection**: PostgreSQL 15+

**Justification**:
- **Enterprise-grade reliability**: PostgreSQL offers ACID compliance and robust transaction support comparable to DB2
- **Rich SQL support**: Advanced SQL features including window functions, CTEs, and JSON support for flexible data handling
- **Partitioning support**: Native table partitioning matches the quarterly partitioning used in DB2 POSHIST table
- **Excellent Python integration**: Mature drivers (psycopg2, asyncpg) and SQLAlchemy support
- **Cost-effective**: Open-source with no licensing fees, reducing total cost of ownership
- **Scalability**: Supports horizontal scaling through read replicas and logical replication

**DB2 Table Migration Mapping**:

| DB2 Table | PostgreSQL Table | Notes |
|-----------|------------------|-------|
| POSHIST | position_history | Partitioned by trans_date |
| ERRLOG | error_log | Standard table with indexes |
| AUTHFILE | auth_permissions | Normalized structure |
| AUDITLOG | audit_log | Time-series optimized |
| PORTFOLIO_MASTER | portfolios | Primary entity table |
| INVESTMENT_POSITIONS | positions | Foreign key to portfolios |
| TRANSACTION_HISTORY | transactions | Foreign key to portfolios |

### VSAM File Replacement: PostgreSQL

**Selection**: PostgreSQL (same instance as DB2 replacement)

**Justification**:
- **Unified data layer**: Consolidating VSAM and DB2 into a single PostgreSQL instance simplifies architecture
- **KSDS equivalent**: PostgreSQL B-tree indexes provide similar key-sequenced access patterns
- **Transaction support**: Full ACID compliance for data integrity
- **Reduced complexity**: Single database technology reduces operational overhead

**VSAM File Migration Mapping**:

| VSAM File | PostgreSQL Table | Key Structure |
|-----------|------------------|---------------|
| PORTMSTR | portfolio_master | portfolio_id, account_type, branch_id |
| TRANHIST | transaction_history_vsam | trans_date, trans_time, portfolio_id, sequence_no |
| POSHIST (VSAM) | position_history_vsam | portfolio_id, position_date, investment_id |

**Alternative Considered**: SQLite was considered for VSAM replacement due to its file-based nature, but PostgreSQL was chosen for consistency and to avoid managing multiple database technologies.

## Job Scheduling Layer

### Batch Scheduler: Apache Airflow

**Selection**: Apache Airflow 2.x

**Justification**:
- **DAG-based workflows**: Directed Acyclic Graphs naturally model the job dependencies managed by BCHCTL00 and PRCSEQ00
- **Python-native**: Workflows defined in Python, enabling seamless integration with migrated business logic
- **Rich operator ecosystem**: Built-in operators for database operations, file handling, and external system integration
- **Monitoring and alerting**: Web UI for job monitoring, logging, and failure alerting
- **Retry and recovery**: Built-in retry logic and task-level recovery matches COBOL checkpoint/restart patterns
- **Scalability**: Supports distributed execution with Celery or Kubernetes executors

**JCL Job Migration Mapping**:

| JCL Job | Airflow DAG | Schedule |
|---------|-------------|----------|
| RPTPOS.jcl | daily_position_report_dag | Daily |
| RPTAUD.jcl | audit_report_dag | Daily |
| RPTSTA.jcl | statistics_report_dag | Daily |
| UTLMNT.jcl | file_maintenance_dag | Weekly |
| UTLMON.jcl | system_monitoring_dag | Continuous |
| UTLVAL.jcl | data_validation_dag | Daily |
| TSTGEN.jcl | test_data_generation_dag | On-demand |
| TSTVAL.jcl | test_validation_dag | On-demand |

**Alternative Considered**: Celery was considered for its simplicity, but Airflow was chosen for its superior workflow visualization, dependency management, and built-in scheduling capabilities that better match z/OS job scheduling patterns.

## Web Interface Layer

### Backend Framework: Flask

**Selection**: Flask 3.x with Flask-RESTful

**Justification**:
- **Lightweight and flexible**: Minimal overhead, allowing precise control over application structure
- **REST API focus**: Flask-RESTful extension provides clean API endpoint definition
- **Microservices-friendly**: Easy to decompose into smaller services if needed
- **Mature ecosystem**: Extensive extensions for authentication, database integration, and API documentation
- **Learning curve**: Simpler than Django for teams new to Python web development
- **CICS replacement**: Flask routes map naturally to CICS transaction handlers

**CICS Program Migration Mapping**:

| CICS Program | Flask Blueprint/Endpoint | Purpose |
|--------------|-------------------------|---------|
| INQONLN | /api/inquiry/ | Main inquiry controller |
| INQPORT | /api/inquiry/portfolio | Portfolio position inquiry |
| INQHIST | /api/inquiry/history | Transaction history inquiry |
| SECMGR | /api/auth/ | Authentication and authorization |

### Frontend Framework: React

**Selection**: React 18+ with TypeScript

**Justification**:
- **Component-based architecture**: Maps well to BMS screen maps (MENMAP, POSMAP, HISMAP, ERRMAP)
- **Rich ecosystem**: Extensive libraries for forms, tables, and data visualization
- **TypeScript support**: Type safety reduces runtime errors and improves maintainability
- **Industry standard**: Large talent pool and community support
- **State management**: Redux or React Query for managing application state
- **Responsive design**: Modern UI that works across devices

**BMS Map Migration Mapping**:

| BMS Map | React Component | Purpose |
|---------|-----------------|---------|
| MENMAP | MainMenu.tsx | Main navigation menu |
| POSMAP | PortfolioPosition.tsx | Portfolio position display |
| HISMAP | TransactionHistory.tsx | Transaction history table |
| ERRMAP | ErrorDisplay.tsx | Error message display |

**Alternative Considered**: Vue.js was considered for its gentler learning curve, but React was chosen for its larger ecosystem and better TypeScript integration.

## Testing Framework

### Selection: pytest

**Justification**:
- **Python standard**: De facto standard for Python testing
- **Fixture system**: Powerful fixtures for test setup and teardown
- **Plugin ecosystem**: Extensions for coverage, mocking, and parallel execution
- **Parameterized tests**: Easy to create data-driven tests matching TSTGEN00/TSTVAL00 patterns
- **Integration testing**: Supports both unit and integration testing
- **CI/CD integration**: Excellent support in all major CI systems

**Test Migration Mapping**:

| COBOL Test Program | pytest Module | Purpose |
|-------------------|---------------|---------|
| TSTGEN00 | tests/fixtures/data_generators.py | Test data generation |
| TSTVAL00 | tests/validation/ | Result validation tests |

## Additional Technology Selections

### ORM: SQLAlchemy 2.x

**Justification**:
- Industry-standard Python ORM
- Supports both ORM and Core (raw SQL) patterns
- Excellent PostgreSQL support
- Alembic for database migrations

### Authentication: Flask-JWT-Extended

**Justification**:
- JWT-based authentication for stateless API
- Replaces CICS RACF/security integration
- Supports refresh tokens and token revocation

### API Documentation: OpenAPI/Swagger

**Justification**:
- Auto-generated API documentation
- Interactive API testing
- Client SDK generation

### Logging: Python logging + structlog

**Justification**:
- Structured logging for better searchability
- Replaces ERRLOG DB2 table functionality
- Integration with log aggregation systems

### Configuration: python-dotenv + Pydantic Settings

**Justification**:
- Environment-based configuration
- Type-safe configuration validation
- Replaces JCL parameter passing

## Architecture Diagram

```
                                    +------------------+
                                    |   React Frontend |
                                    |   (TypeScript)   |
                                    +--------+---------+
                                             |
                                             | REST API
                                             |
                                    +--------v---------+
                                    |   Flask Backend  |
                                    |   (Python 3.11+) |
                                    +--------+---------+
                                             |
                    +------------------------+------------------------+
                    |                        |                        |
           +--------v--------+      +--------v--------+      +--------v--------+
           |    Services     |      |    Services     |      |    Services     |
           | (Business Logic)|      |  (Reporting)    |      |   (Utilities)   |
           +-----------------+      +-----------------+      +-----------------+
                    |                        |                        |
                    +------------------------+------------------------+
                                             |
                                    +--------v---------+
                                    |   SQLAlchemy     |
                                    |   (ORM Layer)    |
                                    +--------+---------+
                                             |
                                    +--------v---------+
                                    |   PostgreSQL     |
                                    |   (Database)     |
                                    +------------------+

                    +------------------+
                    |  Apache Airflow  |
                    | (Job Scheduling) |
                    +--------+---------+
                             |
                    +--------v---------+
                    |  Batch Services  |
                    | (Python Workers) |
                    +------------------+
```

## Dependency Summary

### Core Dependencies

```
# Web Framework
flask>=3.0.0
flask-restful>=0.3.10
flask-cors>=4.0.0
flask-jwt-extended>=4.6.0

# Database
sqlalchemy>=2.0.0
psycopg2-binary>=2.9.9
alembic>=1.13.0

# Data Validation
pydantic>=2.5.0
pydantic-settings>=2.1.0

# Job Scheduling
apache-airflow>=2.8.0

# Testing
pytest>=7.4.0
pytest-cov>=4.1.0
pytest-asyncio>=0.21.0

# Utilities
python-dotenv>=1.0.0
structlog>=23.2.0
```

### Frontend Dependencies

```
# Core
react>=18.2.0
react-dom>=18.2.0
typescript>=5.3.0

# State Management
@tanstack/react-query>=5.0.0

# UI Components
@mui/material>=5.15.0

# HTTP Client
axios>=1.6.0

# Routing
react-router-dom>=6.21.0
```

## Migration Considerations

### Data Type Mappings

| COBOL Type | Python Type | PostgreSQL Type |
|------------|-------------|-----------------|
| PIC X(n) | str | VARCHAR(n) or CHAR(n) |
| PIC 9(n) | int | INTEGER or BIGINT |
| PIC S9(n)V9(m) COMP-3 | Decimal | DECIMAL(n+m, m) |
| PIC S9(n) COMP | int | INTEGER or BIGINT |

### Performance Considerations

1. **Connection Pooling**: SQLAlchemy connection pool replaces DB2ONLN connection management
2. **Batch Processing**: Airflow task parallelization for improved throughput
3. **Caching**: Redis can be added for frequently accessed data
4. **Indexing**: PostgreSQL indexes designed to match COBOL access patterns

### Security Considerations

1. **Authentication**: JWT tokens replace CICS RACF integration
2. **Authorization**: Role-based access control (RBAC) replaces AUTHFILE
3. **Audit Logging**: Structured logging replaces AUDITLOG table
4. **Data Encryption**: TLS for data in transit, PostgreSQL encryption for data at rest
