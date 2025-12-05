package com.portfolio.repository;

import com.portfolio.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Audit Log entity
 * Provides data access methods for audit trail operations
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by user ID
     */
    List<AuditLog> findByUserId(String userId);

    /**
     * Find audit logs by user ID with pagination
     */
    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    /**
     * Find audit logs by portfolio ID
     */
    List<AuditLog> findByPortfolioId(String portfolioId);

    /**
     * Find audit logs by portfolio ID with pagination
     */
    Page<AuditLog> findByPortfolioId(String portfolioId, Pageable pageable);

    /**
     * Find audit logs by audit type
     */
    List<AuditLog> findByAuditType(String auditType);

    /**
     * Find audit logs by action
     */
    List<AuditLog> findByAction(String action);

    /**
     * Find audit logs by status
     */
    List<AuditLog> findByStatus(String status);

    /**
     * Find audit logs by program ID
     */
    List<AuditLog> findByProgramId(String programId);

    /**
     * Find audit logs by system ID
     */
    List<AuditLog> findBySystemId(String systemId);

    /**
     * Find audit logs within timestamp range
     */
    List<AuditLog> findByAuditTimestampBetween(OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * Find audit logs within timestamp range with pagination
     */
    Page<AuditLog> findByAuditTimestampBetween(OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable);

    /**
     * Find audit logs by user ID within timestamp range
     */
    List<AuditLog> findByUserIdAndAuditTimestampBetween(
            String userId, OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * Find audit logs by portfolio ID within timestamp range
     */
    List<AuditLog> findByPortfolioIdAndAuditTimestampBetween(
            String portfolioId, OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * Find audit logs by audit type and action
     */
    List<AuditLog> findByAuditTypeAndAction(String auditType, String action);

    /**
     * Find failed audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.status = 'FAIL' " +
            "AND a.auditTimestamp >= :startTime ORDER BY a.auditTimestamp DESC")
    List<AuditLog> findFailedAudits(@Param("startTime") OffsetDateTime startTime);

    /**
     * Find transaction audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.auditType = 'TRAN' " +
            "AND a.auditTimestamp >= :startTime ORDER BY a.auditTimestamp DESC")
    List<AuditLog> findTransactionAudits(@Param("startTime") OffsetDateTime startTime);

    /**
     * Find user action audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.auditType = 'USER' " +
            "AND a.auditTimestamp >= :startTime ORDER BY a.auditTimestamp DESC")
    List<AuditLog> findUserActionAudits(@Param("startTime") OffsetDateTime startTime);

    /**
     * Find system event audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.auditType = 'SYST' " +
            "AND a.auditTimestamp >= :startTime ORDER BY a.auditTimestamp DESC")
    List<AuditLog> findSystemEventAudits(@Param("startTime") OffsetDateTime startTime);

    /**
     * Count audit logs by user ID
     */
    long countByUserId(String userId);

    /**
     * Count audit logs by audit type
     */
    long countByAuditType(String auditType);

    /**
     * Count audit logs by status
     */
    long countByStatus(String status);

    /**
     * Count failed audits within timestamp range
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.status = 'FAIL' " +
            "AND a.auditTimestamp BETWEEN :startTime AND :endTime")
    long countFailedAudits(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);
}
