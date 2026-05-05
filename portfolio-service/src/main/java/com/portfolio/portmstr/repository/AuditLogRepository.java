package com.portfolio.portmstr.repository;

import com.portfolio.portmstr.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Audit Log records.
 * Replaces COBOL CALL 'AUDPROC' audit logging mechanism.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPortfolioId(String portfolioId);

    List<AuditLog> findByAuditTimestampBetween(LocalDateTime start, LocalDateTime end);
}
