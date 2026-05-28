package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditRecord entities.
 * Replaces VSAM access patterns for AUDITLOG file.
 */
@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByPortfolioId(String portfolioId);

    List<AuditRecord> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
