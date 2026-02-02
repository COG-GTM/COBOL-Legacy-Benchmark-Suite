package com.portfolio.repository;

import com.portfolio.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity
 * Provides data access operations for security audit trail
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(String userId);

    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    List<AuditLog> findByAuditType(AuditLog.AuditType auditType);

    List<AuditLog> findByAction(AuditLog.AuditAction action);

    List<AuditLog> findByStatus(AuditLog.AuditStatus status);

    List<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<AuditLog> findByPortfolioId(String portfolioId);

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.action = :action " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findUserActions(@Param("userId") String userId, 
                                    @Param("action") AuditLog.AuditAction action);

    @Query("SELECT a FROM AuditLog a WHERE a.status = 'FAIL' ORDER BY a.timestamp DESC")
    List<AuditLog> findFailedOperations();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId AND a.action = 'LOGIN' " +
           "AND a.timestamp > :since")
    long countLoginAttempts(@Param("userId") String userId, @Param("since") LocalDateTime since);
}
