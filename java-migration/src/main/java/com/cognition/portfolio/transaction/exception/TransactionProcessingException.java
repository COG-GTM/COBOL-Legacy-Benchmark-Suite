package com.cognition.portfolio.transaction.exception;

/**
 * Raised where the COBOL would {@code MOVE ... TO ERR-TEXT} and {@code PERFORM
 * 9000-ERROR-ROUTINE}: the transaction is counted as an error and processing of that record stops.
 */
public class TransactionProcessingException extends RuntimeException {

  private final String ruleId;
  private final String cobolParagraph;

  public TransactionProcessingException(String message, String ruleId, String cobolParagraph) {
    super(message);
    this.ruleId = ruleId;
    this.cobolParagraph = cobolParagraph;
  }

  /** Business rule identifier from MIGRATION-NOTES.md. */
  public String getRuleId() {
    return ruleId;
  }

  /** COBOL paragraph that produced the error text. */
  public String getCobolParagraph() {
    return cobolParagraph;
  }
}
