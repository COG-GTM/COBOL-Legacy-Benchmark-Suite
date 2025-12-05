package com.portfolio.repository;

import com.portfolio.entity.ErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Error Log entity
 * Provides data access methods for error logging operations
 */
@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, UUID> {

    /**
     * Find errors by program ID
     */
    List<ErrorLog> findByProgramId(String programId);

    /**
     * Find errors by program ID with pagination
     */
    Page<ErrorLog> findByProgramId(String programId, Pageable pageable);

    /**
     * Find errors by process date
     */
    List<ErrorLog> findByProcessDate(LocalDate processDate);

    /**
     * Find errors by process date with pagination
     */
    Page<ErrorLog> findByProcessDate(LocalDate processDate, Pageable pageable);

    /**
     * Find errors by error type
     */
    List<ErrorLog> findByErrorType(String errorType);

    /**
     * Find errors by error severity
     */
    List<ErrorLog> findByErrorSeverity(Integer errorSeverity);

    /**
     * Find errors by error code
     */
    List<ErrorLog> findByErrorCode(String errorCode);

    /**
     * Find errors by user ID
     */
    List<ErrorLog> findByUserId(String userId);

    /**
     * Find errors within timestamp range
     */
    List<ErrorLog> findByErrorTimestampBetween(OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * Find errors by process date and severity (sorted by severity descending)
     */
    @Query("SELECT e FROM ErrorLog e WHERE e.processDate = :processDate " +
            "ORDER BY e.errorSeverity DESC, e.errorTimestamp DESC")
    List<ErrorLog> findByProcessDateOrderBySeverity(@Param("processDate") LocalDate processDate);

    /**
     * Find severe errors (severity >= 3)
     */
    @Query("SELECT e FROM ErrorLog e WHERE e.errorSeverity >= 3 " +
            "AND e.processDate = :processDate ORDER BY e.errorTimestamp DESC")
    List<ErrorLog> findSevereErrors(@Param("processDate") LocalDate processDate);

    /**
     * Find recent errors
     */
    @Query("SELECT e FROM ErrorLog e WHERE e.processDate >= :startDate " +
            "ORDER BY e.errorTimestamp DESC")
    List<ErrorLog> findRecentErrors(@Param("startDate") LocalDate startDate);

    /**
     * Find recent errors with pagination
     */
    @Query("SELECT e FROM ErrorLog e WHERE e.processDate >= :startDate " +
            "ORDER BY e.errorTimestamp DESC")
    Page<ErrorLog> findRecentErrors(@Param("startDate") LocalDate startDate, Pageable pageable);

    /**
     * Count errors by process date
     */
    long countByProcessDate(LocalDate processDate);

    /**
     * Count errors by program ID
     */
    long countByProgramId(String programId);

    /**
     * Count errors by severity
     */
    long countByErrorSeverity(Integer errorSeverity);

    /**
     * Count errors by process date and severity
     */
    long countByProcessDateAndErrorSeverity(LocalDate processDate, Integer errorSeverity);

    /**
     * Delete errors older than specified date (for cleanup)
     */
    void deleteByProcessDateBefore(LocalDate retentionDate);
}
