# COBOL Legacy Benchmark Suite: Investment Portfolio Management System

![Project Diagram](cobol/documentation/assets/CLBS-logo.png)

[![Project Status: Active](https://img.shields.io/badge/Project%20Status-Active-green.svg)]()
[![COBOL](https://img.shields.io/badge/COBOL-Enterprise-blue.svg)]()
[![Java](https://img.shields.io/badge/Java-17-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)]()

A production-grade implementation of an Investment Portfolio Management System designed to challenge and benchmark Large Language Model (LLM) translation tools, specifically for COBOL modernization efforts. This repository contains both the original Enterprise COBOL implementation and a parallel Java implementation to demonstrate the migration path.

## Project Overview

This project presents a comprehensive COBOL-based system that simulates the complexity of real-world legacy applications. Developed using Anthropic Claude 3.5 Sonnet over five sprints, it serves as a robust test case for evaluating and fine-tuning LLM translation tools aimed at modernizing COBOL codebases.

### Purpose

- **Benchmarking LLMs**: Provides a complex, production-grade COBOL codebase to assess the capabilities of LLMs in translating legacy code to modern languages like Java or C#.
- **COBOL Modernization Reference**: Serves as a detailed example for the COBOL modernization community, addressing the scarcity of publicly available, complex COBOL projects.
- **LLM Development and Testing**: Aids in the development and fine-tuning of LLM translation tools by offering realistic challenges beyond simplistic code snippets.

## Key Features

- **Complex Business Logic**: Implements full transaction processing, portfolio management, and financial calculations.
- **Comprehensive System Architecture**: Includes batch processing, online transaction processing, reporting, utilities, and testing components.
- **Production-Grade Standards**: Follows industry best practices for error handling, data validation, security, and performance optimization.
- **Mainframe Environment Simulation**: Emulates key features of mainframe environments, exercising COBOL and z/OS functionalities.

## High-Level Description

The system simulates an investment portfolio management application that:

- Manages portfolios and transaction histories.
- Processes financial transactions and updates positions.
- Generates reports on positions, audits, and system statistics.
- Supports online inquiries for portfolio positions and transaction histories.

While it is not intended for actual deployment, it mirrors the complexity and structure of real-world COBOL applications found in mainframe environments.

## Development Approach

- **LLM Pair Programming**: Developed collaboratively with Anthropic Claude 3.5 Sonnet, turning requirements into design and code.
- **Five Sprints Execution**: Completed over five development sprints, each focusing on different system components.
- **Best Practices Implementation**: Adheres to standard mainframe development patterns and COBOL coding standards.
- **Comprehensive Documentation**: Includes detailed documentation to aid understanding and facilitate translation efforts.

## Repository Structure

This repository is organized to support both COBOL and Java implementations side-by-side:

```
project-root/
├── cobol/                  # Original Enterprise COBOL implementation
│   ├── documentation/      # COBOL system documentation
│   │   ├── technical/      # Technical documentation and architecture
│   │   ├── operations/     # Operational guides and specifications
│   │   ├── user/           # User documentation and guides
│   │   └── assets/         # Shared documentation assets
│   │
│   └── src/                # COBOL source code root
│       ├── programs/       # COBOL source programs
│       │   ├── batch/      # Batch processing programs
│       │   ├── online/     # Online transaction programs
│       │   ├── utility/    # Utility programs
│       │   └── test/       # Test programs
│       │
│       ├── copybook/       # COBOL copybooks (data definitions)
│       ├── database/       # Database definitions (VSAM and DB2)
│       ├── jcl/            # Job Control Language scripts
│       ├── maps/           # BMS screen definitions
│       ├── cics/           # CICS resource definitions
│       └── templates/      # Code templates and standards
│
├── java/                   # Java implementation (Spring Boot)
│   ├── common/             # Domain models and shared utilities
│   ├── data-access/        # Repository layer for database access
│   ├── batch-processing/   # Spring Batch jobs
│   ├── online-services/    # Spring Boot REST APIs
│   ├── reporting/          # Reporting engine
│   ├── utilities/          # Maintenance tools
│   └── pom.xml             # Parent Maven configuration
│
└── shared/                 # Cross-version resources
    ├── test-data/          # Test data for both implementations
    ├── migration-docs/     # COBOL-to-Java mapping documentation
    └── comparison-reports/ # Validation reports comparing outputs
```

## System Architecture Overview

The system comprises several key components:

- **Batch Processing Layer**: Handles transaction validation (`TRNVAL00`), position updates (`POSUPD00`), and history loading (`HISTLD00`).
- **Online Layer**: Provides inquiry capabilities through `INQONLN`, allowing users to view portfolio positions and transaction histories.
- **Utility Programs**: Include file maintenance (`UTLMNT00`), system monitoring (`UTLMON00`), and data validation (`UTLVAL00`).
- **Reporting System**: Generates reports on positions (`RPTPOS00`), audits (`RPTAUD00`), and system statistics (`RPTSTA00`).
- **Test Components**: Consist of test data generation (`TSTGEN00`) and test validation (`TSTVAL00`) to support testing and benchmarking.

## Use in LLM Translation Tools Benchmarking

- **Complexity**: Offers a complex codebase that presents realistic challenges for LLMs, unlike simplistic examples.
- **Variety of Features**: Covers a wide range of COBOL and mainframe features, including VSAM files, DB2 database interactions, CICS transactions, and error handling mechanisms.
- **Benchmark Creation**: Serves as a foundation for creating benchmark translations and training pairs for developing and fine-tuning LLM translation tools.

## Development Status

### COBOL Implementation (Complete)

- ✅ Core Batch Processing Programs
- ✅ Online Inquiry System
- ✅ Utility Programs
- ✅ Reporting System
- ✅ Test Components
- ✅ System Monitoring Tools
- ✅ Security Framework

### Java Implementation (In Progress)

**Sprint 0 - Foundation & Repository Setup** ✅
- ✅ Multi-module Maven project structure
- ✅ Spring Boot 3.x configuration
- ✅ Base exception hierarchy
- ✅ CI/CD pipeline (GitHub Actions)
- ✅ Testing framework setup (JUnit 5, Mockito, TestContainers)

**Upcoming Sprints**
- 📋 Sprint 1: Core domain models and data access layer
- 📋 Sprint 2: Batch processing implementation
- 📋 Sprint 3: Online services (REST APIs)
- 📋 Sprint 4: Reporting engine
- 📋 Sprint 5: Integration testing and validation

## Getting Started

### Building the Java Application

The Java implementation uses Maven as the build tool and requires Java 17.

**Prerequisites:**
- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.8+ (or use the Maven wrapper included)
- Git

**Build Instructions:**

```bash
# Clone the repository
git clone https://github.com/COG-GTM/COBOL-Legacy-Benchmark-Suite.git
cd COBOL-Legacy-Benchmark-Suite

# Navigate to the Java directory
cd java

# Build all modules
mvn clean install

# Run tests
mvn test

# Build without running tests
mvn clean install -DskipTests
```

**Running Individual Modules:**

```bash
# Run the online services (REST API)
cd online-services
mvn spring-boot:run

# The API will be available at http://localhost:8080/api
# H2 console available at http://localhost:8080/api/h2-console

# Run batch processing jobs
cd batch-processing
mvn spring-boot:run
```

### COBOL Implementation

The COBOL implementation requires a z/OS or compatible mainframe environment. See the [COBOL Operations Guide](cobol/documentation/operations/README.md) for detailed setup instructions.

## Documentation

Comprehensive documentation is provided to facilitate understanding and translation:

**COBOL Documentation:**
- [System Architecture Document (SAD)](cobol/documentation/technical/system-architecture.md)
- [Data Dictionary](cobol/documentation/technical/data-dictionary.md)
- [Operations Guide](cobol/documentation/operations/README.md)
- [Test Data Specifications](cobol/documentation/operations/test-data-specs.md)

**Java Documentation:**
- Java API documentation (generated via Javadoc)
- Module-specific README files (coming in future sprints)

## Technical Standards

- **COBOL Standards**: Enterprise COBOL for z/OS compliance.
- **Mainframe Patterns**: Implements standard file handling, batch processing, and error handling patterns.
- **Database Integration**: Follows DB2 for z/OS SQL standards.
- **Online Processing**: Incorporates CICS transaction processing standards.
- **Security**: Adheres to mainframe security best practices.

## Quality Standards

- **Production-Grade Code**: Written to production standards suitable for z/OS deployment.
- **Error Handling**: Includes comprehensive error handling and recovery procedures.
- **Documentation**: Provides detailed documentation for all components.
- **Testing**: Contains test programs and data for validation purposes.

## Future Plans

- **LLM Translation Training Pairs**: Create benchmark translations to modern languages like Java or C#.
- **Fine-Tuning LLMs**: Use the project to enhance the capabilities of LLM translation tools in handling complex legacy codebases.
- **Community Contribution**: Encourage collaboration within the COBOL modernization community to improve and expand the benchmark.

## License

[Appropriate License]

## Contributing

Contributions are welcome, especially from those interested in COBOL modernization and LLM development. Please see the [Contributing Guidelines](CONTRIBUTING.md) for more information.
