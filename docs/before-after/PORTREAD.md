# `PORTREAD` -> `modernized/src/programs/portread.js`

Sequential list and keyed read. Baseline: **EXECUTED** — this is the one program that compiled
clean, with no staging transform at all.

## Before

`src/programs/portfolio/PORTREAD.cbl`, `2000-PROCESS` and `2100-DISPLAY-RECORD`:

```cobol
2000-PROCESS.
    READ PORTFOLIO-FILE NEXT RECORD
        AT END
            SET END-OF-FILE TO TRUE
        NOT AT END
            ADD 1 TO WS-RECORD-COUNT
            PERFORM 2100-DISPLAY-RECORD
    END-READ
    .

2100-DISPLAY-RECORD.
    DISPLAY 'Portfolio Record: ' WS-RECORD-COUNT
    DISPLAY '  ID: ' PORT-ID
    DISPLAY '  Account: ' PORT-ACCOUNT-NO
    DISPLAY '  Client: ' PORT-CLIENT-NAME
    DISPLAY '  Status: ' PORT-STATUS
    DISPLAY '  Total Value: ' PORT-TOTAL-VALUE
    DISPLAY ' '
    .
```

`READ ... NEXT RECORD` on a KSDS returns records in **ascending primary-key order**, which is
not insertion order. The golden seed deck is written deliberately out of order so the
distinction is actually tested; `AT END` sets FILE STATUS `10`.

## After

`modernized/src/programs/portread.js`:

```javascript
function listPortfolios({ store }) {
  const records = [];
  let cursor = 0;
  while (true) {
    const next = store.readNext(cursor);
    if (next.status === '10') return fileResult('00', { records, count: records.length });
    if (next.status !== '00') return fileResult(next.status, { records, count: records.length });
    records.push(next.record);
    cursor = next.cursor;
  }
}
```

The cursor loop is kept instead of returning the whole map, so that `readNext` / FILE STATUS
`10` semantics stay visible at the call site rather than being buried in the store. `store.readNext` sorts on the 18-byte key each call:

```javascript
readNext(cursor = 0) {
  const keys = [...this.records.keys()].sort();
  if (cursor >= keys.length) return { status: '10', cursor };
  ...
}
```

The `Map` preserves insertion order, so sorting is what supplies KSDS ordering. Note that
sorting the concatenated `PORT-ID` + `PORT-ACCOUNT-NO` string is byte-order-equivalent to
sorting the COBOL key only because both components are fixed-width and space-padded — which is
exactly why the port keeps them fixed-width instead of trimming.

Keyed read is the same store call with an exact key, mapping FILE STATUS `23` to
`result: 'notFound'` / HTTP 404.

## Parity

```
READ-LIST     PORTREAD    EXECUTED  sequential READ NEXT in PORT-KEY order                PASS
```

The COBOL `DISPLAY` block is parsed back into records by `parseReadListing` in
`golden/parity/normalize.js` and compared to the JS `records` array **in order**, so a
correct-set-but-wrong-order result fails. The record count (`3`) is compared too.

`PORT-TOTAL-VALUE` is displayed by COBOL as a de-edited `COMP-3` field; the normalizer parses
it to a decimal and compares by value, so the JS `Decimal` does not have to reproduce
GnuCOBOL's display formatting.
