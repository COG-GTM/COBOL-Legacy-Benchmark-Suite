package com.portfolio.repository;

import com.portfolio.model.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Audit Record entity.
 * Replaces: DB2 AUDITLOG INSERT in SECMGR.cbl P300-LOG-ACCESS and AUDPROC.cbl.
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByUserId(String userId);

    List<AuditRecord> findByPortfolioId(String portfolioId);

    List<AuditRecord> findByAuditTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditRecord> findByAuditTypeAndAuditTimestampBetween(
            String auditType, LocalDateTime start, LocalDateTime end);

    List<AuditRecord> findByUserIdAndAuditTimestampBetween(
            String userId, LocalDateTime start, LocalDateTime end);
}
