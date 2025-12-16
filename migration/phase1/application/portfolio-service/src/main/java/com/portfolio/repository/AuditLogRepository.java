package com.portfolio.repository;

import com.portfolio.entity.AuditLog;
import com.portfolio.entity.AuditLog.AuditAction;
import com.portfolio.entity.AuditLog.AuditEventType;
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
 * Repository for AuditLog entity.
 * Replaces DB2 AUDITLOG table access operations.
 * 
 * @see src/copybook/common/AUDITLOG.cpy
 * @see src/programs/online/SECMGR.cbl - P300-LOG-ACCESS
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    List<AuditLog> findByPortfolioId(String portfolioId);

    Page<AuditLog> findByAuditTimestampBetween(
            OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
           "AND a.auditTimestamp >= :startTime ORDER BY a.auditTimestamp DESC")
    List<AuditLog> findRecentByUser(
            @Param("userId") String userId,
            @Param("startTime") OffsetDateTime startTime);

    @Query("SELECT a FROM AuditLog a WHERE a.status = 'FAILURE' " +
           "AND a.auditTimestamp >= :startTime ORDER BY a.auditTimestamp DESC")
    List<AuditLog> findRecentFailures(@Param("startTime") OffsetDateTime startTime);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action " +
           "AND a.auditTimestamp >= :startTime")
    long countByActionSince(@Param("action") AuditAction action, @Param("startTime") OffsetDateTime startTime);

    @Query("SELECT a FROM AuditLog a WHERE a.programId = :programId " +
           "ORDER BY a.auditTimestamp DESC")
    Page<AuditLog> findByProgram(@Param("programId") String programId, Pageable pageable);
}
