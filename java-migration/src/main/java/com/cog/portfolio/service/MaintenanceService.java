package com.cog.portfolio.service;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.repository.AuditLogRepository;
import com.cog.portfolio.repository.ErrorLogRepository;
import com.cog.portfolio.repository.PositionHistoryRepository;
import com.cog.portfolio.repository.ReturnCodeLogRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UTLMNT00.cbl (ARCHIVE / CLEANUP / REORG / ANALYZE). VSAM reorg and IDCAMS
 * archive have no H2 equivalent, so CLEANUP (ERRLOG_CLEANUP with RETENTION_DAYS)
 * is the only function with real work; ANALYZE reports table counts.
 */
@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    /** UTLMNT00 WS-FUNCTIONS. */
    public enum Function implements CodedValue {
        ARCHIVE("ARCHIVE"), CLEANUP("CLEANUP"), REORG("REORG"), ANALYZE("ANALYZE");

        private final String code;

        Function(String code) {
            this.code = code;
        }

        @Override
        public String code() {
            return code;
        }
    }

    @ConfigurationProperties(prefix = "portfolio.maintenance")
    public record MaintenanceProperties(int retentionDays, String cron) {
    }

    public record MaintenanceResult(Function function, long recordsAffected, Map<String, Long> details) {
    }

    private final ErrorLogRepository errorLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final ReturnCodeLogRepository returnCodeLogRepository;
    private final MaintenanceProperties properties;

    public MaintenanceService(ErrorLogRepository errorLogRepository, AuditLogRepository auditLogRepository,
                              PositionHistoryRepository positionHistoryRepository,
                              ReturnCodeLogRepository returnCodeLogRepository, MaintenanceProperties properties) {
        this.errorLogRepository = errorLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.positionHistoryRepository = positionHistoryRepository;
        this.returnCodeLogRepository = returnCodeLogRepository;
        this.properties = properties;
    }

    @Transactional
    public MaintenanceResult run(Function function) {
        return switch (function) {
            case CLEANUP -> cleanup();
            case ANALYZE -> analyze();
            case ARCHIVE, REORG -> {
                log.info("{} is a no-op on H2 (VSAM/IDCAMS function)", function);
                yield new MaintenanceResult(function, 0, Map.of());
            }
        };
    }

    /** Replaces the UTLMNT JCL schedule. */
    @Scheduled(cron = "${portfolio.maintenance.cron}")
    public void scheduledCleanup() {
        run(Function.CLEANUP);
    }

    /** 2300-CLEANUP-PROCESS / ERRLOG_CLEANUP stored procedure. */
    private MaintenanceResult cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(properties.retentionDays());
        int deleted = errorLogRepository.deleteOlderThan(cutoff);
        log.info("ERRLOG cleanup removed {} rows older than {}", deleted, cutoff);
        return new MaintenanceResult(Function.CLEANUP, deleted, Map.of("ERRLOG", (long) deleted));
    }

    /** 2500-ANALYZE-PROCESS: space analysis becomes row counts. */
    private MaintenanceResult analyze() {
        Map<String, Long> counts = Map.of(
                "ERRLOG", errorLogRepository.count(),
                "AUDIT_LOG", auditLogRepository.count(),
                "POSHIST", positionHistoryRepository.count(),
                "RTNCODES", returnCodeLogRepository.count());
        return new MaintenanceResult(Function.ANALYZE, counts.values().stream().mapToLong(Long::longValue).sum(), counts);
    }
}
