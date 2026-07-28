# Modernized Java

Java translation of the COBOL Legacy Benchmark Suite, starting with the transaction-processing
slice. Every mapping decision and every discrepancy found in the COBOL source is recorded in
[TRANSLATION-NOTES.md](TRANSLATION-NOTES.md) - read that first.

## Build

```bash
mvn -f modernized/java/pom.xml test
```

Requires JDK 11 or newer and Maven 3.6+.

## Layout

| Path            | Contents |
| --------------- | -------- |
| `model/`        | Translated copybooks: records, level-88 enums and the COBOL storage helpers (`com.clbs.portfolio.model`) |
| `service/`      | Translated programs and the contracts for the subroutines they `CALL` (`com.clbs.portfolio.service`) |
| `src/test/java/`| Test harness (`harness`) and tests, standing in for the z/OS runtime |

## Conventions

- Every decimal field is `BigDecimal` at the scale its picture clause declares, enforced in the
  setter. No `double`, no `float`.
- Arithmetic truncates rather than rounds, and overflow is dropped silently, because that is what
  the unrounded, unguarded COBOL statements do.
- `PIC X(n)` fields are stored space-padded to `n`, so `IF field = SPACES` and record comparisons
  behave as they do on the mainframe.
- Coded fields keep their raw bytes alongside the enum interpretation, so validation can reject and
  echo a value no level-88 covers.
- Methods are named after the paragraphs they translate.
