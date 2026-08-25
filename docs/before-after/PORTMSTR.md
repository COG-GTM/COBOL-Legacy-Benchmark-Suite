# `PORTMSTR` -> `modernized/src/programs/portmstr.js` + `handler.js`

The C/R/U/D dispatcher — the program that becomes the Lambda entry point. Baseline:
**DERIVED** — does not compile.

## Why there is no executed baseline

`PORTMSTR.cbl` has a `PROCEDURE DIVISION USING` clause but is built as an executable rather
than a called subprogram, and it references `LS-*` linkage fields and `ERR-*` error fields that
no copybook in this repository defines.

Those are not, however, what a plain `cobc -x src/programs/portfolio/PORTMSTR.cbl` prints. That
reports three errors at line 1 (`PROGRAM-ID header missing`, `PROCEDURE DIVISION header
missing`, `syntax error, unexpected *`) and stops, because the banner comment's `*` sits in
column 8 instead of column 7 — the same fixed-format misalignment that staging transform 1
fixes for the programs that do run. Apply that transform and the real blockers appear:

```
:83:  error: executable program requested but PROCEDURE/ENTRY has USING clause
:244: error: 'LS-PROGRAM-ID' is not defined
:245: error: 'ERR-CAT-VSAM' is not defined
... 20 further undefined LS-* / ERR-* / WS-FILE-STATUS / PORT-KEY references
```

These cannot be staged away the way transforms 1-3 are: the missing linkage section and `ERR-*`
copybook would have to be *authored*, which is writing new logic rather than compiling existing
logic. Unlike `PORTTRAN`, though, the *logic* here is complete and unambiguous — only the
linkage is broken — so the derived expectation is a much shorter reach: one `WHEN OTHER` arm.

## Before

`src/programs/portfolio/PORTMSTR.cbl`, the main `EVALUATE`:

```cobol
EVALUATE TRUE
    WHEN CREATE-PORT   PERFORM 2000-CREATE-PORTFOLIO
    WHEN READ-PORT     PERFORM 3000-READ-PORTFOLIO
    WHEN UPDATE-PORT   PERFORM 4000-UPDATE-PORTFOLIO
    WHEN DELETE-PORT   PERFORM 5000-DELETE-PORTFOLIO
    WHEN OTHER
        MOVE 'Invalid command' TO WS-ERROR-MSG
        PERFORM 9000-ERROR
END-EVALUATE
```

and the file-status mapping in `9000-ERROR`:

```cobol
EVALUATE WS-FILE-STATUS
    WHEN ERR-VSAM-DUPKEY   ...
    WHEN ERR-VSAM-NOTFND   ...
    WHEN OTHER             ...
END-EVALUATE
```

`CREATE-PORT` / `READ-PORT` / `UPDATE-PORT` / `DELETE-PORT` are level-88 condition names on a
single command field — the classic COBOL dispatch idiom, and a direct match for a serverless
`action` router.

## After

`modernized/src/programs/portmstr.js` is the router:

```javascript
const ROUTES = {
  create:      (event, context) => portadd.validateAndAdd(event.record, context),
  read:        (event, context) => portread.readPortfolio(event.key, context),
  list:        (event, context) => portread.listPortfolios(context),
  update:      (event, context) => portupdt.processUpdate(event, context),
  delete:      (event, context) => portdel.processDelete(event, context),
  transaction: (event, context) => porttran.processTransaction(event.transaction, context),
};

function dispatch(action, event, context) {
  const route = ROUTES[action];
  if (!route) return { result: 'invalidCommand', http: 400, message: 'Invalid command' };
  return route(event, context);
}
```

and `modernized/src/handler.js` is the Lambda surface:

```javascript
const system = createSystem({ runDate: '00000000' });
module.exports = { handler: system.handler, store: system.store, ... };
```

Two deliberate structural choices:

- **`list` is a route the COBOL `EVALUATE` does not have.** `READ-PORT` covers both keyed read
  and sequential list in `PORTREAD`; splitting them is the right REST shape, and the `list` case
  is exercised by `READ-LIST` rather than smuggled into the `read` case.
- **The level-88 dispatch becomes an object lookup, not a `switch`.** An unknown key falls to
  the `WHEN OTHER` arm by the absence of a route, which is structurally the same guarantee the
  `EVALUATE` gives.

`9000-ERROR`'s status mapping lives in `modernized/src/programs/result.js`: `00` -> `ok` / 200
(201 on create), `22` -> `conflict` / 409, `23` -> `notFound` / 404, `10` -> EOF, anything else
-> `ioError` / 500. Validation failures are 400 with the `PORTVALD` return code and message
attached.

`AUDPROC` / `ERRPROC`, which `PORTMSTR` and `PORTDEL` `CALL`, are replaced by structured audit
records through `auditStore.append` — a keyed append-only log, matching the mainframe
data-layer mapping of a sequential audit file.

## Parity

```
MSTR-OTHER    PORTMSTR    DERIVED   WHEN OTHER arm of the command EVALUATE                PASS
```

Only the invalid-command arm is asserted at this level, and it is derived. That is honest
rather than thin: the four real arms are dispatch, and their behaviour is proven end-to-end by
the `ADD-*`, `READ-LIST`, `UPDT-*` and `DEL-*` cases, which all reach their programs **through
this router** — every one of those 10 cases goes through `dispatch()`, and all of them are
`EXECUTED` against real COBOL. A broken route would fail them.
