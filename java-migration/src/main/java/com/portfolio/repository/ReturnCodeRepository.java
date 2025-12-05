package com.portfolio.repository;

import com.portfolio.entity.ReturnCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Return Code entity
 * Provides data access methods for batch return code tracking
 */
@Repository
public interface ReturnCodeRepository extends JpaRepository<ReturnCode, UUID> {

    /**
     * Find return codes by program ID
     */
    List<ReturnCode> findByProgramId(String programId);

    /**
     * Find return codes by program ID with pagination
     */
    Page<ReturnCode> findByProgramId(String programId, Pageable pageable);

    /**
     * Find return codes by status code
     */
    List<ReturnCode> findByStatusCode(String statusCode);

    /**
     * Find return codes within timestamp range
     */
    List<ReturnCode> findByLogTimestampBetween(OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * Find return codes by program ID within timestamp range
     */
    List<ReturnCode> findByProgramIdAndLogTimestampBetween(
            String programId, OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * Find latest return code by program ID
     */
    @Query("SELECT r FROM ReturnCode r WHERE r.programId = :programId " +
            "ORDER BY r.logTimestamp DESC LIMIT 1")
    Optional<ReturnCode> findLatestByProgramId(@Param("programId") String programId);

    /**
     * Find return codes with errors (status E or F)
     */
    @Query("SELECT r FROM ReturnCode r WHERE r.statusCode IN ('E', 'F') " +
            "AND r.logTimestamp >= :startTime ORDER BY r.logTimestamp DESC")
    List<ReturnCode> findErrorReturnCodes(@Param("startTime") OffsetDateTime startTime);

    /**
     * Find return codes with warnings
     */
    @Query("SELECT r FROM ReturnCode r WHERE r.statusCode = 'W' " +
            "AND r.logTimestamp >= :startTime ORDER BY r.logTimestamp DESC")
    List<ReturnCode> findWarningReturnCodes(@Param("startTime") OffsetDateTime startTime);

    /**
     * Find successful return codes
     */
    @Query("SELECT r FROM ReturnCode r WHERE r.statusCode = 'S' " +
            "AND r.logTimestamp >= :startTime ORDER BY r.logTimestamp DESC")
    List<ReturnCode> findSuccessfulReturnCodes(@Param("startTime") OffsetDateTime startTime);

    /**
     * Get highest return code by program ID within timestamp range
     */
    @Query("SELECT MAX(r.highestCode) FROM ReturnCode r WHERE r.programId = :programId " +
            "AND r.logTimestamp BETWEEN :startTime AND :endTime")
    Integer getHighestReturnCode(
            @Param("programId") String programId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Count return codes by program ID
     */
    long countByProgramId(String programId);

    /**
     * Count return codes by status code
     */
    long countByStatusCode(String statusCode);

    /**
     * Count error return codes within timestamp range
     */
    @Query("SELECT COUNT(r) FROM ReturnCode r WHERE r.statusCode IN ('E', 'F') " +
            "AND r.logTimestamp BETWEEN :startTime AND :endTime")
    long countErrorReturnCodes(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);
}
