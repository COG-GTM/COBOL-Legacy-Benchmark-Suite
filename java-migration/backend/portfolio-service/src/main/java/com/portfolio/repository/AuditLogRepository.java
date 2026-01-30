package com.portfolio.repository;

import com.portfolio.model.entity.AuditLog;
import com.portfolio.model.enums.AuditAction;
import com.portfolio.model.enums.AuditStatus;
import com.portfolio.model.enums.AuditType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPortfolioId(String portfolioId);

    List<AuditLog> findByAccountNo(String accountNo);

    List<AuditLog> findByType(AuditType type);

    List<AuditLog> findByAction(AuditAction action);

    List<AuditLog> findByStatus(AuditStatus status);

    List<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<AuditLog> findByPortfolioIdAndTimestampBetween(
            String portfolioId, LocalDateTime startTime, LocalDateTime endTime);

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByProgram(String program);

    long countByStatus(AuditStatus status);

    long countByPortfolioId(String portfolioId);
}
