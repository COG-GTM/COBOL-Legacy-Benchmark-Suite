# COBOL to Java Migration Demo

This directory contains a demonstration of migrating the COBOL `HISTLD00` batch program to Java, showcasing the translation patterns and challenges involved in COBOL modernization.

## Overview

The demo translates the **Position History DB2 Load Program** (`HISTLD00.cbl`) from COBOL to Java, preserving the original program structure while applying modern Java idioms.

## Directory Structure

```
src/demo/
└── java/
    └── com/clbs/
        ├── batch/
        │   └── HistoryLoadProcessor.java    # Main batch processor (HISTLD00)
        ├── model/
        │   ├── PositionHistoryRecord.java   # DB2 table record (DBTBLS.cpy)
        │   ├── TransactionHistoryRecord.java # VSAM input record (HISTREC.cpy)
        │   └── BatchControlRecord.java      # Checkpoint control (BCHCTL.cpy)
        ├── repository/
        │   └── PositionHistoryRepository.java # DB2 operations
        ├── exception/
        │   └── BatchProcessingException.java  # Error handling (ERRHAND.cpy)
        └── config/
```

## Migration Mapping

### COBOL to Java Component Mapping

| COBOL Component | Java Equivalent | Notes |
|-----------------|-----------------|-------|
| `HISTLD00.cbl` | `HistoryLoadProcessor.java` | Main batch program |
| `DBTBLS.cpy` (POSHIST-RECORD) | `PositionHistoryRecord.java` | DB2 table structure |
| `HISTREC.cpy` | `TransactionHistoryRecord.java` | VSAM input file |
| `BCHCTL.cpy` | `BatchControlRecord.java` | Checkpoint/restart |
| `ERRHAND.cpy` | `BatchProcessingException.java` | Error handling |
| Embedded SQL | `PositionHistoryRepository.java` | JDBC operations |

### Data Type Mapping

| COBOL PIC Clause | Java Type | Example |
|------------------|-----------|---------|
| `PIC X(n)` | `String` | `PIC X(8)` → `String` (8 chars) |
| `PIC S9(n)V9(m) COMP-3` | `BigDecimal` | `PIC S9(13)V9(2)` → `BigDecimal` |
| `PIC S9(n) COMP` | `int` / `long` | `PIC S9(9) COMP` → `long` |
| `PIC 9(n)` | `int` | `PIC 9(4)` → `int` |
| 88-level conditions | `enum` | Status values → `Status` enum |

### Control Flow Mapping

| COBOL Pattern | Java Equivalent |
|---------------|-----------------|
| `PERFORM paragraph` | Method call |
| `PERFORM...UNTIL` | `while` loop |
| `EVALUATE...WHEN` | `switch` statement |
| `IF...ELSE...END-IF` | `if...else` |
| `GOBACK` with `RETURN-CODE` | `return int` |

## Key Migration Patterns Demonstrated

### 1. Paragraph to Method Translation

**COBOL:**
```cobol
0000-MAIN.
    PERFORM 1000-INITIALIZE
    PERFORM 2000-PROCESS UNTIL END-OF-FILE
    PERFORM 3000-TERMINATE
    GOBACK.
```

**Java:**
```java
public int execute(Iterator<TransactionHistoryRecord> input) {
    initialize();
    while (!endOfFile && errorCount <= MAX_ERRORS) {
        process();
    }
    terminate();
    return (int) errorCount;
}
```

### 2. Embedded SQL to JDBC

**COBOL:**
```cobol
EXEC SQL
    INSERT INTO POSHIST VALUES (:POSHIST-RECORD)
END-EXEC

IF SQLCODE = 0
    ADD 1 TO WS-RECORDS-WRITTEN
ELSE
    IF SQLCODE = -803
        CONTINUE
    ELSE
        PERFORM DB2-ERROR-ROUTINE
    END-IF
END-IF
```

**Java:**
```java
try (PreparedStatement stmt = connection.prepareStatement(INSERT_SQL)) {
    stmt.setString(1, record.getAccountNo());
    // ... set parameters
    stmt.executeUpdate();
    return InsertResult.SUCCESS;
} catch (SQLException e) {
    if (e.getErrorCode() == -803) {
        return InsertResult.DUPLICATE_KEY;
    }
    throw new BatchProcessingException(...);
}
```

### 3. Copybook to Class Translation

**COBOL Copybook (DBTBLS.cpy):**
```cobol
01  POSHIST-RECORD.
    05  PH-ACCOUNT-NO        PIC X(8).
    05  PH-QUANTITY          PIC S9(12)V9(3) COMP-3.
    05  PH-TRANS-TYPE        PIC X(2).
```

**Java Class:**
```java
public class PositionHistoryRecord {
    private String accountNo;           // PIC X(8)
    private BigDecimal quantity;        // PIC S9(12)V9(3) COMP-3
    private String transType;           // PIC X(2)
    
    // Getters/setters with length validation
}
```

### 4. 88-Level Conditions to Enums

**COBOL:**
```cobol
05  BCT-STATUS        PIC X(1).
    88  BCT-STATUS-READY    VALUE 'R'.
    88  BCT-STATUS-ACTIVE   VALUE 'A'.
    88  BCT-STATUS-DONE     VALUE 'D'.
    88  BCT-STATUS-ERROR    VALUE 'E'.
```

**Java:**
```java
public enum Status {
    READY('R'),
    ACTIVE('A'),
    DONE('D'),
    ERROR('E');
    
    private final char code;
    // Constructor and methods
}
```

### 5. Checkpoint/Restart Pattern

**COBOL:**
```cobol
2300-CHECK-COMMIT.
    ADD 1 TO WS-COMMIT-COUNT
    IF WS-COMMIT-COUNT >= WS-COMMIT-THRESHOLD
        EXEC SQL COMMIT WORK END-EXEC
        PERFORM 2310-UPDATE-CHECKPOINT
    END-IF.
```

**Java:**
```java
private void checkCommit() throws BatchProcessingException {
    commitCount++;
    if (commitCount >= COMMIT_THRESHOLD) {
        repository.commit();
        commitCount = 0;
        updateCheckpoint();
    }
}
```

## Running the Demo on Your Mac

### Prerequisites
- Java 17 or higher (`java -version`)
- Maven 3.6+ (`mvn -version`)

Install if needed:
```bash
# macOS with Homebrew
brew install openjdk@17
brew install maven

# Add Java to PATH (if needed)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
```

### Quick Start

```bash
# Navigate to the demo directory
cd src/demo/java

# Build the executable JAR
mvn clean package

# Run with default 100 records
java -jar target/cobol-migration-demo-1.0.0.jar

# Or specify record count (e.g., 1000 records)
java -jar target/cobol-migration-demo-1.0.0.jar 1000
```

### What the Demo Does

1. **Creates an H2 in-memory database** (simulating DB2 on mainframe)
2. **Generates sample transaction records** (simulating VSAM input file)
3. **Runs the batch processor** (Java translation of HISTLD00.cbl)
4. **Displays results** (records loaded, statistics by portfolio)

### Expected Output

```
╔════════════════════════════════════════════════════════════════╗
║     COBOL to Java Migration Demo - HISTLD00 Batch Program      ║
║     Position History DB2 Load Program                          ║
╚════════════════════════════════════════════════════════════════╝

┌────────────────────────────────────────────────────────────────┐
│ PHASE 1: Generating Test Data (simulating VSAM input file)    │
└────────────────────────────────────────────────────────────────┘
  Generated 100 transaction history records

┌────────────────────────────────────────────────────────────────┐
│ PHASE 2: Running HISTLD00 Batch Process                       │
│ (This is the Java translation of the COBOL program)           │
└────────────────────────────────────────────────────────────────┘

HISTLD00 Processing Statistics:
  Records Read:    100
  Records Written: 100
  Errors:          0

┌────────────────────────────────────────────────────────────────┐
│ PHASE 3: Verifying Results (querying DB2/H2 database)         │
└────────────────────────────────────────────────────────────────┘
  Total records in POSHIST table: 100
  ...
```

## Migration Challenges Highlighted

1. **Packed Decimal Precision**: COMP-3 fields require `BigDecimal` to maintain precision
2. **Fixed-Length Strings**: COBOL PIC X fields need length validation/padding
3. **File Status Codes**: VSAM status codes map to specific exceptions
4. **SQLCODE Handling**: DB2 error codes require vendor-specific handling
5. **Checkpoint/Restart**: Batch recovery patterns need explicit implementation
6. **88-Level Conditions**: Condition names require enum or boolean methods

## Related Documentation

- [COBOL Migration Demo Use Case](../../documentation/demo/COBOL-Migration-Demo-Use-Case.md)
- [System Architecture](../../documentation/technical/system-architecture.md)
- [Original COBOL Source](../programs/batch/HISTLD00.cbl)
