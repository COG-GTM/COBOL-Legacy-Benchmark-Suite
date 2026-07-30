package com.cognition.portfolio.transaction.domain;

import com.cognition.portfolio.traceability.CobolOrigin;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Transaction status, from the 88-level condition names on {@code TRN-STATUS PIC X(01)} in
 * {@code TRNREC.cpy}.
 *
 * <pre>
 * 88  TRN-STATUS-PEND  VALUE 'P'.
 * 88  TRN-STATUS-DONE  VALUE 'D'.
 * 88  TRN-STATUS-FAIL  VALUE 'F'.
 * 88  TRN-STATUS-REV   VALUE 'R'.
 * </pre>
 *
 * <p>The legacy programs never assign {@code TRN-STATUS}; the allowed transitions below are
 * <em>derived</em> from how {@code PORTTRAN 2100-VALIDATE-TRANSACTION} counts processed versus
 * failed records. See "Open questions for the legacy owners" in MIGRATION-NOTES.md.
 */
@CobolOrigin(program = "TRNREC", paragraph = "TRN-STATUS 88-levels", rules = {"BR-23"}, derived = true)
public enum TransactionStatus {

  /** {@code 88 TRN-STATUS-PEND VALUE 'P'}. */
  PENDING("P"),

  /** {@code 88 TRN-STATUS-DONE VALUE 'D'}. */
  DONE("D"),

  /** {@code 88 TRN-STATUS-FAIL VALUE 'F'}. */
  FAILED("F"),

  /** {@code 88 TRN-STATUS-REV VALUE 'R'}. */
  REVERSED("R");

  private final String code;

  TransactionStatus(String code) {
    this.code = code;
  }

  /** The one-character value stored in {@code TRN-STATUS}. */
  public String getCode() {
    return code;
  }

  /** Resolves a {@code TRN-STATUS} value; empty when outside the 88-level list. */
  public static Optional<TransactionStatus> fromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    String trimmed = code.trim();
    for (TransactionStatus status : values()) {
      if (status.code.equals(trimmed)) {
        return Optional.of(status);
      }
    }
    return Optional.empty();
  }

  /**
   * Statuses reachable from this one.
   *
   * <p>Derived rule BR-23: a pending transaction either completes ({@code D}) or fails ({@code F});
   * only a completed transaction can be reversed ({@code R}); {@code F} and {@code R} are terminal.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2100-VALIDATE-TRANSACTION", rules = {"BR-23"}, derived = true)
  public Set<TransactionStatus> allowedTransitions() {
    return switch (this) {
      case PENDING -> EnumSet.of(DONE, FAILED);
      case DONE -> EnumSet.of(REVERSED);
      case FAILED, REVERSED -> EnumSet.noneOf(TransactionStatus.class);
    };
  }

  /** Whether a transition from this status to {@code target} is permitted by BR-23. */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2100-VALIDATE-TRANSACTION", rules = {"BR-23"}, derived = true)
  public boolean canTransitionTo(TransactionStatus target) {
    return target != null && allowedTransitions().contains(target);
  }
}
