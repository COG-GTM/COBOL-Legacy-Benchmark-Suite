# INQONLN authorization regression test

Checks that `INQONLN` asks `SECMGR` for authorization before it links to
`INQPORT`/`INQHIST`, that it audits every attempt (`ACCESS_TYPE = 'DENIED'` for
denied ones), and that it fails closed when the audit write fails.

Requires GnuCOBOL (`cobc`) and `python3`; CICS and DB2 are not needed.

```
src/programs/test/inqonln-security/run.sh
```

- `xform.py` copies the real `src/programs/online/INQONLN.cbl` and replaces each
  `EXEC CICS` block with a `CALL` to a stub.
- `stubs.cbl` holds stub versions of `SECMGR`, `INQPORT`, `INQHIST`, `ERRHNDL`
  and of the RECEIVE/SEND/ASSIGN/RETURN commands. Each stub prints an `EVT` line.
  Environment variables pick the scenario: `SCN_FUNC` is the function code, and
  `SCN_V`/`SCN_A`/`SCN_L` are the SECMGR return codes for validate, authorize
  and audit.
- `run.sh` checks the exact order of events for each scenario.
