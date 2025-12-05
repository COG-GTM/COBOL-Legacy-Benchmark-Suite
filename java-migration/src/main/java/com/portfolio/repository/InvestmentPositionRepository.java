package com.portfolio.repository;

import com.portfolio.entity.InvestmentPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Investment Position entity
 * Provides data access methods for position operations
 * Supports row-level locking to replicate VSAM record-level locking behavior
 */
@Repository
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, UUID> {

    /**
     * Find position by composite key (portfolio ID, investment ID, position date)
     * Replicates VSAM KSDS key access pattern
     */
    Optional<InvestmentPosition> findByPortfolioIdAndInvestmentIdAndPositionDate(
            String portfolioId, String investmentId, LocalDate positionDate);

    /**
     * Find position by composite key with pessimistic lock
     * Replicates VSAM record-level locking for updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM InvestmentPosition p WHERE p.portfolioId = :portfolioId " +
            "AND p.investmentId = :investmentId AND p.positionDate = :positionDate")
    Optional<InvestmentPosition> findByKeyWithLock(
            @Param("portfolioId") String portfolioId,
            @Param("investmentId") String investmentId,
            @Param("positionDate") LocalDate positionDate);

    /**
     * Find all positions by portfolio ID
     */
    List<InvestmentPosition> findByPortfolioId(String portfolioId);

    /**
     * Find all positions by portfolio ID with pagination
     */
    Page<InvestmentPosition> findByPortfolioId(String portfolioId, Pageable pageable);

    /**
     * Find all positions by portfolio ID and status
     */
    List<InvestmentPosition> findByPortfolioIdAndStatus(String portfolioId, String status);

    /**
     * Find all active positions by portfolio ID
     */
    @Query("SELECT p FROM InvestmentPosition p WHERE p.portfolioId = :portfolioId AND p.status = 'A'")
    List<InvestmentPosition> findActivePositionsByPortfolioId(@Param("portfolioId") String portfolioId);

    /**
     * Find all positions by investment ID
     */
    List<InvestmentPosition> findByInvestmentId(String investmentId);

    /**
     * Find all positions by position date
     */
    List<InvestmentPosition> findByPositionDate(LocalDate positionDate);

    /**
     * Find positions by portfolio ID and position date
     */
    List<InvestmentPosition> findByPortfolioIdAndPositionDate(String portfolioId, LocalDate positionDate);

    /**
     * Find positions within date range
     */
    List<InvestmentPosition> findByPositionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find positions by portfolio ID within date range
     */
    List<InvestmentPosition> findByPortfolioIdAndPositionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    /**
     * Find latest position for portfolio and investment
     */
    @Query("SELECT p FROM InvestmentPosition p WHERE p.portfolioId = :portfolioId " +
            "AND p.investmentId = :investmentId ORDER BY p.positionDate DESC LIMIT 1")
    Optional<InvestmentPosition> findLatestPosition(
            @Param("portfolioId") String portfolioId,
            @Param("investmentId") String investmentId);

    /**
     * Find current positions (yesterday's date as per COBOL pattern)
     */
    @Query("SELECT p FROM InvestmentPosition p WHERE p.positionDate = :date AND p.status = 'A'")
    List<InvestmentPosition> findCurrentPositions(@Param("date") LocalDate date);

    /**
     * Get total market value by portfolio ID
     */
    @Query("SELECT SUM(p.marketValue) FROM InvestmentPosition p " +
            "WHERE p.portfolioId = :portfolioId AND p.status = 'A'")
    BigDecimal getTotalMarketValueByPortfolioId(@Param("portfolioId") String portfolioId);

    /**
     * Get total cost basis by portfolio ID
     */
    @Query("SELECT SUM(p.costBasis) FROM InvestmentPosition p " +
            "WHERE p.portfolioId = :portfolioId AND p.status = 'A'")
    BigDecimal getTotalCostBasisByPortfolioId(@Param("portfolioId") String portfolioId);

    /**
     * Count positions by portfolio ID
     */
    long countByPortfolioId(String portfolioId);

    /**
     * Count active positions by portfolio ID
     */
    long countByPortfolioIdAndStatus(String portfolioId, String status);

    /**
     * Check if position exists by composite key
     */
    boolean existsByPortfolioIdAndInvestmentIdAndPositionDate(
            String portfolioId, String investmentId, LocalDate positionDate);

    /**
     * Find positions with quantity greater than zero
     */
    @Query("SELECT p FROM InvestmentPosition p WHERE p.portfolioId = :portfolioId " +
            "AND p.quantity > 0 AND p.status = 'A'")
    List<InvestmentPosition> findPositionsWithHoldings(@Param("portfolioId") String portfolioId);

    /**
     * Find positions by currency code
     */
    List<InvestmentPosition> findByCurrencyCode(String currencyCode);
}
