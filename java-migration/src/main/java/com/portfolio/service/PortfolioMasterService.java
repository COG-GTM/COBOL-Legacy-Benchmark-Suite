package com.portfolio.service;

import com.portfolio.entity.Portfolio;
import com.portfolio.entity.HistoryRecord;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.HistoryRepository;
import com.portfolio.util.CommonConstants;
import com.portfolio.util.PortfolioValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PortfolioMasterService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioMasterService.class);
    private final PortfolioRepository portfolioRepository;
    private final HistoryRepository historyRepository;
    private final AuditProcessor auditProcessor;
    private final DatabaseErrorHandler errorHandler;

    public PortfolioMasterService(PortfolioRepository portfolioRepository,
                                  HistoryRepository historyRepository,
                                  AuditProcessor auditProcessor,
                                  DatabaseErrorHandler errorHandler) {
        this.portfolioRepository = portfolioRepository;
        this.historyRepository = historyRepository;
        this.auditProcessor = auditProcessor;
        this.errorHandler = errorHandler;
    }

    @Transactional
    public Portfolio createPortfolio(Portfolio portfolio) {
        List<String> errors = PortfolioValidation.validatePortfolio(portfolio);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + String.join(", ", errors));
        }

        if (portfolioRepository.existsById(portfolio.getPortfolioId())) {
            throw new IllegalArgumentException("Portfolio ID already exists: " + portfolio.getPortfolioId());
        }

        portfolio.setOpenDate(LocalDate.now());
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolio.setLastMaintUser("SYSTEM");
        if (portfolio.getStatus() == null) {
            portfolio.setStatus(CommonConstants.STATUS_ACTIVE);
        }
        if (portfolio.getTotalValue() == null) {
            portfolio.setTotalValue(BigDecimal.ZERO);
        }
        if (portfolio.getCashBalance() == null) {
            portfolio.setCashBalance(BigDecimal.ZERO);
        }

        Portfolio saved = portfolioRepository.save(portfolio);
        recordHistory(saved.getPortfolioId(), "PT", "A", null, saved.toString());
        auditProcessor.logTransaction("SYSTEM", saved.getPortfolioId(),
                CommonConstants.AUDIT_ACTION_CREATE, "Portfolio created: " + saved.getPortfolioId());
        log.info("Portfolio created: {}", saved.getPortfolioId());
        return saved;
    }

    public Optional<Portfolio> readPortfolio(String portfolioId) {
        return portfolioRepository.findById(portfolioId);
    }

    @Transactional
    public Portfolio updatePortfolio(String portfolioId, Portfolio updates) {
        Portfolio existing = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        String beforeImage = existing.getPortfolioName() + "|" + existing.getStatus();

        if (updates.getPortfolioName() != null) {
            existing.setPortfolioName(updates.getPortfolioName());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        if (updates.getClientName() != null) {
            existing.setClientName(updates.getClientName());
        }
        if (updates.getTotalValue() != null) {
            existing.setTotalValue(updates.getTotalValue());
        }
        if (updates.getCashBalance() != null) {
            existing.setCashBalance(updates.getCashBalance());
        }
        if (updates.getRiskLevel() != null) {
            existing.setRiskLevel(updates.getRiskLevel());
        }

        existing.setLastMaintDate(LocalDateTime.now());
        existing.setLastMaintUser("SYSTEM");

        Portfolio saved = portfolioRepository.save(existing);
        String afterImage = saved.getPortfolioName() + "|" + saved.getStatus();
        recordHistory(portfolioId, "PT", "C", beforeImage, afterImage);
        auditProcessor.logTransaction("SYSTEM", portfolioId,
                CommonConstants.AUDIT_ACTION_UPDATE, "Portfolio updated");
        log.info("Portfolio updated: {}", portfolioId);
        return saved;
    }

    @Transactional
    public void deletePortfolio(String portfolioId, String reasonCode) {
        Portfolio existing = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        existing.setStatus(CommonConstants.STATUS_CLOSED);
        existing.setCloseDate(LocalDate.now());
        existing.setLastMaintDate(LocalDateTime.now());
        existing.setLastMaintUser("SYSTEM");
        portfolioRepository.save(existing);

        recordHistory(portfolioId, "PT", "D", existing.toString(), null);
        auditProcessor.logTransaction("SYSTEM", portfolioId,
                CommonConstants.AUDIT_ACTION_DELETE,
                "Portfolio closed. Reason: " + (reasonCode != null ? reasonCode : "N/A"));
        log.info("Portfolio deleted (closed): {}", portfolioId);
    }

    public List<Portfolio> findAllPortfolios() {
        return portfolioRepository.findAll();
    }

    public List<Portfolio> findActivePortfolios() {
        return portfolioRepository.findActivePortfolios();
    }

    public List<Portfolio> findByClientId(String clientId) {
        return portfolioRepository.findByClientId(clientId);
    }

    private void recordHistory(String portfolioId, String recordType, String actionCode,
                               String beforeImage, String afterImage) {
        HistoryRecord history = new HistoryRecord();
        history.setPortfolioId(portfolioId);
        history.setHistoryDate(LocalDate.now().toString().replace("-", "").substring(0, 8));
        history.setHistoryTime(LocalDateTime.now().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss")));
        history.setRecordType(recordType);
        history.setActionCode(actionCode);
        history.setBeforeImage(beforeImage != null ? beforeImage.substring(0, Math.min(beforeImage.length(), 400)) : null);
        history.setAfterImage(afterImage != null ? afterImage.substring(0, Math.min(afterImage.length(), 400)) : null);
        history.setProcessDate(LocalDateTime.now());
        history.setProcessUser("SYSTEM");
        historyRepository.save(history);
    }
}
