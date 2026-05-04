package com.portfolio.service.portfolio;

import com.portfolio.domain.Portfolio;
import com.portfolio.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Portfolio Master Service - migrated from COBOL PORTMSTR.cbl.
 * Main portfolio management orchestrator.
 * Routes CRUD commands similar to COBOL EVALUATE TRUE on LS-COMMAND.
 */
@Service
public class PortfolioMasterService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioMasterService.class);

    private final PortfolioService portfolioService;
    private final PortfolioTransactionService transactionService;

    public PortfolioMasterService(PortfolioService portfolioService,
                                  PortfolioTransactionService transactionService) {
        this.portfolioService = portfolioService;
        this.transactionService = transactionService;
    }

    public Portfolio executeCommand(char command, Portfolio portfolio, String userId) {
        return switch (command) {
            case 'C' -> portfolioService.createPortfolio(portfolio);
            case 'R' -> portfolioService.getPortfolio(portfolio.getPortfolioId())
                    .orElseThrow(() -> new ProcessingException(
                            "Portfolio not found: " + portfolio.getPortfolioId()));
            case 'U' -> portfolioService.updatePortfolio(portfolio);
            case 'D' -> {
                portfolioService.deletePortfolio(portfolio.getPortfolioId(), userId);
                yield portfolio;
            }
            default -> throw new ProcessingException("Invalid command: " + command);
        };
    }

    public Optional<Portfolio> findPortfolio(String portfolioId) {
        return portfolioService.getPortfolio(portfolioId);
    }

    public List<Portfolio> listActivePortfolios() {
        return portfolioService.getActivePortfolios();
    }
}
