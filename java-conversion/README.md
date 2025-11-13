# COBOL to Java 17 Conversion - Common Utility Components

This directory contains Java 17 conversions of self-contained COBOL common utility components from the COBOL Legacy Benchmark Suite. The conversion focuses on maintaining the same functionality and business logic while adapting to Java idioms and best practices.

## Overview

This initial conversion phase includes three core components that serve as foundational building blocks for the portfolio management system:

1. **PORTVALD** - Portfolio Validation Subroutine
2. **ERRPROC** - Error Processing Subroutine
3. **AUDPROC** - Audit Trail Processing Subroutine

These components were selected for initial conversion due to their minimal dependencies, clear scope, and foundational nature that will support future conversions of more complex components.

## Project Structure

```
java-conversion/
├── pom.xml                                    # Maven project configuration
├── README.md                                  # This file
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── portfolio/
    │               ├── common/                # Common utility classes
    │               │   ├── AuditProcessor.java
    │               │   └── ErrorProcessor.java
    │               ├── model/                 # Data models and constants
    │               │   ├── AuditRequest.java
    │               │   ├── ErrorConstants.java
    │               │   ├── ErrorRequest.java
    │               │   ├── ValidationConstants.java
    │               │   ├── ValidationRequest.java
    │               │   └── ValidationResult.java
    │               └── validation/            # Validation logic
    │                   └── PortfolioValidator.java
    └── test/
        └── java/
            └── com/
                └── portfolio/
                    ├── common/
                    │   ├── AuditProcessorTest.java
                    │   └── ErrorProcessorTest.java
                    └── validation/
                        └── PortfolioValidatorTest.java
```

## Component Conversions

### 1. PORTVALD - Portfolio Validation

**Original COBOL:** `src/programs/portfolio/PORTVALD.cbl`  
**Java Implementation:** `com.portfolio.validation.PortfolioValidator`

The portfolio validation component validates portfolio data elements including:
- Portfolio IDs (must start with 'PORT' followed by 4 numeric digits)
- Account numbers (must be 10 numeric digits, not all zeros)
- Investment types (must be STK, BND, MMF, or ETF)
- Amounts (must be within valid range: -9,999,999,999,999.99 to +9,999,999,999,999.99)

**Design Decisions:**
- Used Java enums for validation types instead of COBOL level-88 conditions
- Implemented BigDecimal for precise decimal arithmetic matching COBOL PIC S9(13)V99
- Created separate model classes (ValidationRequest, ValidationResult) for type safety
- Used Java switch expressions (Java 17 feature) for cleaner validation routing

**Usage Example:**
```java
PortfolioValidator validator = new PortfolioValidator();

// Validate portfolio ID
ValidationRequest request = new ValidationRequest(
    ValidationRequest.ValidationType.ID, 
    "PORT1234"
);
ValidationResult result = validator.validate(request);

if (result.isSuccess()) {
    System.out.println("Validation passed");
} else {
    System.out.println("Error: " + result.getErrorMessage());
}
```

### 2. ERRPROC - Error Processing

**Original COBOL:** `src/programs/common/ERRPROC.cbl`  
**Java Implementation:** `com.portfolio.common.ErrorProcessor`

The error processing component handles error logging and display with:
- Timestamped error messages
- Categorized errors (VSAM, Validation, Processing, System)
- Severity levels (Success, Warning, Error, Severe, Terminal)
- Sequential file logging with formatted records
- Console display of error details

**Design Decisions:**
- Used Java NIO for file operations instead of COBOL sequential file I/O
- Implemented DateTimeFormatter for timestamp formatting matching COBOL TIMESTAMP
- Created ErrorConstants class for return codes and categories
- Used try-with-resources for automatic file handle management
- Maintained fixed-width formatting to match COBOL record structure

**Usage Example:**
```java
ErrorProcessor errorProcessor = new ErrorProcessor("error.log");

ErrorRequest request = new ErrorRequest(
    "TESTPROG",
    ErrorConstants.Categories.VALIDATION,
    "E001",
    ErrorConstants.ReturnCodes.ERROR,
    "Invalid portfolio ID",
    "Portfolio ID must start with PORT"
);

int returnCode = errorProcessor.processError(request);
```

### 3. AUDPROC - Audit Trail Processing

**Original COBOL:** `src/programs/common/AUDPROC.cbl`  
**Java Implementation:** `com.portfolio.common.AuditProcessor`

The audit trail component writes audit records for:
- Transaction auditing (CREATE, UPDATE, DELETE, INQUIRE)
- User actions (LOGIN, LOGOUT)
- System events (STARTUP, SHUTDOWN)
- Before/after images for data changes
- Status tracking (SUCCESS, FAILURE, WARNING)

**Design Decisions:**
- Used Java enums for audit types, actions, and statuses
- Implemented fixed-width record formatting matching COBOL structure
- Created AuditRequest model with strongly-typed fields
- Used Java NIO for file operations
- Maintained compatibility with COBOL record layout for potential interoperability

**Usage Example:**
```java
AuditProcessor auditProcessor = new AuditProcessor("audit.log");

AuditRequest request = new AuditRequest(
    "SYS001",                              // System ID
    "USER123",                             // User ID
    "PORTUPD",                             // Program
    "TERM001",                             // Terminal
    AuditRequest.AuditType.TRANSACTION,    // Type
    AuditRequest.AuditAction.UPDATE,       // Action
    AuditRequest.AuditStatus.SUCCESS,      // Status
    "PORT1234",                            // Portfolio ID
    "1234567890",                          // Account number
    "Old data",                            // Before image
    "New data",                            // After image
    "Portfolio updated"                    // Message
);

int returnCode = auditProcessor.processAudit(request);
```

## Building and Testing

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Build
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

All 26 unit tests should pass, covering:
- 17 tests for PortfolioValidator
- 4 tests for ErrorProcessor
- 5 tests for AuditProcessor

### Package
```bash
mvn package
```

## Conversion Approach

### Principles
1. **Functional Equivalence**: Maintain the same business logic and validation rules
2. **Type Safety**: Use Java's type system to prevent errors at compile time
3. **Modern Java**: Leverage Java 17 features (switch expressions, records consideration)
4. **Testability**: Design for easy unit testing with dependency injection
5. **Maintainability**: Clear separation of concerns with model, service, and utility layers

### COBOL to Java Mappings

| COBOL Construct | Java Equivalent |
|-----------------|-----------------|
| PICTURE X(n) | String with length validation |
| PICTURE 9(n) | String (for validation) or int/long |
| PICTURE S9(13)V99 | BigDecimal |
| Level-88 conditions | Enum values |
| EVALUATE TRUE | Switch expression |
| COPY copybook | Import class/constants |
| CALL subroutine | Method invocation |
| Sequential file I/O | Java NIO Files API |
| WORKING-STORAGE | Instance variables |
| LINKAGE SECTION | Method parameters |

### Differences from COBOL

1. **Memory Management**: Java handles garbage collection automatically
2. **String Handling**: Java strings are immutable and don't require explicit padding
3. **File I/O**: Java NIO provides more flexible file operations
4. **Error Handling**: Java exceptions vs COBOL return codes (both supported)
5. **Type System**: Java's strong typing catches errors at compile time
6. **Object-Oriented**: Java uses classes and objects vs COBOL's procedural approach

## Testing Strategy

The test suite covers:
- **Happy path scenarios**: Valid inputs producing expected outputs
- **Validation failures**: Invalid inputs triggering appropriate error messages
- **Edge cases**: Boundary values, null handling, empty strings
- **File operations**: Creating files, writing records, handling multiple operations
- **Return codes**: Verifying correct severity/status codes

## Future Enhancements

Potential improvements for production use:
1. **Logging Framework**: Replace System.out with SLF4J/Logback
2. **Configuration**: Externalize file paths and constants
3. **Thread Safety**: Add synchronization for concurrent access
4. **Performance**: Buffer writes for high-volume operations
5. **Monitoring**: Add metrics and health checks
6. **Integration**: Connect to enterprise logging/audit systems

## Next Steps for Conversion

Based on this foundation, the following components are recommended for the next conversion phase:

1. **PORTREAD** - Portfolio record reading (builds on validation)
2. **TSTGEN00** - Test data generator (uses error processing)
3. **More complex batch programs** - Once file I/O patterns are established

Components to avoid until later phases:
- Online programs requiring CICS infrastructure
- Programs with DB2 dependencies
- Complex batch programs with checkpoint/restart

## References

- Original COBOL source: `src/programs/`
- COBOL copybooks: `src/copybook/common/`
- Documentation: `documentation/`

## License

This conversion maintains the same license as the original COBOL Legacy Benchmark Suite.
