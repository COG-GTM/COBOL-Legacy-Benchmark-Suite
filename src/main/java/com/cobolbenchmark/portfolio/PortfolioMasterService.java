package com.cobolbenchmark.portfolio;

import com.cobolbenchmark.common.DuplicateRecordException;
import com.cobolbenchmark.common.RecordNotFoundException;
import com.cobolbenchmark.db.PortfolioMasterRepository;
import com.cobolbenchmark.model.PortfolioMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Portfolio Master Service - migrated from PORTMSTR.cbl.
 * C/R/U/D operations on portfolio master records.
 * Replaces VSAM file operations with repository calls.
 * Validates status as one of 'A'/'I'/'C' (VALID-STATUS VALUE 'A' 'I' 'C').
 */
@Service
@Transactional
public class PortfolioMasterService {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioMasterService.class);

    private final PortfolioMasterRepository portfolioMasterRepository;

    public PortfolioMasterService(PortfolioMasterRepository portfolioMasterRepository) {
        this.portfolioMasterRepository = portfolioMasterRepository;
    }

    /**
     * Create a new portfolio - replaces VSAM WRITE operation.
     */
    public PortfolioMaster createPortfolio(PortfolioMaster portfolio) {
        logger.info("Creating portfolio: {}", portfolio.getPortfolioId());

        validateStatus(portfolio.getStatus());

        if (portfolioMasterRepository.existsById(portfolio.getPortfolioId())) {
            throw new DuplicateRecordException("Portfolio", portfolio.getPortfolioId());
        }

        portfolio.setLastMaintDate(Timestamp.from(Instant.now()));
        return portfolioMasterRepository.save(portfolio);
    }

    /**
     * Read a portfolio by ID - replaces VSAM READ operation.
     */
    public PortfolioMaster readPortfolio(String portfolioId) {
        logger.debug("Reading portfolio: {}", portfolioId);
        return portfolioMasterRepository.findById(portfolioId)
                .orElseThrow(() -> new RecordNotFoundException("Portfolio", portfolioId));
    }

    /**
     * Update an existing portfolio - replaces VSAM REWRITE operation.
     */
    public PortfolioMaster updatePortfolio(String portfolioId, PortfolioMaster updates) {
        logger.info("Updating portfolio: {}", portfolioId);

        validateStatus(updates.getStatus());

        PortfolioMaster existing = portfolioMasterRepository.findById(portfolioId)
                .orElseThrow(() -> new RecordNotFoundException("Portfolio", portfolioId));

        existing.setPortfolioName(updates.getPortfolioName());
        existing.setStatus(updates.getStatus());
        existing.setCurrencyCode(updates.getCurrencyCode());
        existing.setRiskLevel(updates.getRiskLevel());
        existing.setCloseDate(updates.getCloseDate());
        existing.setLastMaintDate(Timestamp.from(Instant.now()));
        existing.setLastMaintUser(updates.getLastMaintUser());

        return portfolioMasterRepository.save(existing);
    }

    /**
     * Delete a portfolio - replaces VSAM DELETE operation.
     */
    public void deletePortfolio(String portfolioId) {
        logger.info("Deleting portfolio: {}", portfolioId);

        if (!portfolioMasterRepository.existsById(portfolioId)) {
            throw new RecordNotFoundException("Portfolio", portfolioId);
        }
        portfolioMasterRepository.deleteById(portfolioId);
    }

    /**
     * List portfolios by status.
     */
    public List<PortfolioMaster> findByStatus(String status) {
        validateStatus(status);
        return portfolioMasterRepository.findByStatus(status);
    }

    /**
     * List portfolios by client ID.
     */
    public List<PortfolioMaster> findByClientId(String clientId) {
        return portfolioMasterRepository.findByClientId(clientId);
    }

    /**
     * Validate portfolio status - from PORTMSTR.cbl VALID-STATUS VALUE 'A' 'I' 'C'.
     */
    private void validateStatus(String status) {
        if (status == null || !("A".equals(status) || "I".equals(status) || "C".equals(status))) {
            throw new IllegalArgumentException("Invalid portfolio status: " + status + ". Must be A, I, or C");
        }
    }
}
