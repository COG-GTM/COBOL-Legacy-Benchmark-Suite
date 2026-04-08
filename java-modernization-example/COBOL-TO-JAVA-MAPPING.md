# COBOL-to-Java Mapping Guide

This document is a reference for converting the remaining COBOL programs in the COBOL Legacy Benchmark Suite to Java Spring Boot, using the patterns established in the `INQONLN.cbl` → `InquiryController.java` conversion.

---

## 1. CICS Commands → Spring Equivalents

| CICS Command | Purpose in COBOL | Java / Spring Equivalent | Example |
|---|---|---|---|
| `EXEC CICS RECEIVE MAP` | Read input from BMS terminal screen into COMMAREA | `@RequestParam`, `@PathVariable`, `@RequestBody` | `@PathVariable String portfolioId` replaces reading PORT-ID from the map |
| `EXEC CICS SEND MAP` | Render output to 3270 terminal screen | `ResponseEntity<T>` returning a DTO (Jackson → JSON) | `return ResponseEntity.ok(dto)` replaces SEND MAP |
| `EXEC CICS HANDLE CONDITION ERROR(...)` | Register global error handler for CICS conditions | `@ControllerAdvice` + `@ExceptionHandler` | `GlobalExceptionHandler.java` replaces P900-ERROR-ROUTINE |
| `EXEC CICS LINK PROGRAM(...)` | Call a sub-program passing COMMAREA | Service method call (dependency injection) | `inquiryService.getPortfolio(id)` replaces `LINK PROGRAM('INQPORT')` |
| `EXEC CICS RETURN` | End transaction, return control to CICS | Implicit — HTTP response is sent when method returns | Controller method `return` ends the request |
| `EXEC CICS RETURN TRANSID(...)` | Pseudo-conversational: schedule next transaction | Stateless REST — no equivalent needed | Client makes next HTTP request independently |
| `EXEC CICS ABEND ABCODE(...)` | Abnormal termination | Throw exception (caught by `@ControllerAdvice`) | `throw new PortfolioException(...)` |
| `EXEC CICS ASSIGN USERID(...)` | Get current user ID from CICS | Spring Security `SecurityContextHolder` or JWT claims | `Authentication.getName()` |
| COMMAREA (pseudo-conversational state) | Pass data between transaction cycles | Stateless REST (or JWT/session token if state needed) | Each request is independent |

### Source References
- CICS HANDLE CONDITION: [`INQONLN.cbl` lines 40-44](../src/programs/online/INQONLN.cbl)
- CICS RECEIVE MAP: [`INQONLN.cbl` lines 56-60](../src/programs/online/INQONLN.cbl)
- CICS LINK PROGRAM: [`INQONLN.cbl` lines 102-106](../src/programs/online/INQONLN.cbl)
- CICS SEND MAP: [`INQONLN.cbl` lines 93-97](../src/programs/online/INQONLN.cbl)

---

## 2. Copybook → JPA Entity Mapping Rules

### General Pattern

| COBOL Construct | Java Equivalent |
|---|---|
| `01 RECORD-NAME.` (top-level record) | `@Entity` class |
| `05 GROUP-NAME.` (group item) | Embedded fields or `@Embeddable` class |
| `10 FIELD-NAME PIC X(n).` | `String` field with `@Column(length = n)` |
| `10 FIELD-NAME PIC 9(n).` (date) | `LocalDate` or `LocalTime` |
| `10 FIELD-NAME PIC S9(n)V9(m) COMP-3.` | `BigDecimal` with `@Column(precision = n+m, scale = m)` |
| `FILLER PIC X(n).` | Omitted (no Java field needed) |
| Record key fields | `@EmbeddedId` with `@Embeddable` key class, or `@IdClass` |

### Source References
- Portfolio copybook: [`PORTFLIO.cpy`](../src/copybook/common/PORTFLIO.cpy) → [`Portfolio.java`](src/main/java/com/portfolio/model/Portfolio.java)
- Transaction copybook: [`TRNREC.cpy`](../src/copybook/common/TRNREC.cpy) → [`TransactionRecord.java`](src/main/java/com/portfolio/model/TransactionRecord.java)
- DB2 table: [`POSHIST.sql`](../src/database/db2/POSHIST.sql) → [`PositionHistory.java`](src/main/java/com/portfolio/model/PositionHistory.java)

### Composite Key Pattern

COBOL records often have multi-field keys. Map these to either `@EmbeddedId` or `@IdClass`:

```cobol
       05  PORT-KEY.
           10  PORT-ID             PIC X(8).
           10  PORT-ACCOUNT-NO     PIC X(10).
```

```java
@Embeddable
public class PortfolioKey implements Serializable {
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "account_no", length = 10)
    private String accountNo;

    // equals() and hashCode() required
}

@Entity
public class Portfolio {
    @EmbeddedId
    private PortfolioKey key;
    // ...
}
```

---

## 3. 88-Level Conditions → Java Enum Pattern

COBOL 88-level conditions define valid values for a field. Map these to Java enums with a JPA `AttributeConverter` so the original COBOL codes are stored in the database.

### COBOL

```cobol
       10  PORT-CLIENT-TYPE    PIC X(1).
           88  PORT-INDIVIDUAL    VALUE 'I'.
           88  PORT-CORPORATE     VALUE 'C'.
           88  PORT-TRUST         VALUE 'T'.
```

### Java

```java
public enum ClientType {
    INDIVIDUAL('I'), CORPORATE('C'), TRUST('T');

    private final char code;
    ClientType(char code) { this.code = code; }
    public char getCode() { return code; }

    public static ClientType fromCode(char code) {
        for (ClientType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }

    @Converter(autoApply = true)
    public static class ClientTypeConverter
            implements AttributeConverter<ClientType, String> {
        @Override
        public String convertToDatabaseColumn(ClientType attr) {
            return attr == null ? null : String.valueOf(attr.getCode());
        }
        @Override
        public ClientType convertToEntityAttribute(String db) {
            return db == null || db.isBlank() ? null : ClientType.fromCode(db.charAt(0));
        }
    }
}
```

### All Enum Mappings in This Project

| COBOL Field | Copybook | Values | Java Enum |
|---|---|---|---|
| `PORT-CLIENT-TYPE` | `PORTFLIO.cpy` | I, C, T | `ClientType` |
| `PORT-STATUS` | `PORTFLIO.cpy` | A, C, S | `PortfolioStatus` |
| `TRN-TYPE` | `TRNREC.cpy` | BU, SL, TR, FE | `TransactionType` |
| `TRN-STATUS` | `TRNREC.cpy` | P, D, F, R | `TransactionStatus` |

---

## 4. COMP-3 Fields → BigDecimal Rules

COBOL packed-decimal (COMP-3) fields **must always** be mapped to `java.math.BigDecimal`. Never use `float` or `double` for financial data.

| COBOL PIC | Java Type | JPA Annotation |
|---|---|---|
| `PIC S9(13)V99 COMP-3` | `BigDecimal` | `@Column(precision = 15, scale = 2)` |
| `PIC S9(11)V9(4) COMP-3` | `BigDecimal` | `@Column(precision = 15, scale = 4)` |
| `PIC S9(7)V99 COMP-3` | `BigDecimal` | `@Column(precision = 9, scale = 2)` |

**Rule:** `precision = integer_digits + decimal_digits`, `scale = decimal_digits`.

Set scale explicitly when creating BigDecimal values:

```java
new BigDecimal("17550.00").setScale(2, RoundingMode.HALF_UP);
```

---

## 5. VSAM Status Codes → HTTP Status Codes

| VSAM Status | COBOL Constant | Meaning | HTTP Status | Java Exception |
|---|---|---|---|---|
| `00` | `ERR-VSAM-SUCCESS` | Successful operation | `200 OK` | (none) |
| `10` | `ERR-VSAM-EOF` | End of file | `200 OK` (empty list) | (none) |
| `22` | `ERR-VSAM-DUPKEY` | Duplicate record key | `409 Conflict` | `DuplicateKeyException` (custom) |
| `23` | `ERR-VSAM-NOTFND` | Record not found | `404 Not Found` | `PortfolioNotFoundException` |

### Source Reference
- VSAM status definitions: [`ERRHAND.cpy` lines 44-48](../src/copybook/common/ERRHAND.cpy)

---

## 6. Return Code Framework → Exception Hierarchy

The COBOL error handling framework (ERRHAND.cpy) uses numeric return codes and error categories. In Java, these map to an exception hierarchy caught by `@ControllerAdvice`.

### Return Codes

| COBOL Return Code | Constant | Meaning | Java Handling |
|---|---|---|---|
| `0` | `ERR-SUCCESS` | Success | Normal return (no exception) |
| `4` | `ERR-WARNING` | Warning | Log warning, continue processing |
| `8` | `ERR-ERROR` | Error | Throw `PortfolioException` → HTTP 500 |
| `12` | `ERR-SEVERE` | Severe error | Throw `PortfolioException` → HTTP 500 |
| `16` | `ERR-TERMINAL` | Terminal error | Throw `PortfolioException` → HTTP 500 + alert |

### Error Categories

| COBOL Category | Constant | Meaning | Java Mapping |
|---|---|---|---|
| `VS` | `ERR-CAT-VSAM` | VSAM file error | `PortfolioNotFoundException`, `DuplicateKeyException` |
| `VL` | `ERR-CAT-VALID` | Validation error | `ConstraintViolationException` → HTTP 400 |
| `PR` | `ERR-CAT-PROC` | Processing error | `PortfolioException` → HTTP 500 |
| `SY` | `ERR-CAT-SYSTEM` | System error | Generic `Exception` → HTTP 500 |

### Error Response Structure

The `ErrorResponse` DTO mirrors the `ERR-MESSAGE` structure from [`ERRHAND.cpy` lines 30-39](../src/copybook/common/ERRHAND.cpy):

```
ERR-MESSAGE (COBOL)          →  ErrorResponse (Java)
  ERR-DATE + ERR-TIME        →  timestamp (Instant)
  ERR-PROGRAM                →  program (String)
  ERR-CATEGORY               →  category (String)
  ERR-CODE                   →  code (String)
  ERR-SEVERITY               →  severity (String)
  ERR-TEXT                   →  message (String)
  ERR-DETAILS               →  details (String)
```

---

## 7. Program Structure Mapping

### CICS Online Programs

| COBOL Pattern | Java Equivalent |
|---|---|
| `PROCEDURE DIVISION.` | `@RestController` class |
| `EVALUATE WS-FUNCTION` dispatch | Multiple `@GetMapping`/`@PostMapping` methods |
| `PERFORM paragraph` | Private method or service call |
| `EXEC CICS LINK PROGRAM('X')` | `@Autowired` service method call |
| `COPY copybook.` | `import` entity/DTO class |
| `WORKING-STORAGE SECTION.` | Instance fields or local variables |
| `LINKAGE SECTION.` / `DFHCOMMAREA` | Method parameters + return type |

### Batch Programs

| COBOL Pattern | Java Equivalent |
|---|---|
| `OPEN/READ/WRITE/CLOSE` file I/O | Spring Data JPA repository methods |
| `EXEC SQL ... END-EXEC` (DB2) | Spring Data JPA or `@Query` annotations |
| Sequential file processing loop | `repository.findAll()` + stream processing |
| `SORT` verb | `List.sort()` or `ORDER BY` in query |
| Checkpoint/restart | Spring Batch `@StepScope` with chunk processing |

### Source References
- Online controller: [`INQONLN.cbl`](../src/programs/online/INQONLN.cbl) → [`InquiryController.java`](src/main/java/com/portfolio/controller/InquiryController.java)
- CRUD operations: [`PORTMSTR.cbl` lines 82-100](../src/programs/portfolio/PORTMSTR.cbl)
- DB2 connection: [`DB2CONN.cbl` lines 46-80](../src/programs/common/DB2CONN.cbl) → Spring Boot auto-configuration (`application.yml`)

---

## 8. Data Type Quick Reference

| COBOL PIC | Java Type | Notes |
|---|---|---|
| `PIC X(n)` | `String` | Fixed-length in COBOL; trim trailing spaces |
| `PIC 9(n)` | `int` / `long` | Use `long` if n > 9 |
| `PIC 9(8)` (date: YYYYMMDD) | `LocalDate` | Parse with `DateTimeFormatter.BASIC_ISO_DATE` |
| `PIC 9(6)` (time: HHMMSS) | `LocalTime` | Parse with pattern `HHmmss` |
| `PIC S9(n)V9(m) COMP-3` | `BigDecimal` | Always BigDecimal for financial amounts |
| `PIC S9(n) COMP` | `int` / `long` | Binary integer |
| `PIC X(1)` with 88-levels | `enum` | With JPA `AttributeConverter` |
| `PIC X(2)` with 88-levels | `enum` | With JPA `AttributeConverter` (2-char code) |
| Timestamp (`PIC X(26)`) | `Instant` | ISO-8601 format |
| `FILLER` | _(omit)_ | No Java field needed |

---

## 9. DB2 → JPA Mapping

| DB2 SQL Type | JPA/Java Type | Annotation |
|---|---|---|
| `CHAR(n)` | `String` | `@Column(length = n)` |
| `VARCHAR(n)` | `String` | `@Column(length = n)` |
| `DATE` | `LocalDate` | `@Column` |
| `TIME` | `LocalTime` | `@Column` |
| `TIMESTAMP` | `Instant` | `@Column` |
| `DECIMAL(p,s)` | `BigDecimal` | `@Column(precision = p, scale = s)` |
| `INTEGER` | `int` or `Integer` | `@Column` |
| `SMALLINT` | `short` or `Short` | `@Column` |
| Primary key (multi-column) | `@IdClass` or `@EmbeddedId` | See composite key pattern above |
| `WITH DEFAULT` | Column default | `@Column(columnDefinition = "... DEFAULT ...")` or set in constructor |

### Source References
- POSHIST table: [`POSHIST.sql`](../src/database/db2/POSHIST.sql) → [`PositionHistory.java`](src/main/java/com/portfolio/model/PositionHistory.java)
- ERRLOG table: [`ERRLOG.sql`](../src/database/db2/ERRLOG.sql) → (error logging entity, not yet converted)

---

## 10. Security Mapping

| COBOL/CICS | Java/Spring |
|---|---|
| `EXEC CICS ASSIGN USERID(...)` | `SecurityContextHolder.getContext().getAuthentication()` |
| `SECMGR.cbl` (validation) | Spring Security `UserDetailsService` |
| `SECMGR.cbl` (authorization) | `@PreAuthorize` / `@Secured` annotations |
| `SECMGR.cbl` (audit logging) | Spring AOP / `@EventListener` |
| CICS resource security | Spring Security method-level or URL-based security |

---

## 11. Conversion Checklist for New Programs

When converting another COBOL program, follow these steps:

1. **Identify copybooks** used (`COPY` statements) → create JPA entities
2. **Map 88-level conditions** → create Java enums with `AttributeConverter`
3. **Map the PROCEDURE DIVISION structure**:
   - CICS online → `@RestController` with `@GetMapping`/`@PostMapping`
   - Batch → `@Service` or Spring Batch `@StepScope`
4. **Map VSAM/DB2 access** → Spring Data JPA repositories
5. **Map error handling** → exception classes + `@ControllerAdvice`
6. **Create DTOs** for API responses (don't expose entities directly)
7. **Add seed data** (`data.sql`) matching COBOL test data patterns
8. **Write tests** using `@WebMvcTest` or `@SpringBootTest`
9. **Document the mapping** with inline comments referencing original COBOL line numbers

---

## 12. Programs Remaining to Convert

| COBOL Program | Type | Priority | Complexity |
|---|---|---|---|
| `INQPORT.cbl` | Online (CICS) | High | Low — already covered by service layer |
| `INQHIST.cbl` | Online (CICS) | High | Low — already covered by service layer |
| `PORTMSTR.cbl` | Batch/Online | High | Medium — full CRUD operations |
| `TRNVAL00.cbl` | Batch | Medium | Medium — validation logic |
| `POSUPD00.cbl` | Batch | Medium | Medium — position updates |
| `HISTLD00.cbl` | Batch | Medium | Low — ETL from VSAM to DB2 |
| `BCHCTL00.cbl` | Batch | Medium | High — process control/orchestration |
| `SECMGR.cbl` | Online | Medium | Medium — security management |
| `RPTPOS00.cbl` | Batch | Low | Medium — report generation |
| `RPTAUD00.cbl` | Batch | Low | Medium — audit reporting |
| `RPTSTA00.cbl` | Batch | Low | Medium — statistics reporting |
| `DB2CONN.cbl` | Common | N/A | Replaced by Spring Boot auto-configuration |
| `ERRHNDL.cbl` | Common | N/A | Replaced by `GlobalExceptionHandler` |
