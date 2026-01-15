package com.portfolio.modernization.repository;

import com.portfolio.modernization.entity.PositionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Position Repository Interface
 * 
 * Provides data access operations for PositionRecord entities.
 * Modernized from COBOL VSAM file operations in POSUPD00.cbl and INQPORT.cbl
 * 
 * Original COBOL operations:
 * - READ POSFILE (INQPORT.cbl)
 * - WRITE POSFILE (POSUPD00.cbl)
 * - REWRITE POSFILE (POSUPD00.cbl)
 * - DELETE POSFILE (POSUPD00.cbl)
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Repository
public interface PositionRepository extends JpaRepository<PositionRecord, String> {

    /**
     * Find all positions by account number
     * Equivalent to COBOL: READ POSFILE WITH KEY ACCOUNT-NUMBER
     * 
     * @param accountNumber the account number to search for
     * @return list of positions for the account
     */
    List<PositionRecord> findByAccountNumber(String accountNumber);

    /**
     * Find all active positions by account number
     * 
     * @param accountNumber the account number to search for
     * @return list of active positions for the account
     */
    List<PositionRecord> findByAccountNumberAndStatus(String accountNumber, String status);

    /**
     * Find positions by fund ID updated after a specific date
     * Useful for market value updates and reporting
     * 
     * @param fundId the fund identifier
     * @param date the date threshold
     * @return list of positions matching criteria
     */
    List<PositionRecord> findByFundIdAndLastUpdateAfter(String fundId, LocalDateTime date);

    /**
     * Find all positions for a specific fund
     * 
     * @param fundId the fund identifier
     * @return list of positions for the fund
     */
    List<PositionRecord> findByFundId(String fundId);

    /**
     * Find high-value positions exceeding a threshold
     * Equivalent to COBOL report generation logic in RPTPOS00.cbl
     * 
     * @param threshold the market value threshold
     * @return list of high-value positions
     */
    @Query("SELECT p FROM PositionRecord p WHERE p.marketValue > :threshold AND p.status = 'A'")
    List<PositionRecord> findHighValuePositions(@Param("threshold") BigDecimal threshold);

    /**
     * Find all active positions
     * 
     * @return list of all active positions
     */
    @Query("SELECT p FROM PositionRecord p WHERE p.status = 'A'")
    List<PositionRecord> findAllActivePositions();

    /**
     * Find positions by status
     * 
     * @param status the position status (A=Active, C=Closed, P=Pending)
     * @return list of positions with the specified status
     */
    List<PositionRecord> findByStatus(String status);

    /**
     * Find positions by currency code
     * 
     * @param currencyCode the 3-character currency code
     * @return list of positions in the specified currency
     */
    List<PositionRecord> findByCurrencyCode(String currencyCode);

    /**
     * Calculate total market value for an account
     * 
     * @param accountNumber the account number
     * @return total market value
     */
    @Query("SELECT SUM(p.marketValue) FROM PositionRecord p WHERE p.accountNumber = :accountNumber AND p.status = 'A'")
    BigDecimal calculateTotalMarketValueByAccount(@Param("accountNumber") String accountNumber);

    /**
     * Calculate total cost basis for an account
     * 
     * @param accountNumber the account number
     * @return total cost basis
     */
    @Query("SELECT SUM(p.costBasis) FROM PositionRecord p WHERE p.accountNumber = :accountNumber AND p.status = 'A'")
    BigDecimal calculateTotalCostBasisByAccount(@Param("accountNumber") String accountNumber);

    /**
     * Find positions with unrealized gains
     * 
     * @return list of positions where market value exceeds cost basis
     */
    @Query("SELECT p FROM PositionRecord p WHERE p.marketValue > p.costBasis AND p.status = 'A'")
    List<PositionRecord> findPositionsWithGains();

    /**
     * Find positions with unrealized losses
     * 
     * @return list of positions where cost basis exceeds market value
     */
    @Query("SELECT p FROM PositionRecord p WHERE p.costBasis > p.marketValue AND p.status = 'A'")
    List<PositionRecord> findPositionsWithLosses();

    /**
     * Find positions updated within a date range
     * 
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of positions updated within the range
     */
    @Query("SELECT p FROM PositionRecord p WHERE p.lastUpdate BETWEEN :startDate AND :endDate")
    List<PositionRecord> findByLastUpdateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find positions by account and fund
     * 
     * @param accountNumber the account number
     * @param fundId the fund identifier
     * @return optional position if found
     */
    Optional<PositionRecord> findByAccountNumberAndFundId(String accountNumber, String fundId);

    /**
     * Count positions by status
     * 
     * @param status the position status
     * @return count of positions with the specified status
     */
    long countByStatus(String status);

    /**
     * Count positions by account
     * 
     * @param accountNumber the account number
     * @return count of positions for the account
     */
    long countByAccountNumber(String accountNumber);

    /**
     * Find positions migrated from VSAM
     * 
     * @return list of positions that were migrated from VSAM
     */
    @Query("SELECT p FROM PositionRecord p WHERE p.vsamMigrationDate IS NOT NULL")
    List<PositionRecord> findMigratedPositions();

    /**
     * Find positions by VSAM record key
     * Used for migration verification and audit
     * 
     * @param vsamRecordKey the original VSAM record key
     * @return optional position if found
     */
    Optional<PositionRecord> findByVsamRecordKey(String vsamRecordKey);

    /**
     * Get position summary statistics by fund
     * 
     * @param fundId the fund identifier
     * @return array containing [count, total units, total market value, total cost basis]
     */
    @Query("SELECT COUNT(p), SUM(p.units), SUM(p.marketValue), SUM(p.costBasis) " +
           "FROM PositionRecord p WHERE p.fundId = :fundId AND p.status = 'A'")
    Object[] getPositionStatsByFund(@Param("fundId") String fundId);

    /**
     * Find top positions by market value
     * 
     * @param limit number of positions to return
     * @return list of top positions by market value
     */
    @Query(value = "SELECT p FROM PositionRecord p WHERE p.status = 'A' ORDER BY p.marketValue DESC")
    List<PositionRecord> findTopPositionsByMarketValue();

    /**
     * Check if position exists for account and fund
     * 
     * @param accountNumber the account number
     * @param fundId the fund identifier
     * @return true if position exists
     */
    boolean existsByAccountNumberAndFundId(String accountNumber, String fundId);
}
