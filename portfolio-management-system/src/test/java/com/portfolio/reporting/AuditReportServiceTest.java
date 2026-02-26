package com.portfolio.reporting;

import com.portfolio.model.SecurityLogRecord;
import com.portfolio.support.SecurityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AuditReportService.
 * Validates audit report record counts and key field values.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditReportServiceTest {

    @Autowired
    private AuditReportService reportService;

    @Autowired
    private SecurityLogRepository securityLogRepository;

    @BeforeEach
    void setUp() {
        securityLogRepository.deleteAll();
    }

    @Test
    void testGenerateEmptyReport() {
        AuditReportService.AuditReport report = reportService.generateAuditReport(LocalDate.now());
        assertThat(report.getTotalAuditEntries()).isEqualTo(0);
        assertThat(report.getTotalErrorEntries()).isEqualTo(0);
    }

    @Test
    void testGenerateReportWithAuditEntries() {
        // Create audit log entries
        for (int i = 0; i < 5; i++) {
            SecurityLogRecord log = new SecurityLogRecord();
            log.setAuditTimestamp(LocalDateTime.now());
            log.setUserId("admin");
            log.setProgram("INQONLN");
            log.setAccessType("READ");
            log.setResourceName("PORTFOLIO");
            log.setResponseCode(0);
            securityLogRepository.save(log);
        }

        AuditReportService.AuditReport report = reportService.generateAuditReport(LocalDate.now());
        assertThat(report.getTotalAuditEntries()).isEqualTo(5);
        assertThat(report.getReportDate()).isEqualTo(LocalDate.now());
    }
}
