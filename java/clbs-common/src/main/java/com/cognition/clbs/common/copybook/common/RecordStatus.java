package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCode;

/**
 * {@code STATUS-CODES} from {@code COMMON.cpy}: the one-byte status shared by portfolio, position
 * and transaction records.
 */
public enum RecordStatus implements CobolCode {
  ACTIVE("A"),
  CLOSED("C"),
  PENDING("P"),
  SUSPENDED("S"),
  FAILED("F"),
  REVERSED("R");

  private final String code;

  RecordStatus(String code) {
    this.code = code;
  }

  @Override
  public String code() {
    return code;
  }

  /** Resolves a {@code PIC X(01)} status code. */
  public static RecordStatus fromCode(String code) {
    return CobolCode.fromCode(RecordStatus.class, code);
  }
}
