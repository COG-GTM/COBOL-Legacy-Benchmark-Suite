# Technology Stack Selection for COBOL to Python Migration

## Executive Summary

This document outlines the technology stack selected for migrating the Investment Portfolio Management System from COBOL/z/OS to Python. Each selection is justified based on compatibility with the existing system architecture, ease of migration, maintainability, and industry best practices.

## Database Layer

### Primary Database: PostgreSQL

**Selection:** PostgreSQL (replacing DB2 for z/OS)

**Justification:**
- PostgreSQL offers the closest feature parity to DB2 among open-source databases, including advanced SQL features, stored procedures, and transaction management
- Native support for DECIMAL types with arbitrary precision, essential for financial calculations (matching COBOL COMP-3 and DECIMAL types)
- Robust ACID compliance matching DB2's transaction guarantees
- Excellent Python integration through psycopg2 and SQLAlchemy
- Partitioning support for large tables like POSHIST (matching DB2's partition by range)
- Strong indexing capabilities including B-tree, hash, and partial indexes
- Active community and enterprise support options

**Migration Mapping:**

| DB2 Feature | PostgreSQL Equivalent |
|-------------|----------------------|
| TABLESPACE | TABLESPACE |
| PARTITION BY RANGE | PARTITION BY RANGE (native) |
| DECIMAL(15,3) | NUMERIC(15,3) |
| CHAR(n) | CHAR(n) or VARCHAR(n) |
| TIMESTAMP | TIMESTAMP |
| DATE/TIME | DATE/TIME |
| STOGROUP | Managed by filesystem |
| BUFFERPOOL | shared_buffers configuration |

### Tables to Migrate

| Original Table | Target Table | Notes |
|----------------|--------------|-------|
| POSHIST | poshist | Position history with partitioning |
| ERRLOG | error_log | Error logging |
| AUTHFILE | auth_file | User authorization |
| AUDITLOG | audit_log | Security audit trail |
| PORTFOLIO_MASTER | portfolio_master | Portfolio definitions |
| INVESTMENT_POSITIONS | investment_positions | Current positions |
| TRANSACTION_HISTORY | transaction_history | Transaction records |

## File Storage Layer

### VSAM Replacement: PostgreSQL Tables

**Selection:** PostgreSQL (replacing VSAM KSDS files)

**Justification:**
- Consolidating all data storage in PostgreSQL simplifies the architecture
- PostgreSQL's B-tree indexes provide equivalent functionality to VSAM KSDS (Key-Sequenced Data Sets)
- Eliminates the need for separate file management infrastructure
- Enables SQL-based access patterns for all data
- Supports both random and sequential access patterns required by the original VSAM files
- Transaction support across all data operations

**VSAM to PostgreSQL Mapping:**

| VSAM File | PostgreSQL Table | Key Structure |
|-----------|------------------|---------------|
| PORTMSTR (Portfolio Master) | portfolio_master | portfolio_id, account_type, branch_id |
| TRANHIST (Transaction History) | transaction_file | trans_date, trans_time, portfolio_id, sequence_no |
| POSHIST (Position History) | position_file | portfolio_id, position_date, investment_id |

**Key Considerations:**
- VSAM KSDS alternate indexes map to PostgreSQL secondary indexes
- VSAM SHARE OPTIONS(2,3) behavior achieved through PostgreSQL's MVCC
- VSAM CI/CA splits handled automatically by PostgreSQL's page management
- Record-level locking preserved through PostgreSQL's row-level locking

## Job Scheduling

### Batch Processing: Apache Airflow

**Selection:** Apache Airflow (replacing z/OS Job Scheduler and JCL)

**Justification:**
- Industry-standard workflow orchestration tool with strong Python integration
- DAG (Directed Acyclic Graph) model naturally maps to JCL job dependencies
- Built-in support for task dependencies, retries, and error handling
- Web UI for monitoring and manual intervention (replacing operator console)
- Extensive logging and alerting capabilities
- Support for checkpoint/restart patterns through task-level granularity
- Scalable execution through Celery or Kubernetes executors

**JCL to Airflow Mapping:**

| JCL Concept | Airflow Equivalent |
|-------------|-------------------|
| JOB | DAG |
| EXEC PGM | PythonOperator / BashOperator |
| DD statements | Task parameters / connections |
| COND parameter | Task dependencies / trigger rules |
| RESTART | Task retry / manual trigger |
| NOTIFY | Email/Slack operators |
| SCHEDULE | schedule_interval |

**Batch Jobs to Migrate:**

| Original Job | Airflow DAG | Description |
|--------------|-------------|-------------|
| BCHCTL00 | batch_control_dag | Batch control and orchestration |
| PRCSEQ00 | process_sequence_dag | Process sequencing |
| HISTLD00 | history_load_dag | History loading to database |
| RPTPOS00 | position_report_dag | Position reporting |
| RPTAUD00 | audit_report_dag | Audit reporting |
| RPTSTA00 | statistics_report_dag | Statistics reporting |

## Web Interface

### Backend Framework: Flask

**Selection:** Flask (replacing CICS/BMS)

**Justification:**
- Lightweight and flexible, suitable for the relatively simple screen flows
- Easy to learn and maintain
- Excellent REST API support for modern frontend integration
- Flask-SQLAlchemy for seamless database integration
- Flask-Login for session management (replacing CICS session handling)
- Extensive ecosystem of extensions
- Well-suited for the inquiry-focused nature of the online system

**CICS to Flask Mapping:**

| CICS Concept | Flask Equivalent |
|--------------|------------------|
| Transaction | Route/Endpoint |
| COMMAREA | Session / Request context |
| LINK | Function call / Service class |
| SEND MAP | render_template / JSON response |
| RECEIVE MAP | request.form / request.json |
| HANDLE CONDITION | Exception handlers / try-except |
| ASSIGN USERID | Flask-Login current_user |

### Frontend Framework: React

**Selection:** React (replacing BMS screen maps)

**Justification:**
- Component-based architecture maps well to BMS map structure
- Strong typing support with TypeScript for data validation
- Rich ecosystem for building financial dashboards
- Excellent state management for complex forms
- Modern user experience while preserving workflow logic
- Easy integration with Flask REST APIs

**BMS to React Mapping:**

| BMS Map | React Component | Description |
|---------|-----------------|-------------|
| MENMAP | MainMenu.tsx | Main menu with navigation |
| POSMAP | PositionInquiry.tsx | Portfolio position display |
| HISMAP | HistoryInquiry.tsx | Transaction history display |
| ERRMAP | ErrorDisplay.tsx | Error message display |

## Testing Framework

### Selection: pytest

**Justification:**
- De facto standard for Python testing
- Fixture system for test data setup (replacing TSTGEN00 functionality)
- Parameterized tests for validation scenarios
- Integration with coverage tools
- Easy mocking and patching for unit tests
- Support for integration and end-to-end testing

**Test Migration Strategy:**

| Original Component | pytest Equivalent |
|-------------------|-------------------|
| TSTGEN00 | pytest fixtures / factory_boy |
| TSTVAL00 | pytest test cases with assertions |
| Test data files | pytest fixtures / test databases |

## ORM and Data Access

### Selection: SQLAlchemy

**Justification:**
- Industry-standard Python ORM with excellent PostgreSQL support
- Supports both ORM and raw SQL patterns
- Connection pooling (replacing DB2 connection management)
- Transaction management with savepoints
- Migration support through Alembic
- Type-safe query building

## Data Validation

### Selection: Pydantic

**Justification:**
- Strong data validation matching COBOL's strict typing
- Automatic type coercion where appropriate
- Clear error messages for validation failures
- Integration with FastAPI/Flask for request validation
- Serialization/deserialization support

## Additional Libraries

| Purpose | Library | Replacing |
|---------|---------|-----------|
| Database migrations | Alembic | Manual DDL scripts |
| Configuration | python-dotenv | JCL PARM values |
| Logging | Python logging + structlog | DISPLAY statements |
| Date/Time handling | datetime + python-dateutil | COBOL date functions |
| Decimal arithmetic | decimal.Decimal | COMP-3 / DECIMAL |
| HTTP client | requests | N/A (new capability) |
| Task queue | Celery (optional) | Batch job queue |

## Architecture Overview

```
+------------------+     +------------------+     +------------------+
|   React Frontend |     |   Flask Backend  |     |   Airflow        |
|   (BMS replacement)    |   (CICS replacement)   |   (JCL replacement)
+--------+---------+     +--------+---------+     +--------+---------+
         |                        |                        |
         +------------------------+------------------------+
                                  |
                    +-------------+-------------+
                    |                           |
            +-------+-------+           +-------+-------+
            |   SQLAlchemy  |           |    Alembic    |
            |   (DB2 access)|           |  (Migrations) |
            +-------+-------+           +-------+-------+
                    |                           |
                    +-------------+-------------+
                                  |
                    +-------------+-------------+
                    |        PostgreSQL         |
                    |   (DB2 + VSAM replacement)|
                    +---------------------------+
```

## Data Type Mapping

| COBOL Type | Python Type | SQLAlchemy Type |
|------------|-------------|-----------------|
| PIC X(n) | str | String(n) |
| PIC 9(n) | int | Integer |
| PIC S9(n) COMP | int | Integer |
| PIC S9(n)V9(m) COMP-3 | Decimal | Numeric(n+m, m) |
| PIC S9(n)V9(m) | Decimal | Numeric(n+m, m) |
| DATE (YYYYMMDD) | datetime.date | Date |
| TIME (HHMMSS) | datetime.time | Time |
| TIMESTAMP | datetime.datetime | DateTime |

## Security Considerations

| Original | Python Replacement |
|----------|-------------------|
| RACF/ACF2 | Flask-Login + custom auth |
| CICS security | JWT tokens / session auth |
| DB2 grants | PostgreSQL roles/grants |
| Audit logging | Python logging + audit table |

## Deployment Considerations

- **Containerization:** Docker for consistent deployment
- **Orchestration:** Docker Compose for development, Kubernetes for production
- **CI/CD:** GitHub Actions for automated testing and deployment
- **Monitoring:** Prometheus + Grafana for metrics
- **Logging:** ELK stack or CloudWatch for centralized logging

## Conclusion

This technology stack provides a modern, maintainable, and scalable replacement for the legacy COBOL/z/OS infrastructure while preserving the business logic and data integrity requirements of the Investment Portfolio Management System. The selections prioritize:

1. **Data integrity:** PostgreSQL's ACID compliance and precise numeric types
2. **Maintainability:** Python's readability and extensive ecosystem
3. **Scalability:** Airflow's distributed execution and PostgreSQL's performance
4. **Developer experience:** Modern tooling and debugging capabilities
5. **Migration path:** Clear mappings from COBOL constructs to Python equivalents
