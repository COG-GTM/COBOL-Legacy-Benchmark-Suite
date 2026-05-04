package com.portfolio.service.portfolio;

import com.portfolio.domain.Portfolio;
import com.portfolio.exception.ProcessingException;
import com.portfolio.exception.ValidationException;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.service.common.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Portfolio Service - migrated from COBOL PORTADD/PORTREAD/PORTUPDT/PORTDEL.cbl.
 * Combines all CRUD operations into a single service.
 * VSAM operations replaced with JPA repository calls.
 */
@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final PortfolioRepository portfolioRepository;
    private final PortfolioValidationService validationService;
    private final AuditService auditService;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            PortfolioValidationService validationService,
                            AuditService auditService) {
        this.portfolioRepository = portfolioRepository;
        this.validationService = validationService;
        this.auditService = auditService;
    }

    @Transactional
    public Portfolio createPortfolio(Portfolio portfolio) {
        validationService.validatePortfolio(portfolio);

        if (portfolioRepository.existsById(portfolio.getPortfolioId())) {
            throw new ValidationException("Portfolio ID already exists: " + portfolio.getPortfolioId());
        }

        portfolio.setOpenDate(LocalDate.now());
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolio.setStatus("A");

        try {
            Portfolio saved = portfolioRepository.save(portfolio);
            auditService.logTransaction(portfolio.getLastMaintUser(), "PORTADD",
                    portfolio.getPortfolioId(), "CREATE", "SUCC",
                    "Portfolio created successfully");
            log.info("Portfolio created: {}", saved.getPortfolioId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("Portfolio ID already exists: " + portfolio.getPortfolioId());
        }
    }

    @Transactional(readOnly = true)
    public Optional<Portfolio> getPortfolio(String portfolioId) {
        validationService.validatePortfolioId(portfolioId);
        return portfolioRepository.findById(portfolioId);
    }

    @Transactional
    public Portfolio updatePortfolio(Portfolio portfolio) {
        validationService.validatePortfolio(portfolio);

        Portfolio existing = portfolioRepository.findById(portfolio.getPortfolioId())
                .orElseThrow(() -> new ProcessingException(
                        "Portfolio not found: " + portfolio.getPortfolioId()));

        String beforeImage = existing.getPortfolioName();

        existing.setClientName(portfolio.getClientName());
        existing.setClientType(portfolio.getClientType());
        existing.setPortfolioName(portfolio.getPortfolioName());
        existing.setCurrencyCode(portfolio.getCurrencyCode());
        existing.setRiskLevel(portfolio.getRiskLevel());
        existing.setStatus(portfolio.getStatus());
        existing.setTotalValue(portfolio.getTotalValue());
        existing.setCashBalance(portfolio.getCashBalance());
        existing.setLastMaintDate(LocalDateTime.now());
        existing.setLastMaintUser(portfolio.getLastMaintUser());

        Portfolio saved = portfolioRepository.save(existing);

        auditService.logTransaction(portfolio.getLastMaintUser(), "PORTUPDT",
                portfolio.getPortfolioId(), "UPDATE", "SUCC",
                "Portfolio updated");
        log.info("Portfolio updated: {}", saved.getPortfolioId());
        return saved;
    }

    @Transactional
    public void deletePortfolio(String portfolioId, String userId) {
        validationService.validatePortfolioId(portfolioId);

        Portfolio existing = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ProcessingException("Portfolio not found: " + portfolioId));

        existing.setStatus("C");
        existing.setCloseDate(LocalDate.now());
        existing.setLastMaintDate(LocalDateTime.now());
        existing.setLastMaintUser(userId);
        portfolioRepository.save(existing);

        auditService.logTransaction(userId, "PORTDEL", portfolioId,
                "DELETE", "SUCC", "Portfolio closed (soft delete)");
        log.info("Portfolio soft-deleted: {}", portfolioId);
    }

    @Transactional(readOnly = true)
    public List<Portfolio> getActivePortfolios() {
        return portfolioRepository.findActivePortfolios();
    }

    @Transactional(readOnly = true)
    public List<Portfolio> getPortfoliosByClient(String clientId) {
        return portfolioRepository.findByClientId(clientId);
    }
}
