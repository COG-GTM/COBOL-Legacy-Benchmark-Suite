# Portfolio Inquiry API — Java Modernization Example

This directory contains a working Spring Boot REST API that is a direct conversion of the CICS online inquiry program [`INQONLN.cbl`](../src/programs/online/INQONLN.cbl) from the COBOL Legacy Benchmark Suite.

It serves as a **concrete, buildable example** that other COBOL programs in this repository can follow when being modernized to Java.

## What Was Converted

| COBOL Artifact | Java Equivalent |
|---|---|
| `INQONLN.cbl` (CICS pseudo-conversational program) | `InquiryController.java` (REST controller) |
| `PORTFLIO.cpy` (copybook) | `Portfolio.java` + `PortfolioKey.java` (JPA entities) |
| `TRNREC.cpy` (copybook) | `TransactionRecord.java` (JPA entity) |
| `POSHIST.sql` (DB2 table) | `PositionHistory.java` (JPA entity) |
| `ERRHAND.cpy` + `ERRHNDL.cbl` (error handling) | `GlobalExceptionHandler.java` + exception classes |
| `INQPORT.cbl` / `INQHIST.cbl` (sub-programs) | `PortfolioInquiryService.java` (service layer) |
| BMS maps (`INQSET`) | JSON DTOs (Jackson serialization) |
| VSAM KSDS file access | Spring Data JPA repositories |
| DB2 cursors | Spring Data JPA derived query methods |
| 88-level conditions | Java enums with JPA `AttributeConverter` |

## Prerequisites

- **Java 17** or later
- **Maven 3.8+**

## Build

```bash
cd java-modernization-example
mvn clean package
```

## Run

```bash
mvn spring-boot:run
```

The API starts on **http://localhost:8080**.

An H2 in-memory database is pre-loaded with sample data so the API can be tested immediately.

## API Endpoints

These endpoints map directly to the `EVALUATE` branches in `INQONLN.cbl` (lines 62-77):

| COBOL Function | Endpoint | Description |
|---|---|---|
| `MENU` (P200-DISPLAY-MENU) | `GET /api/inquiry/menu` | List available inquiry options |
| `INQP` (P300-PORTFOLIO-INQUIRY) | `GET /api/inquiry/portfolio/{portfolioId}` | Look up a portfolio by ID |
| `INQH` (P400-HISTORY-INQUIRY) | `GET /api/inquiry/history/{portfolioId}?from=yyyy-MM-dd&to=yyyy-MM-dd` | Query position history for a date range |
| `EXIT` | _(no endpoint — client simply stops calling)_ | Session termination |

### Example Requests

```bash
# Menu (replaces CICS SEND MAP 'INQMNU')
curl http://localhost:8080/api/inquiry/menu

# Portfolio inquiry (replaces CICS LINK PROGRAM('INQPORT'))
curl http://localhost:8080/api/inquiry/portfolio/PORT0001

# History inquiry (replaces CICS LINK PROGRAM('INQHIST'))
curl "http://localhost:8080/api/inquiry/history/PORT0001?from=2024-03-01&to=2024-03-31"

# Non-existent portfolio (returns 404, mirrors VSAM status 23)
curl http://localhost:8080/api/inquiry/portfolio/NOTEXIST
```

### H2 Console

For development, the H2 database console is available at:

```
http://localhost:8080/h2-console
```

- JDBC URL: `jdbc:h2:mem:portfoliodb`
- Username: `sa`
- Password: _(empty)_

## Run Tests

```bash
mvn test
```

## Project Structure

```
java-modernization-example/
├── pom.xml
├── README.md
├── COBOL-TO-JAVA-MAPPING.md          # Detailed mapping reference guide
└── src/
    ├── main/
    │   ├── java/com/portfolio/
    │   │   ├── InquiryApplication.java         # @SpringBootApplication
    │   │   ├── controller/
    │   │   │   └── InquiryController.java      # REST API (replaces INQONLN.cbl)
    │   │   ├── service/
    │   │   │   └── PortfolioInquiryService.java # Business logic
    │   │   ├── model/
    │   │   │   ├── Portfolio.java               # PORTFLIO.cpy → JPA entity
    │   │   │   ├── PortfolioKey.java            # Composite key
    │   │   │   ├── TransactionRecord.java       # TRNREC.cpy → JPA entity
    │   │   │   ├── TransactionRecordKey.java    # Composite key
    │   │   │   ├── PositionHistory.java         # POSHIST table → JPA entity
    │   │   │   ├── PositionHistoryKey.java      # Composite key
    │   │   │   └── enums/
    │   │   │       ├── ClientType.java          # 88-level: I/C/T
    │   │   │       ├── PortfolioStatus.java     # 88-level: A/C/S
    │   │   │       ├── TransactionType.java     # 88-level: BU/SL/TR/FE
    │   │   │       └── TransactionStatus.java   # 88-level: P/D/F/R
    │   │   ├── repository/
    │   │   │   ├── PortfolioRepository.java
    │   │   │   ├── TransactionRepository.java
    │   │   │   └── PositionHistoryRepository.java
    │   │   ├── exception/
    │   │   │   ├── PortfolioException.java      # Base exception
    │   │   │   ├── PortfolioNotFoundException.java  # VSAM status 23
    │   │   │   ├── GlobalExceptionHandler.java  # @ControllerAdvice
    │   │   │   └── ErrorResponse.java           # ERR-MESSAGE structure
    │   │   └── dto/
    │   │       ├── PortfolioDto.java
    │   │       ├── PositionHistoryDto.java
    │   │       └── MenuResponse.java
    │   └── resources/
    │       ├── application.yml
    │       └── data.sql                         # Sample seed data
    └── test/
        └── java/com/portfolio/controller/
            └── InquiryControllerTest.java       # @WebMvcTest tests
```

## Mapping Guide

See [`COBOL-TO-JAVA-MAPPING.md`](COBOL-TO-JAVA-MAPPING.md) for a comprehensive reference on converting the remaining COBOL programs using the patterns established here.
