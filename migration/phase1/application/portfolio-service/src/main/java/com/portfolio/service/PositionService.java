package com.portfolio.service;

import com.portfolio.entity.Position;
import com.portfolio.entity.Position.PositionStatus;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Position operations.
 * Replaces VSAM POSFILE access operations.
 * 
 * @see src/cics/PORTDFN.csd - POSFILE definition
 * @see src/programs/online/INQPORT.cbl - Position inquiry logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PositionService {

    private final PositionRepository positionRepository;
    private final AuditService auditService;

    @Cacheable(value = "positions", key = "#portfolioId + '-' + #positionDate + '-' + #investmentId")
    public Optional<Position> findByKey(String portfolioId, LocalDate positionDate, String investmentId) {
        log.debug("Finding position: portfolio={}, date={}, investment={}", 
                  portfolioId, positionDate, investmentId);
        return positionRepository.findByPortfolioIdAndPositionDateAndInvestmentId(
                portfolioId, positionDate, investmentId);
    }

    public Optional<Position> findById(UUID id) {
        return positionRepository.findById(id);
    }

    public List<Position> findByPortfolioId(String portfolioId) {
        return positionRepository.findByPortfolioId(portfolioId);
    }

    public Page<Position> findByPortfolioId(String portfolioId, Pageable pageable) {
        return positionRepository.findByPortfolioId(portfolioId, pageable);
    }

    public List<Position> findCurrentPositions(String portfolioId) {
        return positionRepository.findCurrentPositions(portfolioId);
    }

    public List<Position> findLatestPositions(String portfolioId) {
        return positionRepository.findLatestPositionsByPortfolio(portfolioId);
    }

    public List<Position> findByInvestmentId(String investmentId) {
        return positionRepository.findByInvestmentId(investmentId);
    }

    @Transactional
    @CacheEvict(value = "positions", key = "#position.portfolioId + '-' + #position.positionDate + '-' + #position.investmentId")
    public Position create(Position position, String userId) {
        log.info("Creating position: portfolio={}, investment={}", 
                 position.getPortfolioId(), position.getInvestmentId());
        
        position.setCreatedBy(userId);
        position.setUpdatedBy(userId);
        
        if (position.getAverageCost() == null || position.getAverageCost().compareTo(BigDecimal.ZERO) == 0) {
            if (position.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                position.setAverageCost(position.getCostBasis().divide(
                        position.getQuantity(), 4, java.math.RoundingMode.HALF_UP));
            }
        }
        
        Position saved = positionRepository.save(position);
        
        auditService.logPositionAction(position.getPortfolioId(), position.getInvestmentId(), 
                                       "CREATE", userId);
        
        return saved;
    }

    @Transactional
    @CacheEvict(value = "positions", key = "#portfolioId + '-' + #positionDate + '-' + #investmentId")
    public Position update(String portfolioId, LocalDate positionDate, String investmentId, 
                          Position updates, String userId) {
        log.info("Updating position: portfolio={}, investment={}", portfolioId, investmentId);
        
        Position existing = positionRepository.findByPortfolioIdAndPositionDateAndInvestmentId(
                portfolioId, positionDate, investmentId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found"));
        
        if (updates.getQuantity() != null) {
            existing.setQuantity(updates.getQuantity());
        }
        if (updates.getCostBasis() != null) {
            existing.setCostBasis(updates.getCostBasis());
        }
        if (updates.getMarketValue() != null) {
            existing.setMarketValue(updates.getMarketValue());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        
        if (existing.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            existing.setAverageCost(existing.getCostBasis().divide(
                    existing.getQuantity(), 4, java.math.RoundingMode.HALF_UP));
        }
        
        existing.setUpdatedBy(userId);
        
        Position saved = positionRepository.save(existing);
        
        auditService.logPositionAction(portfolioId, investmentId, "UPDATE", userId);
        
        return saved;
    }

    @Transactional
    public void closePosition(String portfolioId, LocalDate positionDate, String investmentId, String userId) {
        log.info("Closing position: portfolio={}, investment={}", portfolioId, investmentId);
        
        Position position = positionRepository.findByPortfolioIdAndPositionDateAndInvestmentId(
                portfolioId, positionDate, investmentId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found"));
        
        position.setStatus(PositionStatus.CLOSED);
        position.setUpdatedBy(userId);
        
        positionRepository.save(position);
        
        auditService.logPositionAction(portfolioId, investmentId, "CLOSE", userId);
    }

    public BigDecimal calculateTotalMarketValue(String portfolioId) {
        BigDecimal total = positionRepository.calculateTotalMarketValue(portfolioId);
        return total != null ? total : BigDecimal.ZERO;
    }

    public long countDistinctInvestments(String portfolioId) {
        return positionRepository.countDistinctInvestments(portfolioId);
    }
}
