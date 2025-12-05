package com.portfolio.service;

import com.portfolio.config.RedisConfig;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.repository.PortfolioMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Service layer for Portfolio Master operations
 * Implements caching for frequently accessed data
 */
@Service
@Transactional
public class PortfolioService {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioService.class);

    private final PortfolioMasterRepository portfolioMasterRepository;

    public PortfolioService(PortfolioMasterRepository portfolioMasterRepository) {
        this.portfolioMasterRepository = portfolioMasterRepository;
    }

    /**
     * Find portfolio by ID with caching
     */
    @Cacheable(value = RedisConfig.PORTFOLIO_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public Optional<PortfolioMaster> findById(UUID id) {
        logger.debug("Finding portfolio by ID: {}", id);
        return portfolioMasterRepository.findById(id);
    }

    /**
     * Find portfolio by composite key with caching
     */
    @Cacheable(value = RedisConfig.PORTFOLIO_CACHE, key = "#portfolioId + '-' + #accountType + '-' + #branchId")
    @Transactional(readOnly = true)
    public Optional<PortfolioMaster> findByKey(String portfolioId, String accountType, String branchId) {
        logger.debug("Finding portfolio by key: {}-{}-{}", portfolioId, accountType, branchId);
        return portfolioMasterRepository.findByPortfolioIdAndAccountTypeAndBranchId(
                portfolioId, accountType, branchId);
    }

    /**
     * Find portfolio by portfolio ID
     */
    @Cacheable(value = RedisConfig.PORTFOLIO_CACHE, key = "'pid-' + #portfolioId")
    @Transactional(readOnly = true)
    public Optional<PortfolioMaster> findByPortfolioId(String portfolioId) {
        logger.debug("Finding portfolio by portfolio ID: {}", portfolioId);
        return portfolioMasterRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Find all portfolios by client ID
     */
    @Transactional(readOnly = true)
    public List<PortfolioMaster> findByClientId(String clientId) {
        logger.debug("Finding portfolios by client ID: {}", clientId);
        return portfolioMasterRepository.findByClientId(clientId);
    }

    /**
     * Find all active portfolios
     */
    @Transactional(readOnly = true)
    public List<PortfolioMaster> findAllActivePortfolios() {
        logger.debug("Finding all active portfolios");
        return portfolioMasterRepository.findAllActivePortfolios();
    }

    /**
     * Find active portfolios with pagination
     */
    @Transactional(readOnly = true)
    public Page<PortfolioMaster> findAllActivePortfolios(Pageable pageable) {
        logger.debug("Finding active portfolios with pagination");
        return portfolioMasterRepository.findAllActivePortfolios(pageable);
    }

    /**
     * Find portfolios by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<PortfolioMaster> findByStatus(String status, Pageable pageable) {
        logger.debug("Finding portfolios by status: {}", status);
        return portfolioMasterRepository.findByStatus(status, pageable);
    }

    /**
     * Save portfolio with cache eviction
     */
    @CacheEvict(value = RedisConfig.PORTFOLIO_CACHE, allEntries = true)
    public PortfolioMaster save(PortfolioMaster portfolio) {
        logger.debug("Saving portfolio: {}", portfolio.getPortfolioId());
        return portfolioMasterRepository.save(portfolio);
    }

    /**
     * Update portfolio with cache eviction
     */
    @CacheEvict(value = RedisConfig.PORTFOLIO_CACHE, allEntries = true)
    public PortfolioMaster update(PortfolioMaster portfolio) {
        logger.debug("Updating portfolio: {}", portfolio.getPortfolioId());
        return portfolioMasterRepository.save(portfolio);
    }

    /**
     * Delete portfolio with cache eviction
     */
    @CacheEvict(value = RedisConfig.PORTFOLIO_CACHE, allEntries = true)
    public void delete(UUID id) {
        logger.debug("Deleting portfolio: {}", id);
        portfolioMasterRepository.deleteById(id);
    }

    /**
     * Get total value by client ID
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalValueByClientId(String clientId) {
        logger.debug("Getting total value for client: {}", clientId);
        BigDecimal total = portfolioMasterRepository.getTotalValueByClientId(clientId);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Search portfolios by client name
     */
    @Transactional(readOnly = true)
    public List<PortfolioMaster> searchByClientName(String name) {
        logger.debug("Searching portfolios by client name: {}", name);
        return portfolioMasterRepository.searchByClientName(name);
    }

    /**
     * Find high value portfolios
     */
    @Transactional(readOnly = true)
    public List<PortfolioMaster> findHighValuePortfolios(BigDecimal minValue) {
        logger.debug("Finding high value portfolios with min value: {}", minValue);
        return portfolioMasterRepository.findHighValuePortfolios(minValue);
    }

    /**
     * Count portfolios by status
     */
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return portfolioMasterRepository.countByStatus(status);
    }
}
