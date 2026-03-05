package com.portfolio.support;

import com.portfolio.model.SecurityLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Audit Log table.
 * Replaces COBOL SECMGR P300-LOG-ACCESS DB2 INSERT INTO AUDITLOG.
 */
@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLogRecord, Long> {

    List<SecurityLogRecord> findByUserId(String userId);

    List<SecurityLogRecord> findByAuditTimestampBetween(LocalDateTime start, LocalDateTime end);

    long countByUserId(String userId);
}
