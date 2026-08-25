# DERIVED expectations — `PORTTRAN` and `PORTMSTR`

Five of the seven portfolio programs have a genuinely **executed** baseline in this
directory (see `modernized/CONTRACTS.md` section 10). Two do not, because they cannot be
compiled without writing new logic:

- **`PORTTRAN`** — `COPY PORTREC` names a copybook that does not exist anywhere in this
  repository, and `REWRITE PORTFOLIO-RECORD` names a record that is never declared.
- **`PORTMSTR`** — passes `USING` on a program compiled as an executable and references
  `LS-*` / `ERR-*` fields that no copybook in this repo defines.

Everything below is **hand-derived by reading the COBOL**, and is labelled `DERIVED` in the
parity report. It is reasoned, not observed. Do not present it as captured output.

---

## 1. `PORTTRAN`

### 1.1 The reachable legacy path validates but never updates

`2200-UPDATE-POSITIONS` is **never `PERFORM`ed**. Grep the source: the only call chain is
`0000-MAIN` -> `2000-PROCESS-TRANSACTIONS` -> `2100-VALIDATE-TRANSACTION` ->
`2110-CHECK-PORTFOLIO` / `2120-CHECK-TRANSACTION-TYPE` / `2130-CHECK-AMOUNTS`. So
`2210-PROCESS-BUY`, `2220-PROCESS-SELL`, `2230-PROCESS-TRANSFER`, `2240-PROCESS-FEE`,
`2300-UPDATE-AUDIT-TRAIL` and `2310-WRITE-AUDIT-RECORD` are all dead code. The reachable
program reads transactions, validates them, counts them, and leaves every portfolio
untouched.

Against the 9-case golden transaction deck (`golden/input/transaction-deck.dat`, written by
`GOLDGEN`), with the three seeded portfolios present:

| Case | Type | Portfolio | Reachable legacy outcome |
|---|---|---|---|
| TRN-01 | `BU` | `PORT0001` | valid — counted |
| TRN-02 | `SL` | `PORT0001` | valid — counted |
| TRN-03 | `SL` | `PORT0002` | valid — counted (validation never checks holdings) |
| TRN-04 | `TR` | `PORT0001` | valid — counted (`2130` skips the price and amount tests when type is `TR`) |
| TRN-05 | `FE` | `PORT0002` | valid — counted |
| TRN-06 | `ZZ` | `PORT0001` | `Invalid Transaction Type: ZZ` |
| TRN-07 | `BU` | `PORT9997` | `Invalid Portfolio ID: PORT9997` |
| TRN-08 | `BU` | `PORT0001` | `Quantity must be greater than zero` (quantity `0`) |
| TRN-09 | `BU` | (spaces) | `Portfolio ID is required` |

Totals: **read 9, processed 5, errors 4.** Portfolio state identical to
`golden/expected/seed-state.txt`. No audit records.

Note TRN-03 is counted as processed even though the sale exceeds the holding — the
insufficient-units test lives in `2220-PROCESS-SELL`, which is unreachable. That is the
sharpest illustration of why this program needed converting.

### 1.2 The intended position math

This is what the JS port implements, since a transaction processor that never updates a
position has no business value. Derived from the bodies of `2210`/`2220`/`2230`/`2240`.

**Starting positions.** `PORTREC`'s `PORT-TOTAL-UNITS` and `PORT-TOTAL-COST` occupy bytes
98..113, which in a `PORTFLIO`-shaped record are part of `PORT-FILLER` and therefore contain
spaces — not a valid packed-decimal value. The derived model initialises both to **zero** for
every seeded portfolio. That choice is part of the derivation, not an observation.

| Case | Rule applied | Result |
|---|---|---|
| TRN-01 | `BU`: units += 100.0000, cost += 15025.00 | `PORT0001` units `100.0000`, cost `15025.00` |
| TRN-02 | `SL`: units 100.0000 >= 40.0000, so units -= 40.0000, cost -= 6200.00 | `PORT0001` units `60.0000`, cost `8825.00` |
| TRN-03 | `SL`: units 0.0000 < 999999.0000 | `Insufficient units for sale`, no change |
| TRN-04 | `TR`: hard-coded rejection | `Transfer processing not implemented`, no change |
| TRN-05 | `FE`: cost -= 125.50 (units untouched) | `PORT0002` units `0.0000`, cost `-125.50` |
| TRN-06..09 | rejected in validation, never reach `2200` | no change |

Final derived state: `PORT0001` units `60.0000` / cost `8825.00`; `PORT0002` units `0.0000` /
cost `-125.50`; `PORT0003` units `0.0000` / cost `0.00`.

Counts: read 9, validation-passed 5, position updates applied 3 (TRN-01, TRN-02, TRN-05),
errors 6 — the 4 validation rejects plus the insufficient-units and transfer rejections,
because `9000-ERROR-ROUTINE` increments the same counter from both places.

`PORT0002`'s cost going **negative** on a fee is faithful to `2240-PROCESS-FEE`, which
subtracts unconditionally with no floor. Preserved deliberately rather than "fixed", so the
port stays a port.

### 1.3 Derived audit trail

`2200-UPDATE-POSITIONS` performs `2300-UPDATE-AUDIT-TRAIL` *after* the `EVALUATE`,
unconditionally, so an audit record is written for all five validation-passing cases —
including the two that changed nothing. `AUD-ACTION` comes from a second `EVALUATE` on
`TRN-TYPE`:

| Case | `AUD-ACTION` | `AUD-STATUS` |
|---|---|---|
| TRN-01 `BU` | `CREATE  ` | `SUCC` |
| TRN-02 `SL` | `DELETE  ` | `SUCC` |
| TRN-03 `SL` | `DELETE  ` | `SUCC` |
| TRN-04 `TR` | `UPDATE  ` | `SUCC` |
| TRN-05 `FE` | `UPDATE  ` | `SUCC` |

Two legacy quirks preserved: `BU` is audited as `CREATE` and `SL` as `DELETE` even though
both are updates; and `AUD-STATUS` is `SUCC` whenever `WS-PORT-STATUS` is `'00'`, which
stays `'00'` from the preceding successful read even for the transfer case that did no I/O
at all. So a rejected transaction is still audited as a success. Uses the 392-byte
`AUDITLOG.cpy` layout, not `PORTDEL`'s 80-byte one.

---

## 2. `PORTMSTR`

`PORTMSTR` is a dispatcher; its own logic is the `EVALUATE` on the command area plus the
validation and audit paragraphs. The derived expectation is therefore the routing table
only — the per-action behaviour is already pinned by the executed baselines of `PORTADD`,
`PORTREAD`, `PORTUPDT` and `PORTDEL`.

| Action | Routes to | Expected |
|---|---|---|
| `create` | `2000-CREATE-PORTFOLIO` -> `PORTADD` logic | as `portadd.*` |
| `read` | `3000-READ-PORTFOLIO` -> `PORTREAD` logic | as `portread.stdout.txt` |
| `list` | `2000-PROCESS` (`READ NEXT` loop) | 3 seeded records in `PORT-KEY` order |
| `update` | `4000-UPDATE-PORTFOLIO` -> `PORTUPDT` logic | as `portupdt.*` |
| `delete` | `5000-DELETE-PORTFOLIO` -> `PORTDEL` logic | as `portdel.*` |
| `transaction` | `2200-UPDATE-POSITIONS` -> `PORTTRAN` logic | section 1.2 (DERIVED) |
| anything else | `WHEN OTHER` | `Invalid command`, HTTP 400 |

The dispatcher itself contributes exactly one independently checkable behaviour: the
unknown-command arm. That is the only `PORTMSTR` case the parity report can claim, and it is
DERIVED.
