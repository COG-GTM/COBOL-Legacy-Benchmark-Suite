package com.cognition.clbs.common.copybook.db2;

import com.cognition.clbs.common.cobol.CobolCode;

/** {@code SQL-STATUS-CODES} from {@code SQLCA.cpy}: the SQLSTATE values the programs test for. */
public enum SqlState implements CobolCode {
  SUCCESS("00000"),
  NOT_FOUND("02000"),
  DUPLICATE_KEY("23505"),
  DEADLOCK("40001"),
  TIMEOUT("40003"),
  CONNECTION_ERROR("08001"),
  DB_ERROR("58004");

  /** Width of an SQLSTATE. */
  public static final int LENGTH = 5;

  private final String code;

  SqlState(String code) {
    this.code = code;
  }

  @Override
  public String code() {
    return code;
  }

  /** Resolves a five-character SQLSTATE. */
  public static SqlState fromCode(String code) {
    return CobolCode.fromCode(SqlState.class, code);
  }
}
