package com.cognition.portfolio.transaction.service;

import com.cognition.portfolio.traceability.CobolOrigin;
import com.cognition.portfolio.transaction.domain.PortfolioPostingEffect;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;

/**
 * Outcome of processing one transaction: the stored record with its new {@code TRN-STATUS}, the
 * posting effect on the portfolio master, the audit trail entry {@code 2300-UPDATE-AUDIT-TRAIL}
 * would have written, and the COBOL error text when the record failed.
 *
 * @param transaction the persisted transaction after processing
 * @param effect deltas for {@code PORT-TOTAL-UNITS} / {@code PORT-TOTAL-COST}; null when failed
 * @param auditAction {@code AUD-ACTION} for the record; null when 2200 was never reached
 * @param auditStatus {@code AUD-STATUS}, {@code SUCC} or {@code FAIL}; null when 2200 was never
 *     reached
 * @param errorText the {@code ERR-TEXT} the COBOL would have produced; null when successful
 * @param ruleId business rule that rejected the record, e.g. {@code BR-10}
 * @param cobolParagraph paragraph that produced the outcome
 */
@CobolOrigin(program = "PORTTRAN", paragraph = "2100-VALIDATE-TRANSACTION")
public record TransactionProcessingResult(
    PortfolioTransaction transaction,
    PortfolioPostingEffect effect,
    String auditAction,
    String auditStatus,
    String errorText,
    String ruleId,
    String cobolParagraph) {

  /** {@code AUD-STATUS} value written when {@code WS-PORT-STATUS = '00'}. */
  public static final String AUDIT_SUCCESS = "SUCC";

  /** {@code AUD-STATUS} value written for any other portfolio status. */
  public static final String AUDIT_FAILURE = "FAIL";

  /**
   * Record accepted by {@code 2100-VALIDATE-TRANSACTION} in the batch driver, which counts it in
   * {@code WS-PROCESS-COUNT} without ever reaching 2200/2300.
   */
  public static TransactionProcessingResult validated(PortfolioTransaction transaction) {
    return new TransactionProcessingResult(
        transaction, null, null, null, null, null, "PORTTRAN 2100-VALIDATE-TRANSACTION");
  }

  /** Record rejected by {@code 2100-VALIDATE-TRANSACTION}, which branches to 9000 and skips 2300. */
  public static TransactionProcessingResult validationFailure(
      PortfolioTransaction transaction, String errorText, String ruleId, String paragraph) {
    return new TransactionProcessingResult(
        transaction, null, null, null, errorText, ruleId, paragraph);
  }

  /** Posting applied by {@code 2200-UPDATE-POSITIONS}; 2300 writes {@code AUD-STATUS = 'SUCC'}. */
  public static TransactionProcessingResult postingSuccess(
      PortfolioTransaction transaction, PortfolioPostingEffect effect) {
    return new TransactionProcessingResult(
        transaction,
        effect,
        effect.auditAction(),
        AUDIT_SUCCESS,
        null,
        null,
        "PORTTRAN 2200-UPDATE-POSITIONS");
  }

  /**
   * Posting rejected inside {@code 2210}-{@code 2240}. {@code 2200-UPDATE-POSITIONS} performs
   * {@code 2300-UPDATE-AUDIT-TRAIL} unconditionally, so the per-type {@code AUD-ACTION} is still
   * recorded, with {@code AUD-STATUS = 'FAIL'} (BR-13).
   */
  public static TransactionProcessingResult postingFailure(
      PortfolioTransaction transaction, String errorText, String ruleId, String paragraph) {
    return new TransactionProcessingResult(
        transaction,
        null,
        transaction.getTrnType().getAuditAction(),
        AUDIT_FAILURE,
        errorText,
        ruleId,
        paragraph);
  }

  /** True when the record was counted by {@code WS-PROCESS-COUNT} rather than {@code WS-ERROR-COUNT}. */
  public boolean isProcessed() {
    return errorText == null;
  }
}
