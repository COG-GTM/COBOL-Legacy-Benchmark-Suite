package com.portfolio.service;

import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.Portfolio;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Validation Service.
 * Replaces: UTLVAL00.cbl - Data integrity validation routines.
 * Performs cross-system data validation checks.
 */
@Service
public class DataValidationService {

    private static final Logger log = LoggerFactory.getLogger(DataValidationService.class);

    private final PortfolioRepository portfolioRepository;
    private final InvestmentPositionRepository positionRepository;
    private final TransactionHistoryRepository transactionRepository;

    public DataValidationService(PortfolioRepository portfolioRepository,
                                 InvestmentPositionRepository positionRepository,
                                 TransactionHistoryRepository transactionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Validates data integrity across all tables.
     * Replaces the main validation loop in UTLVAL00.cbl.
     */
    public ValidationResult validateAll() {
        log.info("Starting data integrity validation");
        ValidationResult result = new ValidationResult();

        validatePortfolioIntegrity(result);
        validatePositionIntegrity(result);
        validateOrphanedPositions(result);

        log.info("Validation complete: {} errors, {} warnings",
                result.errors.size(), result.warnings.size());
        return result;
    }

    /**
     * Validates portfolio records.
     */
    private void validatePortfolioIntegrity(ValidationResult result) {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        for (Portfolio p : portfolios) {
            if (p.getPortfolioId() == null || p.getPortfolioId().isBlank()) {
                result.errors.add("Portfolio with null/blank ID found");
            }
            if (p.getStatus() == null || !List.of("A", "C", "S").contains(p.getStatus())) {
                result.errors.add("Invalid status '" + p.getStatus()
                        + "' for portfolio " + p.getPortfolioId());
            }
            if ("C".equals(p.getStatus()) && p.getCloseDate() == null) {
                result.warnings.add("Closed portfolio " + p.getPortfolioId()
                        + " has no close date");
            }
        }
        result.portfoliosChecked = portfolios.size();
    }

    /**
     * Validates position records for financial accuracy.
     */
    private void validatePositionIntegrity(ValidationResult result) {
        List<InvestmentPosition> positions = positionRepository.findAll();
        for (InvestmentPosition pos : positions) {
            if (pos.getQuantity() != null && pos.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                result.warnings.add("Negative quantity for position: "
                        + pos.getKey().getPortfolioId() + "/" + pos.getKey().getInvestmentId());
            }
            if (pos.getMarketValue() != null && pos.getMarketValue().compareTo(BigDecimal.ZERO) < 0) {
                result.warnings.add("Negative market value for position: "
                        + pos.getKey().getPortfolioId() + "/" + pos.getKey().getInvestmentId());
            }
        }
        result.positionsChecked = positions.size();
    }

    /**
     * Checks for positions referencing non-existent portfolios.
     */
    private void validateOrphanedPositions(ValidationResult result) {
        List<InvestmentPosition> positions = positionRepository.findAll();
        for (InvestmentPosition pos : positions) {
            if (!portfolioRepository.existsById(pos.getKey().getPortfolioId())) {
                result.errors.add("Orphaned position found: portfolio "
                        + pos.getKey().getPortfolioId() + " does not exist");
            }
        }
    }

    public static class ValidationResult {
        public List<String> errors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public int portfoliosChecked;
        public int positionsChecked;

        public boolean isValid() {
            return errors.isEmpty();
        }
    }
}
