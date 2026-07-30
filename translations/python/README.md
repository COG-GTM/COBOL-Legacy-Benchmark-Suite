# Python Translation Pairs

Reference Python translations of the COBOL programs in this repository. Each
pair is a benchmark artefact: the COBOL source is the specification, the Python
module is the reference answer, and the parity tests are the grader. Strategy,
scope and roadmap are described in
[Python Migration Plan](../../documentation/technical/python-migration-plan.md).

## Layout

```
translations/python/
├── oracle/          COBOL drivers that produce the expected results
├── src/clbs/        Python translations, mirroring the COBOL source tree
└── tests/           Parity tests and generated golden files
```

Module paths mirror the COBOL tree so a pair can be located mechanically:

| COBOL source                          | Python module                          |
|---------------------------------------|----------------------------------------|
| `src/programs/portfolio/PORTVALD.cbl` | `clbs.programs.portfolio.portvald`     |
| `src/copybook/common/PORTVAL.cpy`     | `clbs.copybook.common.portval`         |

COBOL data-item semantics that Python does not share (fixed-width `PIC X`
moves, the `IS NUMERIC` class test, alphanumeric-to-numeric moves) live in
`clbs.runtime.picture` rather than being reinvented in each program.

## Completed pairs

| Program    | Python module                      | Oracle driver          |
|------------|------------------------------------|------------------------|
| `PORTVALD` | `clbs/programs/portfolio/portvald.py` | `oracle/PVDRIVR.cbl` |

## Running the tests

```bash
cd translations/python
pip install -e '.[test]'
pytest
```

Regenerating the golden file additionally requires GnuCOBOL (`cobc`):

```bash
./oracle/generate_golden.sh
```

The script compiles the COBOL program and the driver, replays
`oracle/portvald_cases.txt` through the real program and rewrites
`tests/golden/portvald.txt`. Golden files are committed so the parity tests run
without a COBOL toolchain.

## Translation rules

1. **Behaviour first.** The translation reproduces what the COBOL program
   does, not what its comments say it should do. Defects are preserved and
   documented, never silently corrected.
2. **Structure is traceable.** Paragraphs become functions carrying the
   paragraph name in the docstring; copybooks become modules; the linkage
   section becomes a dataclass that the entry point mutates, as a COBOL
   `CALL ... USING` does.
3. **Money is `Decimal`.** `PIC S9(n)V99` and `COMP-3` fields never become
   `float`.
4. **Fixed width is part of the contract.** Fields keep their COBOL length and
   space padding, because callers and file layouts depend on it.
5. **Divergences are recorded.** Anything the translation cannot reproduce is
   listed in the pair's divergence notes below.

## PORTVALD divergence notes

The following behaviours are faithful to the COBOL program and confirmed by the
oracle. They are the reason a translation that implements the *intent* of the
program fails the parity tests.

| Behaviour | Cause | Oracle evidence |
|-----------|-------|-----------------|
| Portfolio ID validation rejects every input, including `PORT1234` | `LS-INPUT-VALUE(5:4)` is moved into the `PIC X(10)` field `VAL-NUMERIC-CHECK`, which leaves six trailing spaces, so `IS NOT NUMERIC` is always true | cases `C001`–`C007` |
| Account validation rejects every account number shorter than 50 digits | the class test is applied to the whole `PIC X(50)` linkage field instead of the ten significant characters | cases `C008`–`C013` |
| Amount validation accepts every input | `VAL-MIN-AMOUNT`/`VAL-MAX-AMOUNT` span the full capacity of the `PIC S9(13)V99` work field, so the converted value can never fall outside the range | cases `C021`–`C028` |
| An unknown validation type returns `1`, the same code as an invalid portfolio ID | `WHEN OTHER` moves `VAL-INVALID-ID` into the return code | cases `C029`–`C030` |

`clbs.runtime.picture.move_alphanumeric_to_numeric` reproduces GnuCOBOL 3.1.2
conversion of an alphanumeric sender to a signed numeric receiver. IBM
Enterprise COBOL treats such a sender as an unsigned integer, so a sender
holding `-250.75` converts differently on z/OS. The behaviour is unobservable
here because amount validation accepts everything, but it must be re-verified
against a z/OS oracle before any pair depends on it.
