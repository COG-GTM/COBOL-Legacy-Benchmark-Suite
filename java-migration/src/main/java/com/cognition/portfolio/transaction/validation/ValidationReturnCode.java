package com.cognition.portfolio.transaction.validation;

import com.cognition.portfolio.traceability.CobolOrigin;

/**
 * Return codes from {@code PORTVAL.cpy} {@code 01 VAL-RETURN-CODES}, passed back through
 * {@code LS-RETURN-CODE} by {@code PORTVALD}.
 */
@CobolOrigin(program = "PORTVAL", paragraph = "01 VAL-RETURN-CODES")
public enum ValidationReturnCode {

  /** {@code VAL-SUCCESS PIC S9(4) VALUE +0}. */
  SUCCESS(0),

  /** {@code VAL-INVALID-ID PIC S9(4) VALUE +1}. */
  INVALID_ID(1),

  /** {@code VAL-INVALID-ACCT PIC S9(4) VALUE +2}. */
  INVALID_ACCT(2),

  /** {@code VAL-INVALID-TYPE PIC S9(4) VALUE +3}. */
  INVALID_TYPE(3),

  /** {@code VAL-INVALID-AMT PIC S9(4) VALUE +4}. */
  INVALID_AMT(4);

  private final int code;

  ValidationReturnCode(int code) {
    this.code = code;
  }

  /** Numeric value moved into {@code LS-RETURN-CODE}. */
  public int getCode() {
    return code;
  }
}
