# Before / after: COBOL portfolio programs -> JavaScript

One artifact per program. Each shows the original COBOL paragraph ("before"), the JS function
that replaces it ("after"), and the parity-report lines that prove they agree on the golden
dataset.

| Program | Artifact | Baseline |
|---|---|---|
| `PORTMSTR` | [PORTMSTR.md](PORTMSTR.md) | DERIVED — does not compile |
| `PORTADD` | [PORTADD.md](PORTADD.md) | **EXECUTED** |
| `PORTREAD` | [PORTREAD.md](PORTREAD.md) | **EXECUTED** |
| `PORTUPDT` | [PORTUPDT.md](PORTUPDT.md) | **EXECUTED** |
| `PORTDEL` | [PORTDEL.md](PORTDEL.md) | **EXECUTED** |
| `PORTTRAN` | [PORTTRAN.md](PORTTRAN.md) | DERIVED — `PORTREC` copybook absent |
| `PORTVALD` | [PORTVALD.md](PORTVALD.md) | **EXECUTED** |

## What "EXECUTED" means here

`EXECUTED` means the expectation was captured by compiling the program with GnuCOBOL 3.1.2
and running it against the golden inputs — the bytes in `golden/expected/` came out of a real
COBOL process. `DERIVED` means the program **cannot be compiled** and the expectation was
reasoned out of the source by hand; the reasoning is written down in
`golden/expected/DERIVED.md`. Nothing in this directory blurs the two.

Two of the seven cannot be executed, for reasons that are defects in the repository rather
than limitations of the harness:

- `PORTTRAN` does `COPY PORTREC`, and no `PORTREC` copybook exists anywhere in the repo. Its
  `PORT-TOTAL-UNITS` / `PORT-TOTAL-COST` fields are not in `PORTFLIO.cpy` either.
- `PORTMSTR` passes `USING` on a program compiled as an executable and references `LS-*` and
  `ERR-*` fields that no copybook in this repo defines.

Three of the five that do run needed a purely mechanical staging transform first (a
misaligned banner comment, a duplicated copybook, an unsupported `ACCEPT ... FROM TIME
STAMP`). `golden/cobol/stage.sh` applies those to a copy under `build/stage/`; **nothing under
`src/` is modified**, and every transform is listed in `modernized/CONTRACTS.md` section 6.

## Reproducing

```sh
./golden/cobol/stage.sh          # compile the legacy programs (needs GnuCOBOL)
./golden/cobol/run-baseline.sh   # capture the "before" into golden/expected/
node golden/parity/run.js        # run the "after" and diff -> golden/PARITY-REPORT.txt
npx jest                         # same thing as a build gate
```

The golden inputs are fixed literals written by `golden/cobol/GOLDGEN.cbl` — there is no
random seed to pin, which is a stronger reproducibility guarantee than `TSTGEN00`'s
`RANDOM-SEED` file. The only nondeterministic value anywhere in the baseline is the create-path
date stamp, which comes from `ACCEPT ... FROM DATE`; both sides mask it to `@RUNDATE`.
