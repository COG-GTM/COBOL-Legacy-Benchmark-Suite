# `PORTADD` -> `modernized/src/programs/portadd.js`

Create a portfolio, detect a duplicate key. Baseline: **EXECUTED** (GnuCOBOL 3.1.2, staged
with transform 2 — see below).

## Before

`src/programs/portfolio/PORTADD.cbl`, `2100-VALIDATE-AND-ADD`:

```cobol
2100-VALIDATE-AND-ADD.
    IF PORT-ID EQUAL SPACES OR
       PORT-CLIENT-NAME EQUAL SPACES OR
       PORT-STATUS NOT EQUAL 'A'
        ADD 1 TO WS-ERROR-COUNT
        DISPLAY 'Invalid record data: ' PORT-ID
        EXIT PARAGRAPH
    END-IF

    MOVE WS-CURRENT-DATE TO PORT-CREATE-DATE
    MOVE WS-CURRENT-DATE TO PORT-LAST-MAINT

    WRITE PORT-RECORD

    EVALUATE TRUE
        WHEN WS-SUCCESS-STATUS
            ADD 1 TO WS-ADD-COUNT
        WHEN WS-DUP-STATUS
            ADD 1 TO WS-DUP-COUNT
            DISPLAY 'Duplicate record: ' PORT-ID
        WHEN OTHER
            ADD 1 TO WS-ERROR-COUNT
            DISPLAY 'Write error for: ' PORT-ID
    END-EVALUATE
    .
```

Note what this paragraph does *not* do: it never calls `PORTVALD`. The only checks are blank
ID, blank client name, and status not `A`. A portfolio ID of `!!!!!!!!` is accepted.

## After

`modernized/src/programs/portadd.js`:

```javascript
function validateAndAdd(input, { store, runDate }) {
  const record = canonicalize(input);
  if (!String(record.portId || '').trim()
      || !String(record.clientName || '').trim()
      || record.status !== 'A') {
    return validationResult('Invalid record data');
  }
  record.createDate = runDate;
  record.lastMaint = runDate;
  const written = store.write(record);
  return fileResult(written.status, { record: written.record }, true);
}
```

`store.write` returns FILE STATUS `22` when the 18-byte `PORT-KEY` is already present, which
`fileResult` maps to `result: 'conflict'` / HTTP 409. A successful create maps to HTTP 201.
The paragraph name is preserved as the function name so the mapping stays greppable, and
`runDate` is injected rather than read from the clock — `WS-CURRENT-DATE` comes from
`ACCEPT ... FROM DATE`, so a hard-coded `new Date()` would make the create path untestable.

Deliberately *not* "fixed": the missing `PORTVALD` call. Adding validation the legacy program
never performed would change which records are accepted, and this is a port.

## Parity

Golden deck: 6 add attempts — a seeded duplicate, a blank ID, a blank client name, a record
with status `S`, one clean create, and that same new key offered a second time.

```
ADD-COUNTS    PORTADD     EXECUTED  create + duplicate-key detection tallies              PASS
ADD-MSGS      PORTADD     EXECUTED  per-record diagnostics, in emission order             PASS
ADD-STATE     PORTADD     EXECUTED  resulting KSDS contents                               PASS
```

The COBOL run reports `added 1, duplicates 2, errors 3`; the JS handler reports the same, in
the same order, and leaves an identical KSDS. `ADD-STATE` compares every field of all four
resulting records, with COMP-3 money compared as decimals and `PORT-CREATE-DATE` /
`PORT-LAST-MAINT` masked to `@RUNDATE`.

`PORT-LAST-TRANS` is untouched by the create path, so it stays `00000000` and normalizes to
`null` on both sides — worth stating because it is the one field a reader would expect a
create to stamp.

## Staging transform 2

`PORTADD.cbl` does `COPY PORTFLIO` under two separate `FD`s, so every `PORT-*` name is
ambiguous and the compile fails with `PORT-ID requires qualification`. The staged copy renames
the second copy's record. This is a compile-time ambiguity fix with no effect on behaviour;
the executed baseline is still this program's logic.
