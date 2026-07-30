package com.cognition.portfolio.transaction.exception;

import com.cognition.portfolio.transaction.validation.ValidationOutcome;

/**
 * Raised when {@code PORTTRAN 2100-VALIDATE-TRANSACTION} would set {@code ERR-TEXT} and increment
 * the error counter. Carries the COBOL error text verbatim.
 */
public class TransactionValidationException extends RuntimeException {

  private final transient ValidationOutcome outcome;

  public TransactionValidationException(ValidationOutcome outcome) {
    super(outcome.message());
    this.outcome = outcome;
  }

  public ValidationOutcome getOutcome() {
    return outcome;
  }
}
