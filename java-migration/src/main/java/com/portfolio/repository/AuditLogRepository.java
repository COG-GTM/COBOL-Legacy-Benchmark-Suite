package com.portfolio.repository;

import com.portfolio.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPortfolioIdOrderByAuditTimestampDesc(String portfolioId);

    List<AuditLog> findByAuditTypeOrderByAuditTimestampDesc(String auditType);

    List<AuditLog> findByUserIdOrderByAuditTimestampDesc(String userId);

    List<AuditLog> findByAuditTimestampBetweenOrderByAuditTimestampDesc(
            LocalDateTime start, LocalDateTime end);

    List<AuditLog> findTop100ByOrderByAuditTimestampDesc();
}
