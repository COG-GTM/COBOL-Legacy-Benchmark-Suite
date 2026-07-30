package com.cognition.portfolio.transaction.validation;

import com.cognition.portfolio.traceability.CobolOrigin;

/**
 * Stands in for the {@code READ PORTFOLIO-FILE ... INVALID KEY} performed by
 * {@code PORTTRAN 2110-CHECK-PORTFOLIO}: the existence check against the portfolio master
 * ({@code PORTMSTR}), which is a separate entity and not part of this migration slice.
 *
 * <p>The default implementation is {@link FormatOnlyPortfolioReferenceValidator}; a deployment that
 * has the portfolio service available replaces it with a real lookup.
 */
@FunctionalInterface
@CobolOrigin(program = "PORTTRAN", paragraph = "2110-CHECK-PORTFOLIO", rules = {"BR-02"})
public interface PortfolioReferenceValidator {

  /** True when a portfolio record exists for the given {@code PORT-ID}. */
  boolean exists(String portfolioId);
}
