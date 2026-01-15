package com.portfolio.modernization.repository;

import com.portfolio.modernization.entity.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * History Repository Interface
 * 
 * Provides data access operations for HistoryRecord entities.
 * Modernized from COBOL VSAM file operations in HISTLD00.cbl
 * 
 * Original COBOL operations:
 * - WRITE POSHIST (HISTLD00.cbl)
 * - READ POSHIST (for audit queries)
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Repository
public interface HistoryRepository extends JpaRepository<HistoryRecord, Long> {

    /**
     * Find all history records for a portfolio
     * 
     * @param portfolioId the portfolio identifier
     * @return list of history records for the portfolio
     */
    List<HistoryRecord> findByPortfolioId(String portfolioId);

    /**
     * Find history records by portfolio ordered by date descending
     * 
     * @param portfolioId the portfolio identifier
     * @return list of history records ordered by date
     */
    List<HistoryRecord> findByPortfolioIdOrderByHistoryDateDescHistoryTimeDesc(String portfolioId);

    /**
     * Find history records by portfolio and date range
     * 
     * @param portfolioId the portfolio identifier
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of history records within the date range
     */
    @Query("SELECT h FROM HistoryRecord h WHERE h.portfolioId = :portfolioId " +
           "AND h.historyDate BETWEEN :startDate AND :endDate " +
           "ORDER BY h.historyDate DESC, h.historyTime DESC")
    List<HistoryRecord> findByPortfolioIdAndDateRange(
            @Param("portfolioId") String portfolioId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find history records by record type
     * 
     * @param recordType the record type (PT, PS, TR)
     * @return list of history records of the specified type
     */
    List<HistoryRecord> findByRecordType(String recordType);

    /**
     * Find history records by action code
     * 
     * @param actionCode the action code (A, C, D)
     * @return list of history records with the specified action
     */
    List<HistoryRecord> findByActionCode(String actionCode);

    /**
     * Find history records by record type and action code
     * 
     * @param recordType the record type
     * @param actionCode the action code
     * @return list of matching history records
     */
    List<HistoryRecord> findByRecordTypeAndActionCode(String recordType, String actionCode);

    /**
     * Find history records by reason code
     * 
     * @param reasonCode the reason code
     * @return list of history records with the specified reason
     */
    List<HistoryRecord> findByReasonCode(String reasonCode);

    /**
     * Find history records by process user
     * 
     * @param processUser the user who processed the change
     * @return list of history records processed by the user
     */
    List<HistoryRecord> findByProcessUser(String processUser);

    /**
     * Find history records processed after a specific date
     * 
     * @param processDate the process date threshold
     * @return list of history records processed after the date
     */
    List<HistoryRecord> findByProcessDateAfter(LocalDateTime processDate);

    /**
     * Find history records by date
     * 
     * @param historyDate the history date
     * @return list of history records on the specified date
     */
    List<HistoryRecord> findByHistoryDate(LocalDate historyDate);

    /**
     * Find portfolio change history
     * 
     * @param portfolioId the portfolio identifier
     * @return list of portfolio-type history records
     */
    @Query("SELECT h FROM HistoryRecord h WHERE h.portfolioId = :portfolioId " +
           "AND h.recordType = 'PT' ORDER BY h.historyDate DESC, h.historyTime DESC")
    List<HistoryRecord> findPortfolioHistory(@Param("portfolioId") String portfolioId);

    /**
     * Find position change history
     * 
     * @param portfolioId the portfolio identifier
     * @return list of position-type history records
     */
    @Query("SELECT h FROM HistoryRecord h WHERE h.portfolioId = :portfolioId " +
           "AND h.recordType = 'PS' ORDER BY h.historyDate DESC, h.historyTime DESC")
    List<HistoryRecord> findPositionHistory(@Param("portfolioId") String portfolioId);

    /**
     * Find transaction change history
     * 
     * @param portfolioId the portfolio identifier
     * @return list of transaction-type history records
     */
    @Query("SELECT h FROM HistoryRecord h WHERE h.portfolioId = :portfolioId " +
           "AND h.recordType = 'TR' ORDER BY h.historyDate DESC, h.historyTime DESC")
    List<HistoryRecord> findTransactionHistory(@Param("portfolioId") String portfolioId);

    /**
     * Find add actions for a portfolio
     * 
     * @param portfolioId the portfolio identifier
     * @return list of add action history records
     */
    @Query("SELECT h FROM HistoryRecord h WHERE h.portfolioId = :portfolioId " +
           "AND h.actionCode = 'A' ORDER BY h.historyDate DESC")
    List<HistoryRecord> findAddActions(@Param("portfolioId") String portfolioId);

    /**
     * Find change actions for a portfolio
     * 
     * @param portfolioId the portfolio identifier
     * @return list of change action history records
     */
    @Query("SELECT h FROM HistoryRecord h WHERE h.portfolioId = :portfolioId " +
           "AND h.actionCode = 'C' ORDER BY h.historyDate DESC")
    List<HistoryRecord> findChangeActions(@Param("portfolioId") String portfolioId);

    /**
     * Find delete actions for a portfolio
     * 
     * @param portfolioId the portfolio identifier
     * @return list of delete action history records
     */
    @Query("SELECT h FROM HistoryRecord h WHERE h.portfolioId = :portfolioId " +
           "AND h.actionCode = 'D' ORDER BY h.historyDate DESC")
    List<HistoryRecord> findDeleteActions(@Param("portfolioId") String portfolioId);

    /**
     * Count history records by record type
     * 
     * @param recordType the record type
     * @return count of history records
     */
    long countByRecordType(String recordType);

    /**
     * Count history records by action code
     * 
     * @param actionCode the action code
     * @return count of history records
     */
    long countByActionCode(String actionCode);

    /**
     * Count history records by portfolio
     * 
     * @param portfolioId the portfolio identifier
     * @return count of history records for the portfolio
     */
    long countByPortfolioId(String portfolioId);

    /**
     * Find history records migrated from VSAM
     * 
     * @return list of history records that were migrated from VSAM
     */
    @Query("SELECT h FROM HistoryRecord h WHERE h.vsamMigrationDate IS NOT NULL")
    List<HistoryRecord> findMigratedRecords();

    /**
     * Find history record by VSAM record key
     * Used for migration verification and audit
     * 
     * @param vsamRecordKey the original VSAM record key
     * @return optional history record if found
     */
    Optional<HistoryRecord> findByVsamRecordKey(String vsamRecordKey);

    /**
     * Get history summary by date
     * 
     * @param historyDate the history date
     * @return array containing [total count, add count, change count, delete count]
     */
    @Query("SELECT COUNT(h), " +
           "SUM(CASE WHEN h.actionCode = 'A' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN h.actionCode = 'C' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN h.actionCode = 'D' THEN 1 ELSE 0 END) " +
           "FROM HistoryRecord h WHERE h.historyDate = :historyDate")
    Object[] getHistorySummaryByDate(@Param("historyDate") LocalDate historyDate);

    /**
     * Find recent history records
     * 
     * @param startDate the start date
     * @return list of recent history records
     */
    @Query("SELECT h FROM HistoryRecord h WHERE h.historyDate >= :startDate " +
           "ORDER BY h.historyDate DESC, h.historyTime DESC")
    List<HistoryRecord> findRecentHistory(@Param("startDate") LocalDate startDate);

    /**
     * Find history records by entity ID
     * 
     * @param entityId the entity identifier
     * @return list of history records for the entity
     */
    List<HistoryRecord> findByEntityId(String entityId);

    /**
     * Find history records by entity type
     * 
     * @param entityType the entity type
     * @return list of history records for the entity type
     */
    List<HistoryRecord> findByEntityType(String entityType);

    /**
     * Find history records by correlation ID
     * Used for distributed tracing
     * 
     * @param correlationId the correlation identifier
     * @return list of related history records
     */
    List<HistoryRecord> findByCorrelationId(String correlationId);

    /**
     * Find history records by request ID
     * Used for request tracing
     * 
     * @param requestId the request identifier
     * @return list of history records for the request
     */
    List<HistoryRecord> findByRequestId(String requestId);

    /**
     * Delete history records older than a specific date
     * Used for data retention management
     * 
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    @Query("DELETE FROM HistoryRecord h WHERE h.historyDate < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDate cutoffDate);
}
