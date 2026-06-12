package com.benchmark.portfolio.common.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.benchmark.portfolio.common.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuditLogRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private AuditLogRepository repository;

    private AuditLog audit(String userId, String portfolioId, LocalDateTime timestamp) {
        AuditLog audit = new AuditLog();
        audit.setAuditTimestamp(timestamp);
        audit.setSystemId("CICSPROD");
        audit.setUserId(userId);
        audit.setProgramId("SECMGR");
        audit.setAuditType("TRAN");
        audit.setAuditAction("INQUIRE");
        audit.setAuditStatus("SUCC");
        audit.setPortfolioId(portfolioId);
        return audit;
    }

    @BeforeEach
    void seed() {
        repository.saveAll(List.of(
                audit("OPER01", "PORT0001", LocalDateTime.of(2024, 6, 1, 9, 0)),
                audit("OPER01", "PORT0002", LocalDateTime.of(2024, 6, 1, 10, 0)),
                audit("OPER02", "PORT0001", LocalDateTime.of(2024, 6, 2, 9, 0))));
        repository.flush();
    }

    @Test
    void crudRoundTrip() {
        AuditLog created = repository.saveAndFlush(
                audit("OPER09", "PORT0009", LocalDateTime.of(2024, 7, 1, 8, 0)));
        assertThat(created.getAuditLogId()).isNotNull();

        AuditLog loaded = repository.findById(created.getAuditLogId()).orElseThrow();
        assertThat(loaded.getAuditStatus()).isEqualTo("SUCC");

        loaded.setAuditStatus("WARN");
        repository.saveAndFlush(loaded);
        assertThat(repository.findById(created.getAuditLogId()).orElseThrow().getAuditStatus())
                .isEqualTo("WARN");

        repository.deleteById(created.getAuditLogId());
        repository.flush();
        assertThat(repository.findById(created.getAuditLogId())).isEmpty();
    }

    @Test
    void auditTrailByPortfolioNewestFirst() {
        List<AuditLog> result = repository.findByPortfolioIdOrderByAuditTimestampDesc("PORT0001");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAuditTimestamp())
                .isEqualTo(LocalDateTime.of(2024, 6, 2, 9, 0));
    }

    @Test
    void auditTrailByUserNewestFirst() {
        List<AuditLog> result = repository.findByUserIdOrderByAuditTimestampDesc("OPER01");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPortfolioId()).isEqualTo("PORT0002");
    }
}
