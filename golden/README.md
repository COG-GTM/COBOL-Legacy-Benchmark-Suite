# Golden-dataset parity harness

Proves that the JavaScript serverless handlers in `serverless/src/handlers/`
behave the same as the original COBOL portfolio programs, case by case,
including the error paths.

## How parity is established

```
golden/cases/*.json          hand written case definitions (records + aliases)
        |  npm run golden:inputs
        v
golden/inputs/<PROG>/<CASE>/{seed,input}.dat     fixed-width bytes, per PORTFLIO.cpy / TRNREC.cpy
        |                                    \
        |  npm run golden:capture              \  decoded by serverless/src/codec
        v                                       v
golden/cobol-run/<PROG>/<CASE>/                serverless/src/handlers/<prog>.js
  stdout.txt exit-code.txt dump.dat audit.dat
        |  npm run golden:expected                     |
        v                                              v
golden/expected/<PROG>/<CASE>.json  ===== diff =====  canonical actual result
  (canonical EXPECTED-RESULTS)        npm run parity
                                              |
                                              v
                                  golden/reports/parity-report.txt
```

Both sides are reduced to the same canonical form
(`serverless/src/parity/canonical.js`) before being compared: fixed-width text
is trimmed of padding, COMP-3 money is decoded to exact decimal strings and
compared by value rather than by packed bytes, and clock-driven fields are
replaced with the `@RUNDATE` / `@RUNSTAMP` tokens. Any divergence fails
`npm test`, so the build breaks if the JS conversion drifts from COBOL.

The JS side is fed the *same fixed-width files* the COBOL programs were fed,
decoded with the codec — not the case JSON. The two sides therefore cannot
differ in what they were asked to process, only in how they processed it.

## Commands

| command | effect |
| --- | --- |
| `npm run golden:inputs` | regenerate the fixed-width fixtures from `golden/cases` |
| `npm run golden:capture` | compile the COBOL with GnuCOBOL and re-capture `golden/cobol-run` |
| `npm run golden:expected` | normalize the captured COBOL artifacts into `golden/expected` |
| `npm run parity` | run the parity suite only |
| `npm test` | codec + store unit tests plus the parity suite |

`golden:capture` needs GnuCOBOL; the other commands need only Node 20+ and run
from the committed fixtures, so parity is reproducible in CI without a COBOL
runtime.

## Determinism

`golden/config/golden-config.json` fixes `randomSeed` (`123456789`), mirroring
the RANDSEED file `src/programs/test/TSTGEN00.cbl` reads. Generated volume
records come from a seeded LCG in `tools/golden/lib/generate.mjs`, so
regenerating the inputs reproduces the same bytes. `captureRunDate` records the
date the COBOL goldens were captured, which is what lets clock-stamped fields
be tokenized instead of compared literally.

## Provenance of expected results

- **39 cases (PORTADD / PORTREAD / PORTUPDT / PORTDEL / PORTVALD)**: captured
  from real COBOL runs — GnuCOBOL 3.1.2.0, BDB indexed handler. `derived: false`.
- **10 cases (PORTTRAN)**: `derived: true`. `src/programs/portfolio/PORTTRAN.cbl`
  cannot be compiled in this repo — it copies a `PORTREC` copybook that does not
  exist under `src/copybook` and references `PORT-TOTAL-UNITS` /
  `PORT-TOTAL-COST`, which no copybook here defines. Each expected file carries a
  `derivation` field quoting the paragraphs it was derived from, and the report
  marks these cases `DERIVED EXPECTED`.

Note on PORTTRAN: nothing in the program performs `2200-UPDATE-POSITIONS`, so
the buy/sell/transfer/fee paragraphs, the insufficient-units check and the
`AUDPROC` audit trail are unreachable dead code. The program reads, validates
and counts; the portfolio file is opened I-O and never rewritten. The goldens
and the JS handler encode that actual behavior, not the intent of the dead code
— implementing position keeping in JS would have made the harness green against
a COBOL program that does no such thing.

## Captured COBOL quirks the JS handlers reproduce

These are behaviors of the real programs that differ from what their comments or
`documentation/operations/test-data-specs.md` imply. The goldens are the record
of what the code does.

- `PORTVALD` rejects *every* portfolio ID, including a well-formed `PORT0001`:
  after the `PORT` prefix check it moves the four digit characters into a `X(10)`
  work field and applies the `NUMERIC` class test, which the six trailing spaces
  can never pass.
- `PORTVALD` account validation runs against the whole `X(50)` linkage field, so
  a left-justified account number with trailing spaces fails while a 50-digit
  zero-padded one passes.
- `PORTVALD` amount validation moves an alphanumeric value into `S9(13)V99`,
  which yields an integer with zero decimal places — so no 13-digit input can
  fall outside the documented range.
- `PORTUPDT` action `V` inherits the same MOVE semantics: a 50-character
  `...2500075` becomes `2500075.00`, not `25000.75`.
- `PORTUPDT` does not restamp `PORT-LAST-MAINT` on a successful update.
- `PORTDEL` writes its 80-byte audit record with `AUD-FILLER` still at the
  `INITIALIZE`d low-values, so the golden audit filler is 27 NUL bytes.
- `PORTDEL` does not validate the reason code (`DEL-005`).

## Layout source of truth

`src/copybook/common/PORTFLIO.cpy` (148-byte record, 18-byte `PORT-KEY` =
`PORT-ID X(8)` + `PORT-ACCOUNT-NO X(10)`, `S9(13)V99 COMP-3` money) and
`src/copybook/common/TRNREC.cpy`, transcribed in
`serverless/src/codec/layouts.js`. The simplified layout in
`documentation/operations/test-data-specs.md` is *not* used; only its list of
canonical test cases is.

## Capture-only COBOL patches

`golden/patches/` holds minimal patches applied to *copies* of the sources under
`build/src/` during capture (fixed-format comment columns, a duplicate-copybook
name clash in PORTADD, an `ACCEPT ... FROM TIME STAMP` GnuCOBOL rejects in
PORTDEL). Tracked files under `src/` are never modified. See
`golden/patches/README.md`.
