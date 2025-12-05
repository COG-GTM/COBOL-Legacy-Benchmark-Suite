package com.portfolio.repository;

import com.portfolio.entity.PositionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Position History entity
 * Provides data access methods for historical position data
 * Supports partitioned table queries for efficient historical data access
 */
@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, UUID> {

    /**
     * Find history by account number and portfolio ID
     */
    List<PositionHistory> findByAccountNoAndPortfolioId(String accountNo, String portfolioId);

    /**
     * Find history by account number and portfolio ID with pagination
     */
    Page<PositionHistory> findByAccountNoAndPortfolioId(String accountNo, String portfolioId, Pageable pageable);

    /**
     * Find history by account number, portfolio ID, and date range
     */
    List<PositionHistory> findByAccountNoAndPortfolioIdAndTransDateBetween(
            String accountNo, String portfolioId, LocalDate startDate, LocalDate endDate);

    /**
     * Find history by security ID
     */
    List<PositionHistory> findBySecurityId(String securityId);

    /**
     * Find history by security ID and date range
     */
    List<PositionHistory> findBySecurityIdAndTransDateBetween(
            String securityId, LocalDate startDate, LocalDate endDate);

    /**
     * Find history by transaction date
     */
    List<PositionHistory> findByTransDate(LocalDate transDate);

    /**
     * Find history within date range
     */
    List<PositionHistory> findByTransDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find history within date range with pagination
     */
    Page<PositionHistory> findByTransDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Find history by process date and program ID
     */
    List<PositionHistory> findByProcessDateAndProgramId(LocalDate processDate, String programId);

    /**
     * Find history by transaction type
     */
    List<PositionHistory> findByTransType(String transType);

    /**
     * Find history by user ID
     */
    List<PositionHistory> findByUserId(String userId);

    /**
     * Get total gain/loss by account and portfolio within date range
     */
    @Query("SELECT SUM(p.gainLoss) FROM PositionHistory p " +
            "WHERE p.accountNo = :accountNo AND p.portfolioId = :portfolioId " +
            "AND p.transDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalGainLossByAccountAndPortfolio(
            @Param("accountNo") String accountNo,
            @Param("portfolioId") String portfolioId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total fees by account and portfolio within date range
     */
    @Query("SELECT SUM(p.fees) FROM PositionHistory p " +
            "WHERE p.accountNo = :accountNo AND p.portfolioId = :portfolioId " +
            "AND p.transDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalFeesByAccountAndPortfolio(
            @Param("accountNo") String accountNo,
            @Param("portfolioId") String portfolioId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total amount by transaction type within date range
     */
    @Query("SELECT SUM(p.totalAmount) FROM PositionHistory p " +
            "WHERE p.transType = :transType AND p.transDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalAmountByTransType(
            @Param("transType") String transType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count records by process date
     */
    long countByProcessDate(LocalDate processDate);

    /**
     * Count records by program ID
     */
    long countByProgramId(String programId);

    /**
     * Find buy transactions within date range
     */
    @Query("SELECT p FROM PositionHistory p WHERE p.transType = 'BU' " +
            "AND p.transDate BETWEEN :startDate AND :endDate")
    List<PositionHistory> findBuyTransactions(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find sell transactions within date range
     */
    @Query("SELECT p FROM PositionHistory p WHERE p.transType = 'SL' " +
            "AND p.transDate BETWEEN :startDate AND :endDate")
    List<PositionHistory> findSellTransactions(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find transactions with gains
     */
    @Query("SELECT p FROM PositionHistory p WHERE p.gainLoss > 0 " +
            "AND p.transDate BETWEEN :startDate AND :endDate")
    List<PositionHistory> findTransactionsWithGains(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find transactions with losses
     */
    @Query("SELECT p FROM PositionHistory p WHERE p.gainLoss < 0 " +
            "AND p.transDate BETWEEN :startDate AND :endDate")
    List<PositionHistory> findTransactionsWithLosses(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
