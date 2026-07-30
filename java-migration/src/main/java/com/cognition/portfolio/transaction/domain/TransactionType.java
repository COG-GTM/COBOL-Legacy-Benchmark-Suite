package com.cognition.portfolio.transaction.domain;

import com.cognition.portfolio.traceability.CobolOrigin;
import java.util.Optional;

/**
 * Transaction type, from the 88-level condition names on {@code TRN-TYPE PIC X(02)} in
 * {@code TRNREC.cpy}.
 *
 * <pre>
 * 88  TRN-TYPE-BUY     VALUE 'BU'.
 * 88  TRN-TYPE-SELL    VALUE 'SL'.
 * 88  TRN-TYPE-TRANS   VALUE 'TR'.
 * 88  TRN-TYPE-FEE     VALUE 'FE'.
 * </pre>
 */
@CobolOrigin(program = "TRNREC", paragraph = "TRN-TYPE 88-levels", rules = {"BR-03"})
public enum TransactionType {

  /** {@code 88 TRN-TYPE-BUY VALUE 'BU'} — audit action {@code CREATE} (PORTTRAN 2300). */
  BUY("BU", "CREATE"),

  /** {@code 88 TRN-TYPE-SELL VALUE 'SL'} — audit action {@code DELETE} (PORTTRAN 2300). */
  SELL("SL", "DELETE"),

  /** {@code 88 TRN-TYPE-TRANS VALUE 'TR'} — audit action {@code UPDATE} (PORTTRAN 2300). */
  TRANSFER("TR", "UPDATE"),

  /** {@code 88 TRN-TYPE-FEE VALUE 'FE'} — audit action {@code UPDATE} (PORTTRAN 2300). */
  FEE("FE", "UPDATE");

  private final String code;
  private final String auditAction;

  TransactionType(String code, String auditAction) {
    this.code = code;
    this.auditAction = auditAction;
  }

  /** The two-character value stored in {@code TRN-TYPE}. */
  public String getCode() {
    return code;
  }

  /**
   * Audit action written by {@code PORTTRAN 2300-UPDATE-AUDIT-TRAIL} for this transaction type.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2300-UPDATE-AUDIT-TRAIL", rules = {"BR-13"})
  public String getAuditAction() {
    return auditAction;
  }

  /**
   * Resolves a {@code TRN-TYPE} value; empty when the value is outside the 88-level list, which is
   * the {@code WHEN OTHER} branch of {@code PORTTRAN 2120-CHECK-TRANSACTION-TYPE}.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2120-CHECK-TRANSACTION-TYPE", rules = {"BR-03"})
  public static Optional<TransactionType> fromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    String trimmed = code.trim();
    for (TransactionType type : values()) {
      if (type.code.equals(trimmed)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
