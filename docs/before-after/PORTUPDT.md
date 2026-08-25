# `PORTUPDT` -> `modernized/src/programs/portupdt.js`

Field-level update: status, client name, total value. Baseline: **EXECUTED** (staged with
transform 1 — a misaligned banner comment).

## Before

`src/programs/portfolio/PORTUPDT.cbl`, `2100-PROCESS-UPDATE` and `2200-APPLY-UPDATE`:

```cobol
2100-PROCESS-UPDATE.
    MOVE UPDT-KEY TO PORT-KEY
    READ PORTFOLIO-FILE
    IF WS-SUCCESS-STATUS
        PERFORM 2200-APPLY-UPDATE
    ELSE
        ADD 1 TO WS-ERROR-COUNT
        DISPLAY 'Record not found: ' PORT-KEY
    END-IF
    .

2200-APPLY-UPDATE.
    EVALUATE TRUE
        WHEN UPDT-STATUS
            MOVE UPDT-NEW-VALUE TO PORT-STATUS
        WHEN UPDT-NAME
            MOVE UPDT-NEW-VALUE TO PORT-CLIENT-NAME
        WHEN UPDT-VALUE
            MOVE UPDT-NEW-VALUE TO WS-NUMERIC-WORK
            MOVE WS-NUMERIC-WORK TO PORT-TOTAL-VALUE
    END-EVALUATE

    REWRITE PORT-RECORD

    IF WS-SUCCESS-STATUS
        ADD 1 TO WS-UPDATE-COUNT
    ELSE
        ADD 1 TO WS-ERROR-COUNT
        DISPLAY 'Update failed for: ' PORT-KEY
    END-IF
    .
```

**There is no `WHEN OTHER`.** An unrecognised action code falls straight through the
`EVALUATE`, reaches the `REWRITE`, rewrites the record unchanged, and is counted as a
*successful update*. The executed baseline confirms it: the golden deck feeds action `X` and
the run reports `updates 4` for three real updates.

The `V` arm also launders a `PIC X(50)` through an intermediate numeric work field to land in
a `COMP-3` money field. The executed run shows `'99999.99'` resolving to exactly `99999.99`,
so the de-editing move behaves — but this is where a port would silently introduce a
floating-point error if it used a JS `number`.

## After

`modernized/src/programs/portupdt.js`:

```javascript
function applyUpdate(record, action, newValue) {
  const updated = { ...record };
  switch (action) {
    case 'S': updated.status = String(newValue || '').charAt(0); break;
    case 'N': updated.clientName = String(newValue || '').slice(0, 30); break;
    case 'V':
      try { updated.totalValue = new Decimal(String(newValue ?? '').trim()); }
      catch { return null; }
      break;
    default:
      break;   // no WHEN OTHER in 2200-APPLY-UPDATE: rewrite unchanged, count as success
  }
  return updated;
}
```

The truncations mirror the COBOL `MOVE` semantics exactly: `PORT-STATUS` is `PIC X(1)` so only
the first character survives, and `PORT-CLIENT-NAME` is `PIC X(30)` so a longer name is cut,
not rejected. `totalValue` is a `Decimal`, never a `number`.

The empty `default` arm is the defect, reproduced on purpose. In `mode: 'modernized'` the same
function rejects the unknown action instead — that difference is registered as
`DIV-UPDT-UNKNOWN-ACTION`, not left as an undocumented behaviour change.

## Parity

Golden deck: 5 updates — status, name and total value on `PORT0002`, a missing key, and action
`X` on `PORT0001`.

```
UPDT-COUNTS   PORTUPDT    EXECUTED  update tallies (unknown action counts as success)      PASS
UPDT-MSGS     PORTUPDT    EXECUTED  per-record diagnostics, in emission order              PASS
UPDT-STATE    PORTUPDT    EXECUTED  resulting KSDS contents after status/name/value up     PASS
```

COBOL reports `updates 4, errors 1`; so does the JS. `UPDT-STATE` proves the three real
updates landed (`PORT0002` -> status `C`, name `JANE Q PUBLIC-DOE`, total value `99999.99`)
**and** that `PORT0001` came back byte-identical after its unknown-action rewrite. The
not-found diagnostic carries the full 18-byte key (`PORT99980000009998`), not just the ID,
because `2100` displays `PORT-KEY`.

## Known intentional divergence

| Id | Why |
|---|---|
| `DIV-UPDT-UNKNOWN-ACTION` | Legacy has no `WHEN OTHER`, so an unrecognised action rewrites the record unchanged and counts as a success. Legacy mode reproduces this; modernized mode returns a validation error. |
