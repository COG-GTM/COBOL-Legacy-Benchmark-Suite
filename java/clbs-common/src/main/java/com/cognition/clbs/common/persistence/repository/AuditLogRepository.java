package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link AuditLog}. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  /** Audit trail of one portfolio, oldest first. */
  List<AuditLog> findByPortfolioIdOrderByAuditTimestampAscAuditIdAsc(String portfolioId);
}
