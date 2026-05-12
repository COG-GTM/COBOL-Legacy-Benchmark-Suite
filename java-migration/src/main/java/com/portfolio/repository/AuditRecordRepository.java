package com.portfolio.repository;

import com.portfolio.model.entity.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByPortfolioIdOrderByAuditTimestampDesc(String portfolioId);

    List<AuditRecord> findByAuditTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditRecord> findByAuditTypeAndAuditTimestampBetween(
            String auditType, LocalDateTime start, LocalDateTime end);
}
