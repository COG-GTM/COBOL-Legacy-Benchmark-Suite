# TRNVAL00 - Java 17 Conversion

## Overview

This directory contains the Java 17 conversion of the COBOL TRNVAL00 (Transaction Validation) batch program from the COBOL Legacy Benchmark Suite Investment Portfolio Management System.

## Purpose

TRNVAL00 is the Transaction Validation batch program that validates incoming financial transactions before they are processed by the Position Update (POSUPD00) program. This Java conversion maintains the same validation logic and business rules as the original COBOL specification.

## COBOL to Java Mapping

### Program Structure

| COBOL Component | Java Equivalent | Description |
|----------------|-----------------|-------------|
| PROGRAM-ID TRNVAL00 | `TRNVAL00.java` | Main program class |
| COPY TRNREC | `TransactionRecord.java` | Transaction data model |
| COPY POSREC | `PositionRecord.java` | Position data model |
| COPY ERRHAND | `ValidationError.java`, `ErrorReporter.java` | Error handling |
| COPY RTNCODE | Return code logic in `TRNVAL00.java` | Return code management |
| FILE SECTION | `TransactionFileProcessor.java` | File I/O operations |
| WORKING-STORAGE | Instance variables in classes | Working storage |
| PROCEDURE DIVISION | Methods in `TRNVAL00.java` | Program logic |

### COBOL Paragraphs to Java Methods

| COBOL Paragraph | Java Method | Class |
|----------------|-------------|-------|
| 0000-MAIN | `execute()` | TRNVAL00 |
| 1000-INITIALIZE | `initialize()` | TRNVAL00 |
| 2000-PROCESS | `process()` | TRNVAL00 |
| 3000-CLEANUP | `cleanup()` | TRNVAL00 |
| Validation logic | `validate()` | TransactionValidator |
| Error handling | `addError()`, `writeFooter()` | ErrorReporter |

### Data Type Conversions

| COBOL Type | Java Type | Notes |
|-----------|-----------|-------|
| PIC X(n) | String | Character fields |
| PIC 9(n) | String or int | Numeric fields (kept as String for IDs) |
| PIC S9(n)V9(m) COMP-3 | BigDecimal | Packed decimal for amounts |
| 88 level conditions | enum | Type-safe enumerations |
| FILE STATUS | IOException | Exception handling |

### Validation Rules Implementation

All validation rules from the COBOL specification (data-dictionary.md) are implemented in `TransactionValidator.java`:

1. **Portfolio ID Validation**
   - COBOL: `PIC X(08)` with alphanumeric check
   - Java: Pattern matching `^[A-Z0-9]{8}$`
   - Error Code: E001

2. **Investment ID Validation**
   - COBOL: `PIC X(10)` with alphanumeric check
   - Java: Pattern matching `^[A-Z0-9]{10}$`
   - Error Code: E002

3. **Transaction Type Validation**
   - COBOL: 88-level conditions (TR-BUY, TR-SELL, TR-TRANS, TR-FEE)
   - Java: Enum `TransactionType` with values BUY, SELL, TRANSFER, FEE
   - Error Code: E003

4. **Date Validation**
   - COBOL: `PIC 9(08)` with date range check
   - Java: `LocalDate.parse()` with future date check
   - Error Code: E005

5. **Amount Range Checks**
   - COBOL: `PIC S9(11)V99` range validation
   - Java: BigDecimal comparison with MIN/MAX constants
   - Error Codes: E008, E009, E010

6. **Business Rules**
   - Share Quantity must not be zero for BUY/SELL (E013)
   - Price must be greater than zero for BUY/SELL (E014)
   - Amount must be non-zero for FEE (E015)
   - Zero Dollar Transaction warning (W001)
   - Duplicate Transaction ID warning (W002)

### Return Codes

| Code | COBOL Meaning | Java Implementation |
|------|---------------|---------------------|
| 0 | Successful completion | No errors or warnings |
| 4 | Warning, processing complete | Warnings found, no errors |
| 8 | Errors, processing complete | Errors found |
| 12 | Critical error, abend | IOException or critical failure |
| 16 | Environment error | Unexpected exception |

## Package Structure

```
com.portfolio.batch.trnval/
├── TRNVAL00.java                    # Main program
├── model/
│   ├── TransactionRecord.java       # Transaction data model (TRNREC)
│   └── PositionRecord.java          # Position data model (POSREC)
├── validation/
│   └── TransactionValidator.java    # Validation logic
├── processor/
│   └── TransactionFileProcessor.java # File I/O operations
├── error/
│   ├── ValidationError.java         # Error data structure
│   └── ErrorReporter.java           # Error reporting
└── util/
    (reserved for future utilities)
```

## Compilation

To compile the Java program:

```bash
cd src/java
javac -d ../../build/classes com/portfolio/batch/trnval/**/*.java
```

Or with a module path:

```bash
javac --release 17 -d ../../build/classes com/portfolio/batch/trnval/**/*.java
```

## Execution

### Command Line

```bash
java -cp ../../build/classes com.portfolio.batch.trnval.TRNVAL00 \
    <input-file> <output-file> <error-report>
```

### Example

```bash
java -cp ../../build/classes com.portfolio.batch.trnval.TRNVAL00 \
    transactions.dat valid_transactions.dat error_report.txt
```

## Input File Format

Fixed-format records (89 characters minimum):

| Positions | Field | COBOL Definition | Java Type |
|-----------|-------|------------------|-----------|
| 1-8 | Transaction Date | PIC X(08) | String (YYYYMMDD) |
| 9-14 | Transaction Time | PIC X(06) | String (HHMMSS) |
| 15-22 | Portfolio ID | PIC X(08) | String |
| 23-28 | Sequence Number | PIC X(06) | String |
| 29-38 | Investment ID | PIC X(10) | String |
| 39-40 | Transaction Type | PIC X(02) | TransactionType enum |
| 41-55 | Quantity | PIC S9(11)V9(4) | BigDecimal |
| 56-70 | Price | PIC S9(11)V9(4) | BigDecimal |
| 71-85 | Amount | PIC S9(13)V9(2) | BigDecimal |
| 86-88 | Currency | PIC X(03) | String |
| 89 | Status | PIC X(01) | TransactionStatus enum |

### Sample Input Record

```
20241120093000PORT0001000001INVEST0001BU         100.0000          50.5000         5050.00USDP
```

This represents:
- Date: 2024-11-20
- Time: 09:30:00
- Portfolio: PORT0001
- Sequence: 000001
- Investment: INVEST0001
- Type: BUY
- Quantity: 100.0000
- Price: 50.5000
- Amount: 5050.00
- Currency: USD
- Status: Pending

## Output Files

### Valid Transactions File

Contains all transactions that passed validation (or had only warnings). Same format as input file.

### Error Report File

Text report containing:
- Header with program information and timestamp
- Detailed error/warning messages for each validation failure
- Summary statistics (total records, valid records, error count, warning count)

Example error report format:

```
================================================================================
                    TRANSACTION VALIDATION ERROR REPORT
                           TRNVAL00 - Java Version
================================================================================
Report Generated: 2024-11-20 09:30:00
================================================================================

EE001    ERROR  Line 000005: PORTFOLIO-ID    PORT001         Invalid Portfolio ID format (must be 8 alphanumeric characters)
EE003    ERROR  Line 000012: TRANSACTION-TYPE XX              Invalid Transaction Type (must be BU, SL, TR, or FE)
WW001    WARNING Line 000023: AMOUNT          0.00            Zero Dollar Transaction

================================================================================
                           VALIDATION SUMMARY
================================================================================
Total Records Processed:           1,000
Valid Records:                       987
Records with Errors:                  10
Records with Warnings:                 3
================================================================================
```

## Key Differences from COBOL

1. **Object-Oriented Design**: Java implementation uses OOP principles with separate classes for concerns
2. **Type Safety**: Java enums replace COBOL 88-level conditions for type safety
3. **Exception Handling**: Java exceptions replace COBOL file status codes
4. **BigDecimal**: Used for precise decimal arithmetic (COBOL COMP-3 equivalent)
5. **Collections**: Java Lists replace COBOL tables/arrays
6. **String Processing**: Java String methods replace COBOL string manipulation

## Advantages of Java Version

1. **Portability**: Runs on any platform with JVM (no mainframe required)
2. **Maintainability**: Object-oriented design is easier to understand and modify
3. **Testing**: Easier to unit test individual components
4. **Integration**: Can integrate with modern Java frameworks and tools
5. **Performance**: JVM optimization can provide good performance
6. **Tooling**: Modern IDEs provide better development experience

## Limitations

1. **File Format**: Currently supports text files, not VSAM
2. **Checkpoint/Restart**: Not implemented (would require additional framework)
3. **DB2 Integration**: Not included in this conversion
4. **JCL**: No equivalent (use shell scripts or job schedulers)

## Future Enhancements

Potential improvements for production use:

1. Add checkpoint/restart capability for large files
2. Implement parallel processing for better performance
3. Add database integration (JDBC) for validation lookups
4. Create Spring Batch version for enterprise integration
5. Add metrics and monitoring (JMX, Micrometer)
6. Implement configuration file support
7. Add logging framework (SLF4J/Logback)
8. Create unit tests with JUnit 5
9. Add CSV and JSON input format support
10. Implement streaming processing for very large files

## Testing

To test the program, create sample input files following the format specification above. The program will:

1. Read all transactions from the input file
2. Validate each transaction according to business rules
3. Write valid transactions to the output file
4. Write detailed error report with all validation failures
5. Display summary statistics on console
6. Exit with appropriate return code

## References

- Original COBOL specification: `documentation/technical/system-architecture.md`
- Data dictionary: `documentation/technical/data-dictionary.md`
- COBOL copybooks: `src/copybook/common/TRNREC.cpy`, `POSREC.cpy`
- Error handling: `src/copybook/common/ERRHAND.cpy`
- Validation utility: `src/programs/utility/UTLVAL00.cbl`

## Author

Java 17 conversion from COBOL TRNVAL00 specification
COBOL Legacy Benchmark Suite - Investment Portfolio Management System

## Version History

- 1.0 (2024-11-20): Initial Java 17 conversion
  - Complete validation logic implementation
  - Sequential file processing
  - Error handling and reporting
  - Comprehensive documentation
