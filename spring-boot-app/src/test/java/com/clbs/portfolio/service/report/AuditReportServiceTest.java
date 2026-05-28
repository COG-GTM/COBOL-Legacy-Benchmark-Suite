package com.clbs.portfolio.service.report;

import com.clbs.portfolio.config.ReportConfig;
import com.clbs.portfolio.entity.AuditRecord;
import com.clbs.portfolio.entity.ErrorLog;
import com.clbs.portfolio.repository.AuditRecordRepository;
import com.clbs.portfolio.repository.ErrorLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditReportServiceTest {

    @Mock
    private AuditRecordRepository auditRecordRepository;

    @Mock
    private ErrorLogRepository errorLogRepository;

    @Mock
    private ReportConfig reportConfig;

    @InjectMocks
    private AuditReportService auditReportService;

    @TempDir
    Path tempDir;

    private List<AuditRecord> testAuditRecords;
    private List<ErrorLog> testErrorLogs;

    @BeforeEach
    void setUp() {
        when(reportConfig.getOutputDirectory()).thenReturn(tempDir.toString());

        testAuditRecords = List.of(
                AuditRecord.builder()
                        .timestamp(LocalDateTime.now())
                        .systemId("SYS001")
                        .userId("USER01")
                        .program("PORTADD")
                        .auditType("USER")
                        .action("LOGIN")
                        .status("SUCC")
                        .portfolioId("PORT0001")
                        .message("User login successful")
                        .build(),
                AuditRecord.builder()
                        .timestamp(LocalDateTime.now())
                        .systemId("SYS001")
                        .userId("USER01")
                        .program("PORTUPDT")
                        .auditType("TRAN")
                        .action("UPDATE")
                        .status("SUCC")
                        .portfolioId("PORT0001")
                        .beforeImage("old value")
                        .afterImage("new value")
                        .message("Portfolio updated")
                        .build()
        );

        testErrorLogs = List.of(
                ErrorLog.builder()
                        .errorTimestamp(LocalDateTime.now())
                        .programId("PORTADD")
                        .errorType("A")
                        .errorSeverity(3)
                        .errorCode("ERR001")
                        .errorMessage("Validation error")
                        .build()
        );
    }

    @Test
    void generateTextReport_shouldContainHeader() {
        when(auditRecordRepository.findByTimestampBetween(any(), any())).thenReturn(testAuditRecords);
        when(errorLogRepository.findByErrorTimestampBetween(any(), any())).thenReturn(testErrorLogs);

        String report = auditReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now(), "text");

        assertThat(report).contains("SYSTEM AUDIT REPORT");
        assertThat(report).contains("REPORT PERIOD:");
    }

    @Test
    void generateTextReport_shouldContainSecuritySection() {
        when(auditRecordRepository.findByTimestampBetween(any(), any())).thenReturn(testAuditRecords);
        when(errorLogRepository.findByErrorTimestampBetween(any(), any())).thenReturn(testErrorLogs);

        String report = auditReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now(), "text");

        assertThat(report).contains("SECURITY AUDIT TRAIL");
        assertThat(report).contains("LOGIN");
    }

    @Test
    void generateTextReport_shouldContainProcessAudit() {
        when(auditRecordRepository.findByTimestampBetween(any(), any())).thenReturn(testAuditRecords);
        when(errorLogRepository.findByErrorTimestampBetween(any(), any())).thenReturn(testErrorLogs);

        String report = auditReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now(), "text");

        assertThat(report).contains("PROCESS AUDIT");
        assertThat(report).contains("UPDATE");
        assertThat(report).contains("Before:");
        assertThat(report).contains("After:");
    }

    @Test
    void generateTextReport_shouldContainErrorSummary() {
        when(auditRecordRepository.findByTimestampBetween(any(), any())).thenReturn(testAuditRecords);
        when(errorLogRepository.findByErrorTimestampBetween(any(), any())).thenReturn(testErrorLogs);

        String report = auditReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now(), "text");

        assertThat(report).contains("ERROR SUMMARY");
        assertThat(report).contains("ERR001");
    }

    @Test
    void generateTextReport_shouldContainControlVerification() {
        when(auditRecordRepository.findByTimestampBetween(any(), any())).thenReturn(testAuditRecords);
        when(errorLogRepository.findByErrorTimestampBetween(any(), any())).thenReturn(testErrorLogs);

        String report = auditReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now(), "text");

        assertThat(report).contains("CONTROL VERIFICATION");
        assertThat(report).contains("Verification Status:");
    }

    @Test
    void generateCsvReport_shouldContainAllSections() {
        when(auditRecordRepository.findByTimestampBetween(any(), any())).thenReturn(testAuditRecords);
        when(errorLogRepository.findByErrorTimestampBetween(any(), any())).thenReturn(testErrorLogs);

        String report = auditReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now(), "csv");

        assertThat(report).contains("SECURITY AUDIT TRAIL");
        assertThat(report).contains("PROCESS AUDIT");
        assertThat(report).contains("ERROR SUMMARY");
        assertThat(report).contains("AUDIT SUMMARY");
    }
}
