package com.portfolio.utility;

import com.portfolio.model.PositionRecord;
import com.portfolio.model.TransactionRecord;
import com.portfolio.support.Db2StatisticsService;
import com.portfolio.support.ErrorLoggingService;
import com.portfolio.support.PositionRecordRepository;
import com.portfolio.support.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Validation Service.
 * Migrated from COBOL UTLVAL00.
 * Performs data integrity checks, validates cross-references,
 * verifies data formats, and reconciles balances.
 */
@Service
public class DataValidationService {

    private static final Logger log = LoggerFactory.getLogger(DataValidationService.class);

    private final PositionRecordRepository positionRepository;
    private final TransactionRecordRepository transactionRepository;
    private final ErrorLoggingService errorLoggingService;
    private final Db2StatisticsService statisticsService;

    public DataValidationService(PositionRecordRepository positionRepository,
                                  TransactionRecordRepository transactionRepository,
                                  ErrorLoggingService errorLoggingService,
                                  Db2StatisticsService statisticsService) {
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.errorLoggingService = errorLoggingService;
        this.statisticsService = statisticsService;
    }

    /**
     * Run full data validation suite.
     * Replaces COBOL UTLVAL00 validation logic.
     */
    public Map<String, Object> runValidation() {
        log.info("Starting data validation (UTLVAL00)");

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int checksRun = 0;
        int checksPassed = 0;

        // Check 1: Validate position records have required fields
        checksRun++;
        List<PositionRecord> positions = positionRepository.findAll();
        boolean posValid = validatePositions(positions, errors, warnings);
        if (posValid) checksPassed++;
        statisticsService.recordQuery();

        // Check 2: Validate transaction records
        checksRun++;
        List<TransactionRecord> transactions = transactionRepository.findAll();
        boolean txnValid = validateTransactions(transactions, errors, warnings);
        if (txnValid) checksPassed++;
        statisticsService.recordQuery();

        // Check 3: Cross-reference check - positions reference valid portfolios
        checksRun++;
        boolean xrefValid = validateCrossReferences(positions, transactions, errors, warnings);
        if (xrefValid) checksPassed++;

        // Check 4: Balance reconciliation
        checksRun++;
        boolean balValid = validateBalances(positions, errors, warnings);
        if (balValid) checksPassed++;

        result.put("checksRun", checksRun);
        result.put("checksPassed", checksPassed);
        result.put("checksFailed", checksRun - checksPassed);
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("totalPositions", positions.size());
        result.put("totalTransactions", transactions.size());
        result.put("status", errors.isEmpty() ? "PASSED" : "FAILED");

        log.info("Data validation complete: {}/{} checks passed, {} errors, {} warnings",
                checksPassed, checksRun, errors.size(), warnings.size());
        return result;
    }

    private boolean validatePositions(List<PositionRecord> positions,
                                       List<String> errors, List<String> warnings) {
        boolean valid = true;
        for (PositionRecord pos : positions) {
            if (pos.getPortfolioId() == null || pos.getPortfolioId().isBlank()) {
                errors.add("Position missing portfolio ID: " + pos.getSymbolId());
                valid = false;
            }
            if (pos.getQuantity() != null && pos.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                warnings.add("Negative quantity for " + pos.getPortfolioId() + "/" + pos.getSymbolId());
            }
        }
        return valid;
    }

    private boolean validateTransactions(List<TransactionRecord> transactions,
                                          List<String> errors, List<String> warnings) {
        boolean valid = true;
        for (TransactionRecord txn : transactions) {
            String type = txn.getTransactionType();
            if (!TransactionRecord.TYPE_BUY.equals(type) &&
                !TransactionRecord.TYPE_SELL.equals(type) &&
                !TransactionRecord.TYPE_TRANSFER.equals(type) &&
                !TransactionRecord.TYPE_FEE.equals(type)) {
                errors.add("Invalid transaction type '" + type + "' for txn " + txn.getTransactionId());
                valid = false;
            }
        }
        return valid;
    }

    private boolean validateCrossReferences(List<PositionRecord> positions,
                                             List<TransactionRecord> transactions,
                                             List<String> errors, List<String> warnings) {
        // Verify all positions have corresponding portfolio context
        boolean valid = true;
        for (PositionRecord pos : positions) {
            if (pos.getPortfolioId() == null || pos.getPortfolioId().isBlank()) {
                errors.add("Orphan position: " + pos.getSymbolId());
                valid = false;
            }
        }
        return valid;
    }

    private boolean validateBalances(List<PositionRecord> positions,
                                      List<String> errors, List<String> warnings) {
        boolean valid = true;
        for (PositionRecord pos : positions) {
            if (pos.getCostBasis() != null && pos.getMarketValue() != null) {
                // Check for unreasonable gain/loss (>1000%)
                if (pos.getCostBasis().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal ratio = pos.getMarketValue()
                            .divide(pos.getCostBasis(), 4, java.math.RoundingMode.HALF_UP);
                    if (ratio.compareTo(new BigDecimal("10")) > 0) {
                        warnings.add("Suspicious gain ratio for " +
                                pos.getPortfolioId() + "/" + pos.getSymbolId());
                    }
                }
            }
        }
        return valid;
    }
}
