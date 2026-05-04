package com.portfolio.service;

import com.portfolio.entity.Portfolio;
import com.portfolio.entity.PositionRecord;
import com.portfolio.entity.TransactionRecord;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionRepository;
import com.portfolio.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataValidationService {

    private static final Logger log = LoggerFactory.getLogger(DataValidationService.class);
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;

    public DataValidationService(PortfolioRepository portfolioRepository,
                                 PositionRepository positionRepository,
                                 TransactionRepository transactionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<String> validateAllData() {
        List<String> issues = new ArrayList<>();
        issues.addAll(validatePortfolios());
        issues.addAll(validatePositions());
        issues.addAll(validateTransactions());
        log.info("Data validation complete. Issues found: {}", issues.size());
        return issues;
    }

    private List<String> validatePortfolios() {
        List<String> issues = new ArrayList<>();
        List<Portfolio> portfolios = portfolioRepository.findAll();
        for (Portfolio p : portfolios) {
            if (p.getPortfolioId() == null || p.getPortfolioId().isBlank()) {
                issues.add("Portfolio with null ID found");
            }
            if (p.getStatus() == null || !List.of("A", "C", "S").contains(p.getStatus())) {
                issues.add("Invalid status for portfolio: " + p.getPortfolioId());
            }
        }
        return issues;
    }

    private List<String> validatePositions() {
        List<String> issues = new ArrayList<>();
        List<PositionRecord> positions = positionRepository.findAll();
        for (PositionRecord pos : positions) {
            if (!portfolioRepository.existsById(pos.getPortfolioId())) {
                issues.add("Orphan position for portfolio: " + pos.getPortfolioId());
            }
            if (pos.getQuantity() != null && pos.getQuantity().compareTo(BigDecimal.ZERO) < 0
                    && "A".equals(pos.getStatus())) {
                issues.add("Active position with negative quantity: " + pos.getPortfolioId()
                        + "/" + pos.getInvestmentId());
            }
        }
        return issues;
    }

    private List<String> validateTransactions() {
        List<String> issues = new ArrayList<>();
        List<TransactionRecord> transactions = transactionRepository.findAll();
        for (TransactionRecord t : transactions) {
            if (!portfolioRepository.existsById(t.getPortfolioId())) {
                issues.add("Transaction references non-existent portfolio: " + t.getPortfolioId());
            }
        }
        return issues;
    }
}
