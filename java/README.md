# Java Scaffolding (Migrated from COBOL Templates)

This Maven module contains the Java-migrated equivalents of the reusable
COBOL scaffolding templates under `src/templates/`. These are structural
patterns — not full applications — preserved as translation reference and
benchmark artifacts. The original COBOL templates are left untouched for
side-by-side comparison.

## Mapping

| COBOL template | Java class(es) |
| --- | --- |
| `src/templates/program/standard-program.cbl` | `com.cog.clbs.program.AbstractProgram`, `com.cog.clbs.program.ReturnCode` |
| `src/templates/program/file-handling.cbl` | `com.cog.clbs.file.VsamFile`, `com.cog.clbs.file.SequentialFile`, `com.cog.clbs.file.FileStatus` |
| `src/templates/database/db2-handling.cbl` | `com.cog.clbs.db.Db2Handler`, `com.cog.clbs.db.SqlCodeException` |
| `src/templates/error/error-handling.cbl` (+ `src/copybook/online/ERRHND.cpy`, `src/programs/online/ERRHNDL.cbl`) | `com.cog.clbs.error.ErrorHandler`, `com.cog.clbs.error.ErrorRecord`, `com.cog.clbs.error.ErrorSeverity`, `com.cog.clbs.error.ErrorAction` |

## Concept translation

- COBOL paragraph flow (`0000-MAIN` → `1000-INITIALIZE` → `2000-PROCESS` →
  `3000-TERMINATE`) becomes the `AbstractProgram` lifecycle
  (`run()` → `initialize()` / `execute()` loop / `terminate()`).
- The z/OS `RETURN-CODE` register and RC 0/4/8/12/16 convention become the
  `ReturnCode` enum and `AbstractProgram`'s return-code field.
- VSAM KSDS keyed access (READ/WRITE/REWRITE/DELETE with FILE STATUS
  checking) becomes `VsamFile` with a `FileStatus` enum mirroring the
  two-character status codes ('00', '10', '22', '23', ...).
- Embedded SQL (SQLCA/SQLCODE, host variables, cursors, COMMIT/ROLLBACK)
  becomes `Db2Handler` over JDBC: `PreparedStatement` parameters replace
  host variables, `ResultSet` replaces cursors, and `SqlCodeException`
  carries the SQLCODE.
- The `ERRHND.cpy` error record (program, paragraph, sqlcode, severity
  F/W/I, action R/C/A, message, trace id, timestamp) becomes `ErrorRecord`
  with `ErrorSeverity`/`ErrorAction` enums; `ErrorHandler` implements the
  logging, message formatting, action determination, and final
  return-code logic.

## Build & test

```bash
cd java
mvn compile   # compile the scaffolding
mvn test      # run the JUnit smoke tests
```

Requires JDK 17+ and Maven.
