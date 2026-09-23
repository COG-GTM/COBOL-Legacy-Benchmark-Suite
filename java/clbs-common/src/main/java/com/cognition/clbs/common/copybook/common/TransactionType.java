package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCode;

/** {@code TRANSACTION-TYPES} from {@code COMMON.cpy}. */
public enum TransactionType implements CobolCode {
  BUY("BU"),
  SELL("SL"),
  TRANSFER("TR"),
  FEE("FE");

  private final String code;

  TransactionType(String code) {
    this.code = code;
  }

  @Override
  public String code() {
    return code;
  }

  /** Resolves a {@code PIC X(02)} transaction type. */
  public static TransactionType fromCode(String code) {
    return CobolCode.fromCode(TransactionType.class, code);
  }
}
