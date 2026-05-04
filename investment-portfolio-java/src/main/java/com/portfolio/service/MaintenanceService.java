package com.portfolio.service;

import com.portfolio.repository.ErrorLogRepository;
import com.portfolio.repository.PortfolioMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);
    private static final int ERROR_LOG_RETENTION_DAYS = 90;

    private final ErrorLogRepository errorLogRepository;
    private final PortfolioMasterRepository portfolioRepository;

    private LocalDateTime lastMaintenanceRun;

    public MaintenanceService(ErrorLogRepository errorLogRepository,
                              PortfolioMasterRepository portfolioRepository) {
        this.errorLogRepository = errorLogRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @Transactional
    public Map<String, Object> runMaintenance() {
        log.info("Starting maintenance operations");
        Map<String, Object> result = new HashMap<>();

        int deletedErrors = errorLogRepository.deleteByProcessDateBefore(
                LocalDate.now().minusDays(ERROR_LOG_RETENTION_DAYS));
        result.put("errorLogsCleaned", deletedErrors);

        long totalPortfolios = portfolioRepository.count();
        result.put("totalPortfolios", totalPortfolios);

        lastMaintenanceRun = LocalDateTime.now();
        result.put("completedAt", lastMaintenanceRun);
        result.put("status", "SUCCESS");

        log.info("Maintenance completed: {} error logs cleaned", deletedErrors);
        return result;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastRun", lastMaintenanceRun);
        status.put("retentionDays", ERROR_LOG_RETENTION_DAYS);
        return status;
    }
}
