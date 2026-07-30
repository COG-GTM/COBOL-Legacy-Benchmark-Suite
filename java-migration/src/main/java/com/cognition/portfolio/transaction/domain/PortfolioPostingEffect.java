package com.cognition.portfolio.transaction.domain;

import com.cognition.portfolio.traceability.CobolOrigin;
import java.math.BigDecimal;

/**
 * The change a transaction applies to the portfolio master record, i.e. the deltas that
 * {@code PORTTRAN 2210-PROCESS-BUY}, {@code 2220-PROCESS-SELL} and {@code 2240-PROCESS-FEE} write
 * back to {@code PORT-TOTAL-UNITS} / {@code PORT-TOTAL-COST} before the {@code REWRITE}.
 *
 * <p>Modelling the effect as a value object keeps the transaction service free of a dependency on
 * the (not yet migrated) portfolio master entity while preserving the arithmetic exactly.
 *
 * @param unitsDelta amount added to {@code PORT-TOTAL-UNITS}
 * @param costDelta amount added to {@code PORT-TOTAL-COST}
 * @param auditAction audit action recorded by {@code PORTTRAN 2300-UPDATE-AUDIT-TRAIL}
 */
@CobolOrigin(program = "PORTTRAN", paragraph = "2200-UPDATE-POSITIONS", rules = {"BR-09", "BR-10", "BR-12"})
public record PortfolioPostingEffect(BigDecimal unitsDelta, BigDecimal costDelta, String auditAction) {}
