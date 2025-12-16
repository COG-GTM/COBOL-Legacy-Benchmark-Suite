package com.portfolio.service;

import com.portfolio.entity.Portfolio;
import com.portfolio.entity.Portfolio.PortfolioStatus;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Portfolio operations.
 * Replaces COBOL INQPORT program functionality for portfolio inquiries.
 * 
 * @see src/programs/online/INQPORT.cbl
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final AuditService auditService;

    @Cacheable(value = "portfolios", key = "#portfolioId")
    public Optional<Portfolio> findByPortfolioId(String portfolioId) {
        log.debug("Finding portfolio by ID: {}", portfolioId);
        return portfolioRepository.findByPortfolioId(portfolioId);
    }

    public Optional<Portfolio> findById(UUID id) {
        return portfolioRepository.findById(id);
    }

    public List<Portfolio> findByClientId(String clientId) {
        log.debug("Finding portfolios for client: {}", clientId);
        return portfolioRepository.findByClientId(clientId);
    }

    public Page<Portfolio> findByStatus(PortfolioStatus status, Pageable pageable) {
        return portfolioRepository.findByStatus(status, pageable);
    }

    public Page<Portfolio> findAll(Pageable pageable) {
        return portfolioRepository.findAll(pageable);
    }

    public List<Portfolio> findActivePortfolios() {
        return portfolioRepository.findActivePortfolios(PortfolioStatus.ACTIVE);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolio.portfolioId")
    public Portfolio create(Portfolio portfolio, String userId) {
        log.info("Creating portfolio: {} for client: {}", portfolio.getPortfolioId(), portfolio.getClientId());
        
        if (portfolioRepository.existsByPortfolioId(portfolio.getPortfolioId())) {
            throw new IllegalArgumentException("Portfolio already exists: " + portfolio.getPortfolioId());
        }
        
        portfolio.setCreatedBy(userId);
        portfolio.setUpdatedBy(userId);
        
        Portfolio saved = portfolioRepository.save(portfolio);
        
        auditService.logPortfolioAction(saved.getPortfolioId(), "CREATE", userId, null, saved.toString());
        
        return saved;
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolioId")
    public Portfolio update(String portfolioId, Portfolio updates, String userId) {
        log.info("Updating portfolio: {}", portfolioId);
        
        Portfolio existing = portfolioRepository.findByPortfolioId(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
        
        String beforeImage = existing.toString();
        
        if (updates.getPortfolioName() != null) {
            existing.setPortfolioName(updates.getPortfolioName());
        }
        if (updates.getRiskLevel() != null) {
            existing.setRiskLevel(updates.getRiskLevel());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        
        existing.setUpdatedBy(userId);
        
        Portfolio saved = portfolioRepository.save(existing);
        
        auditService.logPortfolioAction(portfolioId, "UPDATE", userId, beforeImage, saved.toString());
        
        return saved;
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolioId")
    public void close(String portfolioId, String userId) {
        log.info("Closing portfolio: {}", portfolioId);
        
        Portfolio portfolio = portfolioRepository.findByPortfolioId(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
        
        portfolio.setStatus(PortfolioStatus.CLOSED);
        portfolio.setCloseDate(java.time.LocalDate.now());
        portfolio.setUpdatedBy(userId);
        
        portfolioRepository.save(portfolio);
        
        auditService.logPortfolioAction(portfolioId, "CLOSE", userId, null, null);
    }

    public BigDecimal calculateTotalValue(String portfolioId) {
        BigDecimal marketValue = positionRepository.calculateTotalMarketValue(portfolioId);
        return marketValue != null ? marketValue : BigDecimal.ZERO;
    }

    public BigDecimal calculateTotalCostBasis(String portfolioId) {
        BigDecimal costBasis = positionRepository.calculateTotalCostBasis(portfolioId);
        return costBasis != null ? costBasis : BigDecimal.ZERO;
    }

    public long countByStatus(PortfolioStatus status) {
        return portfolioRepository.countByStatus(status);
    }
}
