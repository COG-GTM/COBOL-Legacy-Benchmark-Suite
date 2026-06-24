package com.portfolio.repository;

import com.portfolio.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for AuditLog entity.
 * Mirrors the audit trail writing in PORTTRAN.cbl (2300-UPDATE-AUDIT-TRAIL)
 * and PORTDEL.cbl (2300-WRITE-AUDIT).
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPortfolioId(String portfolioId);

    List<AuditLog> findByAction(String action);
}
