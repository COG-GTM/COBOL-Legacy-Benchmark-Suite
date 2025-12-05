package com.portfolio.service;

import com.portfolio.config.RedisConfig;
import com.portfolio.entity.InvestmentPosition;
import com.portfolio.repository.InvestmentPositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Service layer for Investment Position operations
 * Implements caching for frequently accessed data
 */
@Service
@Transactional
public class PositionService {

    private static final Logger logger = LoggerFactory.getLogger(PositionService.class);

    private final InvestmentPositionRepository positionRepository;

    public PositionService(InvestmentPositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * Find position by ID with caching
     */
    @Cacheable(value = RedisConfig.POSITION_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public Optional<InvestmentPosition> findById(UUID id) {
        logger.debug("Finding position by ID: {}", id);
        return positionRepository.findById(id);
    }

    /**
     * Find position by composite key with caching
     */
    @Cacheable(value = RedisConfig.POSITION_CACHE, 
               key = "#portfolioId + '-' + #investmentId + '-' + #positionDate")
    @Transactional(readOnly = true)
    public Optional<InvestmentPosition> findByKey(String portfolioId, String investmentId, LocalDate positionDate) {
        logger.debug("Finding position by key: {}-{}-{}", portfolioId, investmentId, positionDate);
        return positionRepository.findByPortfolioIdAndInvestmentIdAndPositionDate(
                portfolioId, investmentId, positionDate);
    }

    /**
     * Find all positions by portfolio ID
     */
    @Transactional(readOnly = true)
    public List<InvestmentPosition> findByPortfolioId(String portfolioId) {
        logger.debug("Finding positions by portfolio ID: {}", portfolioId);
        return positionRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Find positions by portfolio ID with pagination
     */
    @Transactional(readOnly = true)
    public Page<InvestmentPosition> findByPortfolioId(String portfolioId, Pageable pageable) {
        logger.debug("Finding positions by portfolio ID with pagination: {}", portfolioId);
        return positionRepository.findByPortfolioId(portfolioId, pageable);
    }

    /**
     * Find active positions by portfolio ID
     */
    @Cacheable(value = RedisConfig.POSITION_CACHE, key = "'active-' + #portfolioId")
    @Transactional(readOnly = true)
    public List<InvestmentPosition> findActivePositionsByPortfolioId(String portfolioId) {
        logger.debug("Finding active positions by portfolio ID: {}", portfolioId);
        return positionRepository.findActivePositionsByPortfolioId(portfolioId);
    }

    /**
     * Find latest position for portfolio and investment
     */
    @Transactional(readOnly = true)
    public Optional<InvestmentPosition> findLatestPosition(String portfolioId, String investmentId) {
        logger.debug("Finding latest position for: {}-{}", portfolioId, investmentId);
        return positionRepository.findLatestPosition(portfolioId, investmentId);
    }

    /**
     * Find current positions (yesterday's date)
     */
    @Transactional(readOnly = true)
    public List<InvestmentPosition> findCurrentPositions() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        logger.debug("Finding current positions for date: {}", yesterday);
        return positionRepository.findCurrentPositions(yesterday);
    }

    /**
     * Find positions within date range
     */
    @Transactional(readOnly = true)
    public List<InvestmentPosition> findByDateRange(LocalDate startDate, LocalDate endDate) {
        logger.debug("Finding positions between {} and {}", startDate, endDate);
        return positionRepository.findByPositionDateBetween(startDate, endDate);
    }

    /**
     * Save position with cache eviction
     */
    @CacheEvict(value = RedisConfig.POSITION_CACHE, allEntries = true)
    public InvestmentPosition save(InvestmentPosition position) {
        logger.debug("Saving position: {}-{}", position.getPortfolioId(), position.getInvestmentId());
        return positionRepository.save(position);
    }

    /**
     * Update position with cache eviction
     */
    @CacheEvict(value = RedisConfig.POSITION_CACHE, allEntries = true)
    public InvestmentPosition update(InvestmentPosition position) {
        logger.debug("Updating position: {}-{}", position.getPortfolioId(), position.getInvestmentId());
        return positionRepository.save(position);
    }

    /**
     * Delete position with cache eviction
     */
    @CacheEvict(value = RedisConfig.POSITION_CACHE, allEntries = true)
    public void delete(UUID id) {
        logger.debug("Deleting position: {}", id);
        positionRepository.deleteById(id);
    }

    /**
     * Get total market value by portfolio ID
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalMarketValueByPortfolioId(String portfolioId) {
        logger.debug("Getting total market value for portfolio: {}", portfolioId);
        BigDecimal total = positionRepository.getTotalMarketValueByPortfolioId(portfolioId);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get total cost basis by portfolio ID
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCostBasisByPortfolioId(String portfolioId) {
        logger.debug("Getting total cost basis for portfolio: {}", portfolioId);
        BigDecimal total = positionRepository.getTotalCostBasisByPortfolioId(portfolioId);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Count positions by portfolio ID
     */
    @Transactional(readOnly = true)
    public long countByPortfolioId(String portfolioId) {
        return positionRepository.countByPortfolioId(portfolioId);
    }

    /**
     * Find positions with holdings
     */
    @Transactional(readOnly = true)
    public List<InvestmentPosition> findPositionsWithHoldings(String portfolioId) {
        logger.debug("Finding positions with holdings for portfolio: {}", portfolioId);
        return positionRepository.findPositionsWithHoldings(portfolioId);
    }
}
