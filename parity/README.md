# COBOL parity harness

Pins the behaviour of the GnuCOBOL-compatible portfolio programs so the Java 21
port can be checked against them.

```
parity/cobol/run_baseline.sh      # needs GnuCOBOL 3.x (cobc) and python3
```

1. `fixtures/generate.py` writes fixed-width input files using the copybook
   layouts (`PORTFLIO` 148 bytes with COMP-3 money, PORTUPDT 69 bytes,
   PORTDEL 80 bytes) plus PORTVALD requests.
2. The script compiles `PORTVALD`, `PORTADD`, `PORTUPDT`, `PORTDEL`,
   `PORTREAD` from `src/programs/portfolio`, runs ADD -> READ -> UPDATE -> READ
   -> DELETE -> READ against one indexed `PORTFILE`, and writes console output
   to `baseline/`. Timestamps are masked.
3. The Java test `CobolParityTest` replays the same fixtures through the Java
   batch services and compares the output line by line with `baseline/`.

Off-mainframe adjustments made at compile time (the COBOL sources in `src/`
are not modified):

| Program | Adjustment | Why |
|---|---|---|
| PORTADD, PORTUPDT, PORTDEL | Line 1 banner comment shifted from column 8 to column 7 | GnuCOBOL rejects `*` in column 8 |
| PORTADD | Input FD uses `COPY PORTFLIO REPLACING LEADING ==PORT-== BY ==INPT-==` | Copying `PORTFLIO` into both FDs makes every `PORT-*` name ambiguous |
| PORTDEL | `ACCEPT ... FROM TIME STAMP` -> `MOVE FUNCTION CURRENT-DATE` | IBM-only syntax; timestamp is masked anyway |
| MKPORT, VALDRIVR | New helper programs in `parity/cobol/` | Create the empty KSDS; drive the `PORTVALD` subroutine |

Programs that need CICS, DB2 or the missing `PORTREC` copybook (PORTTRAN,
PORTMSTR, online and batch DB2 programs) cannot run here and are covered by
characterization unit tests in the Java project instead.
