package com.portfolio.repository;

import com.portfolio.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByAuditTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByResourceName(String resourceName);
}
