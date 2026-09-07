package com.portfolio.repository;

import com.portfolio.domain.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findByAudTimestampBetween(LocalDateTime start, LocalDateTime end);
}
