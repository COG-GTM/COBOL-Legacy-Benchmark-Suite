package com.cognition.portfolio.transaction.validation;

import com.cognition.portfolio.traceability.CobolOrigin;

/**
 * Result of a validation, equivalent to the {@code LS-RETURN-CODE} / {@code LS-ERROR-MSG} pair
 * returned by {@code PORTVALD}, or to {@code ERR-TEXT} being set inside
 * {@code PORTTRAN 2100-VALIDATE-TRANSACTION}.
 *
 * @param returnCode COBOL return code
 * @param message the COBOL error text, verbatim; blank when valid
 * @param ruleId business rule identifier from MIGRATION-NOTES.md, e.g. {@code BR-04}
 * @param cobolParagraph the paragraph that produced this outcome
 */
@CobolOrigin(program = "PORTVALD", paragraph = "LINKAGE LS-VALIDATION-REQUEST")
public record ValidationOutcome(
    ValidationReturnCode returnCode, String message, String ruleId, String cobolParagraph) {

  /** {@code MOVE VAL-SUCCESS TO LS-RETURN-CODE / MOVE SPACES TO LS-ERROR-MSG}. */
  public static ValidationOutcome success(String cobolParagraph) {
    return new ValidationOutcome(ValidationReturnCode.SUCCESS, "", null, cobolParagraph);
  }

  /** A failure carrying the COBOL error text verbatim. */
  public static ValidationOutcome failure(
      ValidationReturnCode returnCode, String message, String ruleId, String cobolParagraph) {
    return new ValidationOutcome(returnCode, message, ruleId, cobolParagraph);
  }

  /** True when {@code ERR-TEXT = SPACES} / {@code LS-RETURN-CODE = VAL-SUCCESS}. */
  public boolean isValid() {
    return returnCode == ValidationReturnCode.SUCCESS;
  }
}
