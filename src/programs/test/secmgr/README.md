# SECMGR regression test

**Objective:** check that the SECMGR authorize request (`'A'`) is tied to the
CICS signed-on user, and that validate (`'V'`) and audit (`'L'`) behave as
before.

CICS and DB2 are not available on Linux. `secmgr_stub.py` rewrites the
`EXEC CICS ASSIGN` and `EXEC SQL` blocks in `SECMGR.cbl` into `CALL`s to
`SECSTUBS.cbl`, keeping the host variables from the source (so the test sees
which user ID is used as the AUTHFILE key). It also drops the
`01 WS-ERROR-AREA.` line, because `ERRHND.cpy` declares its own 01 level.

**Test data:** the AUTHFILE stub holds one grant, `ADMIN / INQONLN / READ`.

| Case | Type | Signed on | SEC-USER-ID | Expected rc | Expected AUTHFILE key |
|---|---|---|---|---|---|
| S1 | A | ALICE | ADMIN | 8 | none queried |
| S2 | A | ADMIN | ADMIN | 0 | ADMIN |
| S3 | A | ALICE | ALICE | 8 | ALICE |
| S4 | A | ADMIN | ALICE | 8 | none queried |
| S5 | A (ASSIGN fails) | ALICE | ADMIN | 12 | none queried |
| S6 | V | ADMIN | ADMIN | 0 | none queried |
| S7 | V | ALICE | ADMIN | 8 | none queried |
| S8 | L | ADMIN | ADMIN | 0 | none queried |

**Run** (needs GnuCOBOL `cobc` and `python3`):

```
src/programs/test/secmgr/run.sh
```

Every line should print `PASS`. If any case fails, the script exits non-zero.
