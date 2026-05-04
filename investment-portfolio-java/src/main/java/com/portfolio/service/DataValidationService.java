package com.portfolio.service;

import com.portfolio.entity.InvestmentPosition;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.entity.TransactionHistory;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.PortfolioMasterRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataValidationService {

    private static final Logger log = LoggerFactory.getLogger(DataValidationService.class);

    private final PortfolioMasterRepository portfolioRepository;
    private final InvestmentPositionRepository positionRepository;
    private final TransactionHistoryRepository transactionRepository;

    public DataValidationService(PortfolioMasterRepository portfolioRepository,
                                 InvestmentPositionRepository positionRepository,
                                 TransactionHistoryRepository transactionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validateAll() {
        log.info("Starting data validation");
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validatePortfolioIntegrity(errors, warnings);
        validatePositionIntegrity(errors, warnings);
        validateTransactionIntegrity(errors, warnings);

        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("errorCount", errors.size());
        result.put("warningCount", warnings.size());
        result.put("status", errors.isEmpty() ? "PASS" : "FAIL");

        log.info("Validation complete: {} errors, {} warnings", errors.size(), warnings.size());
        return result;
    }

    private void validatePortfolioIntegrity(List<String> errors, List<String> warnings) {
        List<PortfolioMaster> portfolios = portfolioRepository.findAll();
        for (PortfolioMaster p : portfolios) {
            if (p.getPortfolioName() == null || p.getPortfolioName().trim().isEmpty()) {
                errors.add("Portfolio " + p.getPortfolioId() + " has blank name");
            }
            if (p.getTotalValue() != null && p.getTotalValue().signum() < 0) {
                warnings.add("Portfolio " + p.getPortfolioId() + " has negative total value");
            }
        }
    }

    private void validatePositionIntegrity(List<String> errors, List<String> warnings) {
        List<InvestmentPosition> positions = positionRepository.findAll();
        for (InvestmentPosition pos : positions) {
            if (!portfolioRepository.existsById(pos.getPortfolioId())) {
                errors.add("Position references non-existent portfolio: " + pos.getPortfolioId());
            }
        }
    }

    private void validateTransactionIntegrity(List<String> errors, List<String> warnings) {
        List<TransactionHistory> transactions = transactionRepository.findAll();
        for (TransactionHistory txn : transactions) {
            if (!portfolioRepository.existsById(txn.getPortfolioId())) {
                errors.add("Transaction " + txn.getTransactionId()
                        + " references non-existent portfolio: " + txn.getPortfolioId());
            }
        }
    }
}
