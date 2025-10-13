# COBOL to Java Migration Guide

## Overview

This guide documents the systematic migration of the Investment Portfolio Management System from COBOL to Java. The migration follows a 10-sprint approach (2 weeks per sprint) designed to maintain functionality while modernizing the technology stack.

## Migration Strategy

### Principles

1. **Incremental Migration**: Migrate components progressively to minimize risk
2. **Dual-System Support**: Maintain both COBOL and Java implementations during transition
3. **Functional Equivalence**: Ensure Java implementation maintains COBOL business logic
4. **Modern Best Practices**: Apply contemporary Java and Spring Boot patterns
5. **Testability First**: Establish comprehensive testing before and after migration

### Technology Stack

#### COBOL (Original)
- **Platform**: IBM z/OS Mainframe
- **Language**: Enterprise COBOL for z/OS
- **Storage**: VSAM files
- **Database**: DB2 for z/OS
- **Online Processing**: CICS
- **Batch Processing**: JCL-controlled batch jobs

#### Java (Target)
- **Platform**: Linux/Docker containers
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Database**: PostgreSQL
- **ORM**: JPA/Hibernate
- **Batch Processing**: Spring Batch 5.1.0
- **Online Processing**: REST APIs
- **Build Tool**: Maven
- **Testing**: JUnit 5, H2 embedded database

## Sprint Breakdown

### Sprint 0: Foundation & Infrastructure ✅ COMPLETE

**Duration**: 2 weeks  
**Status**: Complete

#### Objectives
Establish the Java project structure, database schema, domain model, and testing infrastructure.

#### Deliverables
- ✅ Repository restructured for dual COBOL/Java support
- ✅ Multi-module Maven project with Spring Boot 3.2.0
- ✅ JPA domain entities based on COBOL copybooks
- ✅ Database migration scripts (Flyway) for PostgreSQL
- ✅ Testing infrastructure with H2 embedded database
- ✅ CI/CD pipeline (GitHub Actions)

#### Key Migrations

##### Data Structure Mapping

| COBOL Copybook | Java Entity | Database Table | Notes |
|----------------|-------------|----------------|-------|
| TRNREC | TransactionRecord | transactions | Transaction records with COMP-3 fields mapped to BigDecimal |
| POSREC | PositionRecord | positions | Portfolio position records |
| HISTREC | HistoryRecord | position_history | Audit history records |

##### Field Type Mappings

| COBOL Type | Java Type | PostgreSQL Type | Example |
|------------|-----------|-----------------|---------|
| PIC X(n) | String | VARCHAR(n) | Portfolio ID |
| PIC 9(n) | BigDecimal/Long | NUMERIC/BIGINT | Sequence numbers |
| PIC S9(m)V9(n) COMP-3 | BigDecimal | NUMERIC(m+n, n) | Amounts, quantities |
| PIC X(08) (YYYYMMDD) | LocalDate | DATE | Transaction date |
| PIC X(06) (HHMMSS) | LocalTime | TIME | Transaction time |
| PIC X(26) (timestamp) | LocalDateTime | TIMESTAMP | Process timestamps |

#### Exit Criteria
- ✅ All Maven modules compile successfully
- ✅ Database schema deployable via Flyway migrations
- ✅ Domain entities have proper JPA annotations
- ✅ CI/CD pipeline executes successfully

### Sprint 1: Data Access Layer (Planned)

**Duration**: 2 weeks  
**Status**: Not Started

#### Objectives
Implement JPA repositories and data access services for CRUD operations.

#### Planned Deliverables
- JPA repositories for TransactionRecord, PositionRecord, HistoryRecord
- Custom query methods for complex searches
- Transaction management configuration
- Repository integration tests

### Sprint 2: Batch Processing Framework (Planned)

**Duration**: 2 weeks  
**Status**: Not Started

#### Objectives
Establish Spring Batch infrastructure for batch job processing.

#### Planned Deliverables
- Spring Batch configuration
- Job repository setup
- Chunk-based processing framework
- Job scheduling infrastructure

### Sprint 3: Transaction Validation & Processing (Planned)

**Duration**: 2 weeks  
**Status**: Not Started

#### Objectives
Migrate TRNVAL00 batch program to Java Spring Batch job.

#### Planned Components
- Transaction validation job
- Business rule validation
- Error handling and logging
- Integration with transaction repository

### Sprint 4: Position Update Services (Planned)

**Duration**: 2 weeks  
**Status**: Not Started

#### Objectives
Migrate POSUPD00 batch program to Java Spring Batch job.

#### Planned Components
- Position calculation logic
- Position update job
- History tracking
- Data consistency checks

### Sprint 5: Online REST APIs (Planned)

**Duration**: 2 weeks  
**Status**: Not Started

#### Objectives
Replace CICS online programs with REST APIs.

#### Planned Components
- Portfolio inquiry endpoints
- Transaction history endpoints
- Error handling middleware
- API documentation (OpenAPI/Swagger)

### Sprint 6: Reporting Services (Planned)

**Duration**: 2 weeks  
**Status**: Not Started

#### Objectives
Migrate COBOL reporting programs to Java reporting services.

#### Planned Components
- Position report generation
- Audit report generation
- Statistics report generation
- Report scheduling

### Sprint 7: Security & Authentication (Planned)

**Duration**: 2 weeks  
**Status**: Not Started

#### Objectives
Implement security framework replacing RACF/SECMGR.

#### Planned Components
- Spring Security configuration
- JWT authentication
- Role-based access control
- Audit logging

### Sprint 8: Integration Testing & Performance Tuning (Planned)

**Duration**: 2 weeks  
**Status**: Not Started

#### Objectives
Comprehensive testing and performance optimization.

#### Planned Components
- End-to-end integration tests
- Load testing
- Performance benchmarking vs COBOL
- Optimization based on metrics

### Sprint 9: Deployment & Documentation (Planned)

**Duration**: 2 weeks  
**Status**: Not Started

#### Objectives
Production deployment preparation and final documentation.

#### Planned Components
- Docker containerization
- Kubernetes deployment configurations
- Operations runbooks
- Migration completion documentation

## Data Migration Considerations

### VSAM to PostgreSQL

VSAM files in the COBOL system are replaced with PostgreSQL tables in the Java system:

| VSAM File | Purpose | PostgreSQL Table | Migration Notes |
|-----------|---------|------------------|-----------------|
| POSFILE | Portfolio positions | positions | Direct mapping with indexes |
| TRNFILE | Transactions | transactions | Date/time fields converted |
| HISTFILE | History records | position_history | Text images stored as TEXT |

### DB2 to PostgreSQL

The COBOL system uses DB2 for z/OS. The Java system uses PostgreSQL:

**Key Differences**:
- SQL dialect differences (handled by JPA/Hibernate)
- Date/time function differences (abstracted by JPA)
- Locking mechanisms (Spring transaction management)
- Stored procedures replaced with Java service methods

## Testing Strategy

### Unit Testing
- JUnit 5 for all service and business logic classes
- Mockito for mocking dependencies
- H2 in-memory database for repository tests

### Integration Testing
- Spring Boot Test with TestContainers for PostgreSQL
- End-to-end API testing with REST Assured
- Spring Batch test utilities for job testing

### Validation Testing
- Compare COBOL output with Java output for identical inputs
- Benchmark performance metrics
- Validate business rule preservation

## Performance Considerations

### COBOL Baseline
- Batch processing: ~10,000 transactions/minute
- Online response: <100ms for inquiries
- Report generation: ~5 minutes for daily reports

### Java Targets
- Batch processing: Match or exceed COBOL throughput
- Online response: <50ms for REST API calls
- Report generation: Match or exceed COBOL performance

## Risk Mitigation

### Data Consistency
- Parallel run both systems during transition
- Daily reconciliation reports
- Automated data validation

### Business Logic Preservation
- Comprehensive unit tests for all business rules
- Side-by-side comparison testing
- User acceptance testing

### Performance
- Load testing before production deployment
- Gradual traffic migration
- Rollback procedures documented

## Rollout Plan

1. **Phase 1**: Deploy Java system in parallel with COBOL (read-only mode)
2. **Phase 2**: Gradual traffic shift (10% → 50% → 100%)
3. **Phase 3**: Decommission COBOL system
4. **Phase 4**: Archive COBOL codebase for reference

## Tools and Resources

### Development Tools
- **IDE**: IntelliJ IDEA or Eclipse
- **Build**: Maven 3.8+
- **Version Control**: Git
- **CI/CD**: GitHub Actions
- **Database**: PostgreSQL 15+, DBeaver for management

### Monitoring and Operations
- **Logging**: SLF4J with Logback
- **Metrics**: Spring Boot Actuator + Micrometer
- **APM**: Recommended tools (New Relic, Datadog, or Dynatrace)

## Success Criteria

The migration is considered successful when:

1. ✅ All Sprint 0-9 deliverables are complete
2. ⏳ Java system passes all functional tests
3. ⏳ Performance meets or exceeds COBOL baseline
4. ⏳ Zero data integrity issues for 30 consecutive days
5. ⏳ User acceptance testing completed
6. ⏳ Operations team trained and comfortable
7. ⏳ Documentation complete and accessible

## References

- [System Architecture Document](../../cobol/documentation/technical/system-architecture.md)
- [Data Dictionary](../../cobol/documentation/technical/data-dictionary.md)
- [COBOL Source Code](../../cobol/src/)
- [Java Source Code](../../java/)
- [JIRA Project: MBA-461](https://cog-gtm.atlassian.net/browse/MBA-461)

## Contact and Support

For questions or issues related to the migration:
- **JIRA**: [MBA Project](https://cog-gtm.atlassian.net/jira/software/projects/MBA)
- **Repository**: [COBOL-Legacy-Benchmark-Suite](https://github.com/COG-GTM/COBOL-Legacy-Benchmark-Suite)
