package com.benchmark.portfolio.common.repository;

import com.benchmark.portfolio.common.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link AuditLog}, replacing the DB2 AUDITLOG table written by
 * the security/audit modules (AUDITLOG.cpy).
 *
 * <p>{@code save} replicates SECMGR.cbl P300-LOG-ACCESS
 * ({@code EXEC SQL INSERT INTO AUDITLOG}); the table is insert-only.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Audit trail for one portfolio, newest first - review query over the
     * AUDITLOG rows inserted by SECMGR.cbl P300-LOG-ACCESS (AUD-PORTFOLIO-ID
     * reference).
     */
    List<AuditLog> findByPortfolioIdOrderByAuditTimestampDesc(String portfolioId);

    /**
     * Audit trail for one user, newest first - review query over the AUDITLOG
     * rows inserted by SECMGR.cbl P300-LOG-ACCESS (USER_ID column from
     * {@code EXEC CICS ASSIGN USERID}).
     */
    List<AuditLog> findByUserIdOrderByAuditTimestampDesc(String userId);
}
