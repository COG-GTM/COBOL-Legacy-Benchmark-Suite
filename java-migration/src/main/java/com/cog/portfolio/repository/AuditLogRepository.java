package com.cog.portfolio.repository;

import com.cog.portfolio.domain.AuditLog;
import com.cog.portfolio.domain.AuditStatus;
import com.cog.portfolio.domain.AuditType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Replaces sequential AUDFILE (AUDPROC / RPTAUD00). */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTimestampBetweenOrderByTimestampAsc(LocalDateTime from, LocalDateTime to);

    long countByType(AuditType type);

    long countByStatus(AuditStatus status);
}
