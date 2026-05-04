package com.coggtm.portfolio.service;

import com.coggtm.portfolio.domain.Portfolio;

import java.util.Optional;

/**
 * Portfolio CRUD operations — maps to PORTMSTR.cbl EVALUATE block.
 *
 * <p>COBOL source: {@code src/programs/portfolio/PORTMSTR.cbl}</p>
 */
public interface PortfolioService {

    Portfolio createPortfolio(Portfolio portfolio);

    Optional<Portfolio> readPortfolio(String portfolioId);

    Portfolio updatePortfolio(Portfolio portfolio);

    void deletePortfolio(String portfolioId);
}
