package com.clbs.portfolio.config;

import com.clbs.portfolio.service.report.AuditReportService;
import com.clbs.portfolio.service.report.PositionReportService;
import com.clbs.portfolio.service.report.SystemStatsReportService;
import com.clbs.portfolio.service.maintenance.MaintenanceService;
import com.clbs.portfolio.service.validation.DataValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ReportScheduleConfig {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduleConfig.class);

    private final PositionReportService positionReportService;
    private final AuditReportService auditReportService;
    private final SystemStatsReportService systemStatsReportService;
    private final DataValidationService dataValidationService;
    private final MaintenanceService maintenanceService;

    public ReportScheduleConfig(PositionReportService positionReportService,
                                AuditReportService auditReportService,
                                SystemStatsReportService systemStatsReportService,
                                DataValidationService dataValidationService,
                                MaintenanceService maintenanceService) {
        this.positionReportService = positionReportService;
        this.auditReportService = auditReportService;
        this.systemStatsReportService = systemStatsReportService;
        this.dataValidationService = dataValidationService;
        this.maintenanceService = maintenanceService;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void scheduledPositionReport() {
        log.info("Running scheduled position report");
        positionReportService.generateReport(LocalDate.now(), "text");
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void scheduledAuditReport() {
        log.info("Running scheduled audit report");
        LocalDate today = LocalDate.now();
        auditReportService.generateReport(today.minusDays(1), today, "text");
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void scheduledStatisticsReport() {
        log.info("Running scheduled statistics report");
        LocalDate today = LocalDate.now();
        systemStatsReportService.generateReport(today.minusDays(1), today);
    }

    @Scheduled(cron = "0 0 2 * * SUN")
    public void scheduledValidation() {
        log.info("Running scheduled data validation");
        dataValidationService.validate(List.of("INTEGRITY", "XREF", "FORMAT", "BALANCE"));
    }

    @Scheduled(cron = "0 0 1 * * SAT")
    public void scheduledMaintenance() {
        log.info("Running scheduled maintenance");
        maintenanceService.executeMaintenance(List.of("ARCHIVE", "CLEANUP", "REORG", "ANALYZE"));
    }
}
