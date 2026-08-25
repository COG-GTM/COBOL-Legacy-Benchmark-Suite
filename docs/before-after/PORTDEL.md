# `PORTDEL` -> `modernized/src/programs/portdel.js`

Delete a portfolio and write an audit record. Baseline: **EXECUTED** (staged with transform 3
— an unsupported `ACCEPT ... FROM TIME STAMP`).

## Before

`src/programs/portfolio/PORTDEL.cbl`, `2100-PROCESS-DELETE` through `2300-WRITE-AUDIT`:

```cobol
2100-PROCESS-DELETE.
    MOVE DEL-KEY TO PORT-KEY
    READ PORTFOLIO-FILE
    EVALUATE TRUE
        WHEN WS-SUCCESS-STATUS   PERFORM 2200-DELETE-RECORD
        WHEN WS-REC-NOT-FND      ADD 1 TO WS-NOT-FND-COUNT
                                 DISPLAY 'Record not found: ' PORT-KEY
        WHEN OTHER               ADD 1 TO WS-ERROR-COUNT
                                 DISPLAY 'Read error for: ' PORT-KEY
    END-EVALUATE
    .

2200-DELETE-RECORD.
    DELETE PORTFOLIO-FILE
    IF WS-SUCCESS-STATUS
        ADD 1 TO WS-DELETE-COUNT
        PERFORM 2300-WRITE-AUDIT
    ELSE
        ADD 1 TO WS-ERROR-COUNT
        DISPLAY 'Delete failed for: ' PORT-KEY
    END-IF
    .

2300-WRITE-AUDIT.
    ACCEPT WS-TIMESTAMP FROM TIME STAMP
    MOVE WS-TIMESTAMP     TO AUD-TIMESTAMP
    MOVE 'DELETE'         TO AUD-ACTION
    MOVE PORT-KEY         TO AUD-KEY
    MOVE DEL-REASON-CODE  TO AUD-REASON
    MOVE PORT-STATUS      TO AUD-STATUS
    WRITE AUDIT-RECORD
    .
```

Three details that a careless port gets wrong:

1. A not-found delete increments `WS-NOT-FND-COUNT`, **not** `WS-ERROR-COUNT`. The executed run
   ends with `errors 0000000` despite two missing keys.
2. The audit record is written *only* on a successful delete, and only after the counter.
3. `AUD-STATUS` is `PORT-STATUS` — the **deleted portfolio's own status**, read from the record
   before deletion. It is not a result code for the delete operation. The executed audit line
   is `AUD|DELETE|PORT00030000000003|01|S`, and that trailing `S` is the portfolio's status,
   which is why the golden deck deletes a suspended portfolio: an `A` would have made the two
   readings indistinguishable.

The audit layout is declared inline in `PORTDEL`'s `FD`, not via `AUDITLOG.cpy`, and it is 80
bytes (26 timestamp + 6 action + 18 key + 2 reason + 1 status + 27 filler) — not the 392-byte
`AUDIT-LOG-RECORD` from the copybook. Both layouts exist in
`modernized/src/schema/records.js`, kept separate.

## After

`modernized/src/programs/portdel.js`:

```javascript
function deleteRecord(input, context) {
  const current = context.store.read(input.key);
  if (current.status !== '00') return fileResult(current.status);
  const deleted = context.store.delete(input.key);
  if (deleted.status === '00') {
    // AUD-STATUS is PORT-STATUS: the deleted portfolio's status, not a delete result code.
    writeAudit(context, keyOf(input.key), input.reasonCode, current.record.status);
  }
  return fileResult(deleted.status, { record: deleted.record });
}
```

The read-before-delete is preserved rather than collapsed into a single store call, because the
audit record needs `PORT-STATUS` from the record as it stood before deletion.

`ACCEPT ... FROM TIME STAMP` becomes an injected `runDate` on the audit record. The parity
harness does not compare audit timestamps on either side — the COBOL value is
sub-second-nondeterministic, so comparing it would be comparing noise. Every other field of
the audit record is compared.

## Parity

Golden deck: 3 deletes — a suspended portfolio that exists, a key that never existed, and a
key deleted earlier in the same run.

```
DEL-COUNTS    PORTDEL     EXECUTED  delete tallies                                        PASS
DEL-MSGS      PORTDEL     EXECUTED  per-record diagnostics, in emission order             PASS
DEL-STATE     PORTDEL     EXECUTED  resulting KSDS contents and audit trail               PASS
```

COBOL reports `deleted 1, not found 2, errors 0`; the JS matches, including the
not-found-is-not-an-error distinction. `DEL-STATE` compares the surviving KSDS records and the
one audit record, field by field.

## Staging transform 3

`ACCEPT WS-TIMESTAMP FROM TIME STAMP` is not supported by GnuCOBOL 3.1.2 (`syntax error, unexpected STAMP`). The staged copy substitutes a supported `ACCEPT ... FROM DATE`/`TIME`
pair filling the same `PIC X(26)`. Since the harness excludes the audit timestamp from
comparison, this transform cannot affect any compared value.
