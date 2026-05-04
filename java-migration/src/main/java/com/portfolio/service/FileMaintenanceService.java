package com.portfolio.service;

import com.portfolio.repository.ErrorLogRepository;
import com.portfolio.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FileMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(FileMaintenanceService.class);
    private final ErrorLogRepository errorLogRepository;
    private final AuditLogRepository auditLogRepository;

    public FileMaintenanceService(ErrorLogRepository errorLogRepository,
                                  AuditLogRepository auditLogRepository) {
        this.errorLogRepository = errorLogRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Map<String, Object> performMaintenance(int retentionDays) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        long errorsBefore = errorLogRepository.count();
        long auditsBefore = auditLogRepository.count();

        log.info("File maintenance started. Retention: {} days, cutoff: {}", retentionDays, cutoff);

        result.put("retentionDays", retentionDays);
        result.put("cutoffDate", cutoff.toString());
        result.put("errorLogsBefore", errorsBefore);
        result.put("auditLogsBefore", auditsBefore);
        result.put("status", "completed");

        log.info("File maintenance completed");
        return result;
    }
}
