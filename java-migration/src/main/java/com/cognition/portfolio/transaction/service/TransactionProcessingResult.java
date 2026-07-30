package com.cognition.portfolio.transaction.service;

import com.cognition.portfolio.traceability.CobolOrigin;
import com.cognition.portfolio.transaction.domain.PortfolioPostingEffect;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;

/**
 * Outcome of processing one transaction: the stored record with its new {@code TRN-STATUS}, the
 * posting effect on the portfolio master, and the COBOL error text when the record failed.
 *
 * @param transaction the persisted transaction after processing
 * @param effect deltas for {@code PORT-TOTAL-UNITS} / {@code PORT-TOTAL-COST}; null when failed
 * @param errorText the {@code ERR-TEXT} the COBOL would have produced; null when successful
 * @param ruleId business rule that rejected the record, e.g. {@code BR-10}
 * @param cobolParagraph paragraph that produced the outcome
 */
@CobolOrigin(program = "PORTTRAN", paragraph = "2100-VALIDATE-TRANSACTION")
public record TransactionProcessingResult(
    PortfolioTransaction transaction,
    PortfolioPostingEffect effect,
    String errorText,
    String ruleId,
    String cobolParagraph) {

  /** True when the record was counted by {@code WS-PROCESS-COUNT} rather than {@code WS-ERROR-COUNT}. */
  public boolean isProcessed() {
    return errorText == null;
  }
}
