package com.coggtm.portfolio.service;

import java.math.BigDecimal;

/**
 * Validation subroutines — maps to PORTVALD.cbl.
 *
 * <p>COBOL source: {@code src/programs/portfolio/PORTVALD.cbl}</p>
 * <p>Validation constants from: {@code src/copybook/common/PORTVAL.cpy}</p>
 */
public interface ValidationService {

    boolean validatePortfolioId(String portfolioId);

    boolean validateAccount(String accountNo);

    boolean validateInvestmentType(String investmentType);

    boolean validateAmount(BigDecimal amount);
}
