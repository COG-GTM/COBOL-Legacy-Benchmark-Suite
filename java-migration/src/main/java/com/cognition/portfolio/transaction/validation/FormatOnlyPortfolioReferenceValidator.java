package com.cognition.portfolio.transaction.validation;

import com.cognition.portfolio.traceability.CobolOrigin;

/**
 * Default {@link PortfolioReferenceValidator} used while the portfolio master
 * ({@code PORTMSTR} / {@code PORTFLIO.cpy}) has not been migrated: it accepts any portfolio id that
 * satisfies the format rule of {@code PORTVALD 1000-VALIDATE-ID} (BR-15).
 *
 * <p>This is a deliberate narrowing of BR-02 — recorded as open question OQ-2 — because the record
 * existence check needs the portfolio table.
 */
@CobolOrigin(program = "PORTTRAN", paragraph = "2110-CHECK-PORTFOLIO", rules = {"BR-02"}, derived = true)
public class FormatOnlyPortfolioReferenceValidator implements PortfolioReferenceValidator {

  private final PortfolioFieldValidator fieldValidator;

  public FormatOnlyPortfolioReferenceValidator(PortfolioFieldValidator fieldValidator) {
    this.fieldValidator = fieldValidator;
  }

  @Override
  @CobolOrigin(program = "PORTTRAN", paragraph = "2110-CHECK-PORTFOLIO", rules = {"BR-02"}, derived = true)
  public boolean exists(String portfolioId) {
    return fieldValidator.validatePortfolioId(portfolioId).isValid();
  }
}
