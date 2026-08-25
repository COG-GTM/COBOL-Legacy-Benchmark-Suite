# `PORTVALD` -> `modernized/src/validation.js`

Validation subroutine. Baseline: **EXECUTED** — compiled to `PORTVALD.so` and `CALL`ed for all
20 golden vectors by `golden/cobol/VALDRV.cbl`. This is the program where executing the legacy
code mattered most, because **the documented rules and the actual behaviour disagree.**

## Before

`src/programs/portfolio/PORTVALD.cbl`, `1000-VALIDATE-ID` and `2000-VALIDATE-ACCOUNT`:

```cobol
1000-VALIDATE-ID.
    IF LS-INPUT-VALUE(1:4) NOT = 'PORT'
        MOVE VAL-RC-INVALID-ID TO LS-RETURN-CODE
        MOVE VAL-ERR-ID        TO LS-ERROR-MSG
        EXIT PARAGRAPH
    END-IF
    MOVE LS-INPUT-VALUE(5:4) TO VAL-NUMERIC-CHECK
    IF VAL-NUMERIC-CHECK IS NOT NUMERIC
        MOVE VAL-RC-INVALID-ID TO LS-RETURN-CODE
        MOVE VAL-ERR-ID        TO LS-ERROR-MSG
    END-IF
    .

2000-VALIDATE-ACCOUNT.
    IF LS-INPUT-VALUE IS NOT NUMERIC OR LS-INPUT-VALUE = ZEROS
        MOVE VAL-RC-INVALID-ACCT TO LS-RETURN-CODE
        MOVE VAL-ERR-ACCT        TO LS-ERROR-MSG
    END-IF
    .
```

Both are unreachable-by-construction:

- `VAL-NUMERIC-CHECK` is `PIC X(10)`. Moving 4 characters into it left-justifies and
  space-pads to `"0001      "`. `IS NUMERIC` on an alphanumeric item requires *every*
  character to be a digit, so the padding fails the test for every input. **A well-formed
  `PORT0001` is rejected.**
- `LS-INPUT-VALUE` is `PIC X(50)`. A 10-digit account number followed by 40 spaces is never
  numeric, so **every account is rejected.**

Amount validation is the mirror image: `VAL-MIN-AMOUNT` / `VAL-MAX-AMOUNT` are
`-9999999999999.99` / `+9999999999999.99`, i.e. the field's own representable range, so the
range test can never fail. `NOTANUM` passes. `VAL-ERR-AMT` ("Amount outside valid range") is
dead text the program can never emit.

Only type validation works — and it works correctly for all four documented types, because
comparing `X(50)` against `'STK'` space-pads the literal.

## After

`modernized/src/validation.js` runs in two modes, so the port can be *proved* against
observed behaviour while still offering the intended rules:

```javascript
function validateLegacy(type, input) {
  const value = String(input ?? '');
  switch (String(type || '')) {
    case 'I': return result(RETURN_CODES.id, MESSAGES.id);          // always rejects
    case 'A': return result(RETURN_CODES.account, MESSAGES.account); // always rejects
    case 'T': return ['STK', 'BND', 'MMF', 'ETF'].includes(value.trim())
      ? result(RETURN_CODES.success) : result(RETURN_CODES.type, MESSAGES.type);
    case 'M': return result(RETURN_CODES.success);                   // always accepts
    default: return result(RETURN_CODES.id, MESSAGES.unknown);
  }
}
```

`validateModernized` applies the documented intent: `/^PORT[0-9]{4}$/`, ten digits and not all
zeros, type in the same four-value set, and a `Decimal` amount inside `VAL-MIN`/`VAL-MAX`.

The unconditional returns in the legacy arms look like stubs. They are not — they are the
literal behaviour, and writing them as one-liners with the reason recorded in
`modernized/CONTRACTS.md` section 4.2 is more honest than re-implementing the broken
`IS NUMERIC` dance and hoping it reproduces the same outcome.

## Parity

All 20 vectors, compared to the real `CALL`:

```
VALD-01..05   PORTVALD    EXECUTED  type I                                                PASS
VALD-06..09   PORTVALD    EXECUTED  type A                                                PASS
VALD-10..15   PORTVALD    EXECUTED  type T                                                PASS
VALD-16..19   PORTVALD    EXECUTED  type M                                                PASS
VALD-20       PORTVALD    EXECUTED  unknown validation type                               PASS
```

Return code and error message are both compared, verbatim, per vector. The baseline file
`golden/expected/portvald.txt` is committed, and the JS test parses that same file rather than
a transcription of it — if the module and the executed COBOL disagree, the module is wrong.

## Known intentional divergences

The two modes differ on exactly three validation types. `T` and the unknown-type arm are
identical in both.

| Id | Type | Why |
|---|---|---|
| `DIV-VALD-I` | `I` | Legacy rejects every ID via the `X(10)` space-padding bug; modernized applies `PORT` + 4 digits. |
| `DIV-VALD-A` | `A` | Legacy tests all 50 bytes for `NUMERIC`; modernized applies 10 digits, not all zeros. |
| `DIV-VALD-M` | `M` | Legacy bounds are the field's own range, so even non-numeric text passes; modernized requires a numeric value in range. |

These are listed in the parity report as intentional. Any divergence *not* on that list fails
the build.
