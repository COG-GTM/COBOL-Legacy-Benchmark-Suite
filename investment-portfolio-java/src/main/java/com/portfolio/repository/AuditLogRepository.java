package com.portfolio.repository;

import com.portfolio.entity.AuditAction;
import com.portfolio.entity.AuditLog;
import com.portfolio.entity.AuditType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByPortfolioId(String portfolioId);

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByType(AuditType type);

    List<AuditLog> findByAction(AuditAction action);

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime start, LocalDateTime end);
}
