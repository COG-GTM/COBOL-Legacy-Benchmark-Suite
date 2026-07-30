# Python Translation Pairs

Reference Python translations of the COBOL programs in `src/`, produced as benchmark
material for LLM translation tools. Each pair is a COBOL program, its Python translation and
a set of behaviour vectors recorded from the compiled COBOL.

The strategy, scope and wave plan are in
[documentation/technical/python-migration-plan.md](../../documentation/technical/python-migration-plan.md).
Read section 6 before adding a pair: translations preserve the behaviour of the original,
including its defects.

## Layout

| Path | Contents |
|------|----------|
| `clbs/runtime/` | COBOL data semantics (picture moves, class tests) used by the translations |
| `clbs/copybooks/` | One module per copybook |
| `clbs/<area>/` | One module per COBOL program, mirroring `src/programs/<area>/` |
| `tests/vectors/` | Inputs and COBOL-recorded outputs, one JSON file per program |
| `tools/cobol_bridge.py` | Compiles a COBOL program with GnuCOBOL and calls it through `libcob` |

## Running the tests

```bash
cd translations/python
python -m pytest        # unit tests plus differential tests against compiled COBOL
ruff check . && ruff format --check .
```

Requires Python 3.10+ and `pytest`. GnuCOBOL (`cobc`, `libcob`) is optional: the
differential tests skip themselves when it is absent, and the unit tests still check the
translation against the recorded vectors.

## Adding a pair

1. Translate the program into `clbs/<area>/<program>.py`, keeping COBOL names — one
   function per paragraph, a dataclass for the LINKAGE record, `Decimal` for numerics.
2. Extend `clbs/runtime/` only with the semantics the program actually needs.
3. Record vectors into `tests/vectors/<program>.json` by running the inputs through the
   compiled COBOL with `tools/cobol_bridge.py`; add a `note` to any case that documents a
   quirk of the original.
4. Add `tests/test_<program>.py` (vectors against Python) and
   `tests/test_<program>_equivalence.py` (COBOL against Python).
5. Document behaviour-changing findings in the migration plan.

## Current pairs

| COBOL program | Python module | Vectors |
|---------------|---------------|---------|
| `src/programs/portfolio/PORTVALD.cbl` | `clbs/portfolio/portvald.py` | `tests/vectors/portvald.json` |
