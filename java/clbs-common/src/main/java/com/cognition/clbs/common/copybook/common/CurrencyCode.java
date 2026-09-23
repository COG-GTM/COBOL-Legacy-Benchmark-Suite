package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCode;

/** {@code CURRENCY-CODES} from {@code COMMON.cpy}: the ISO 4217 codes the system trades in. */
public enum CurrencyCode implements CobolCode {
  USD,
  EUR,
  GBP,
  JPY,
  CAD;

  @Override
  public String code() {
    return name();
  }

  /** Resolves a {@code PIC X(03)} currency code. */
  public static CurrencyCode fromCode(String code) {
    return CobolCode.fromCode(CurrencyCode.class, code);
  }
}
