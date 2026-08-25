# `PORTTRAN` -> `modernized/src/programs/porttran.js`

Transaction processing: buy, sell, transfer, fee. Baseline: **DERIVED** — this program cannot
be compiled, for two independent reasons.

## Why there is no executed baseline

1. `PORTTRAN.cbl` contains `COPY PORTREC`. **No `PORTREC` copybook exists anywhere in this
   repository** (`find . -iname 'PORTREC*'` returns nothing). The task brief said to "locate and
   read" it; it is not there.
2. Its `PORT-TOTAL-UNITS` and `PORT-TOTAL-COST` are not in `PORTFLIO.cpy` either, so
   substituting the copybook that *does* exist still leaves the program undefined.

It also declares `RECORD KEY IS PORT-ID` (8 bytes) while every other program in the portfolio
uses the 18-byte `PORT-KEY`, and it does `REWRITE PORTFOLIO-RECORD` where the `FD` record is
`PORT-RECORD`. Even with a `PORTREC` copybook in hand, this program would not link.

The derived layout used by the port keeps the first 98 bytes of `PORTFLIO` intact and places
`PORT-TOTAL-UNITS` as `S9(11)V9(4) COMP-3` at offset 98 and `PORT-TOTAL-COST` as
`S9(13)V99 COMP-3` at offset 106, inside what `PORTFLIO` calls `PORT-FILLER`. That is a
reconstruction from field usage, recorded as `PORTREC_RECORD` in
`modernized/src/schema/records.js` and reasoned out in `golden/expected/DERIVED.md`. **It is
not a copybook that was found.**

## Before

The bug that dominates this program's actual behaviour is in `2000-PROCESS-TRANSACTION`:

```cobol
2000-PROCESS-TRANSACTION.
    PERFORM 2100-VALIDATE-TRANSACTION

    IF WS-VALID-TRANSACTION
        ADD 1 TO WS-PROCESSED-COUNT
    END-IF
    .
```

`2200-UPDATE-POSITIONS` — and with it `2210-PROCESS-BUY`, `2220-PROCESS-SELL`,
`2230-PROCESS-TRANSFER`, `2240-PROCESS-FEE` and `2300-UPDATE-AUDIT-TRAIL` — is **never
`PERFORM`ed from anywhere**. The reachable program validates a transaction, counts it, and
moves on. No position is ever updated; no audit record is ever written. The entire BU/SL/TR/FE
position math the brief describes is dead code.

The dead paragraphs, for the record:

```cobol
2210-PROCESS-BUY.
    ADD TRN-QUANTITY TO PORT-TOTAL-UNITS
    ADD TRN-AMOUNT   TO PORT-TOTAL-COST
    .

2220-PROCESS-SELL.
    IF PORT-TOTAL-UNITS < TRN-QUANTITY
        MOVE 'Insufficient units for sale' TO WS-ERROR-MSG
        PERFORM 9000-ERROR-ROUTINE
    ELSE
        SUBTRACT TRN-QUANTITY FROM PORT-TOTAL-UNITS
        SUBTRACT TRN-AMOUNT   FROM PORT-TOTAL-COST
    END-IF
    .

2230-PROCESS-TRANSFER.
    MOVE 'Transfer processing not implemented' TO WS-ERROR-MSG
    PERFORM 9000-ERROR-ROUTINE
    .

2240-PROCESS-FEE.
    SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST
    .
```

Note `2230`: transfer is not merely unimplemented, it is an *error path*. And `2220`'s
insufficient-units branch routes to the same `9000-ERROR-ROUTINE` that validation failures
use, which is why a sell rejected on units counts as both processed and an error.

## After

The port carries both behaviours, selected by `mode`, because "which one is correct" is not the
harness's call to make:

```javascript
function updatePositions(transaction, context, checked) {
  if (context.mode === 'legacy') {
    // 2200-UPDATE-POSITIONS is never PERFORMed: validate, count, and leave the record alone.
    return fileResult('00', { processed: 1, record: checked.portfolio });
  }
  let updated;
  switch (checked.type) {
    case 'BU': updated = processBuy(checked.portfolio, checked.quantity, checked.amount); break;
    case 'SL': { ... }
    case 'FE': updated = processFee(checked.portfolio, checked.amount); break;
    case 'TR': { ... }
  }
  const saved = context.store.rewrite(updated);
  if (saved.status !== '00') return fileResult(saved.status);
  updateAudit(context, checked);
  return fileResult('00', { processed: 1, applied: true, record: saved.record });
}
```

`legacy` mode reproduces the unreachable-code defect exactly. `modernized` mode implements the
dead paragraphs as written — including transfer staying a rejection, since "not implemented" is
the documented behaviour and inventing transfer semantics would be inventing requirements.

All four arms operate on `Decimal`. `PORT-TOTAL-UNITS` at `V9(4)` and `PORT-TOTAL-COST` at
`V99` are different scales, so a `number`-based port would drift on exactly the fee-and-sell
sequence the golden deck exercises.

## Parity

Golden deck: 9 transactions — buys, a sell within units, a sell exceeding units, a transfer, a
fee, an unknown type `ZZ`, a nonexistent portfolio, a zero quantity, and a blank portfolio ID.

```
TRAN-L-CNT    PORTTRAN    DERIVED   reachable legacy path: validate and count only        PASS
TRAN-L-MSG    PORTTRAN    DERIVED   reachable legacy path: rejection messages             PASS
TRAN-L-POS    PORTTRAN    DERIVED   2200-UPDATE-POSITIONS unreachable, so no position ch  PASS
TRAN-M-CNT    PORTTRAN    DERIVED   intended position math: tallies                       PASS
TRAN-M-POS    PORTTRAN    DERIVED   intended position math: BU/SL/TR/FE applied to holdi  PASS
TRAN-M-MSG    PORTTRAN    DERIVED   intended position math: rejection messages            PASS
TRAN-M-AUD    PORTTRAN    DERIVED   audit trail written after the EVALUATE, even for rej  PASS
```

Legacy: `read 9, processed 5, errors 4`, no position changes, no audit records. Modernized:
`read 9, processed 5, applied 3, errors 6` — the extra two errors are the units-rejected sell
and the transfer, which are counted as processed *and* as errors.

**These seven lines prove the port matches a hand-derived expectation, not a COBOL run.** They
are the weakest evidence in the suite and are labelled `DERIVED` in every report line. The
report footer counts them separately (8 derived of 50) so the distinction survives a skim.

## Known intentional divergence

| Id | Why |
|---|---|
| `DIV-TRAN-POSITIONS` | `2200-UPDATE-POSITIONS` is unreachable in the legacy source, so no position is ever updated. Legacy mode reproduces that; modernized mode implements the intended buy/sell/transfer/fee math. |
