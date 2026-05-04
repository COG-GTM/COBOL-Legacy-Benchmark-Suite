package com.portfolio.repository;

import com.portfolio.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Log repository - replaces COBOL AUDPROC sequential file writes.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByPortfolioId(String portfolioId);

    List<AuditLog> findByAuditTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByAuditType(String auditType);
}
