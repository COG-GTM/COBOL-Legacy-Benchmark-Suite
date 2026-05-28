package com.clbs.portfolio.service.report;

import com.clbs.portfolio.config.ReportConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.explore.JobExplorer;

import jakarta.persistence.EntityManager;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemStatsReportServiceTest {

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private ReportConfig reportConfig;

    @Mock
    private EntityManager entityManager;

    private SystemStatsReportService systemStatsReportService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(reportConfig.getOutputDirectory()).thenReturn(tempDir.toString());
        systemStatsReportService = new SystemStatsReportService(jobExplorer, reportConfig);
    }

    @Test
    void generateReport_shouldContainHeader() {
        when(jobExplorer.getJobNames()).thenReturn(List.of());

        String report = systemStatsReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(report).contains("SYSTEM STATISTICS AND PERFORMANCE REPORT");
        assertThat(report).contains("REPORT PERIOD:");
    }

    @Test
    void generateReport_shouldContainDatabaseStatistics() {
        when(jobExplorer.getJobNames()).thenReturn(List.of());

        String report = systemStatsReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(report).contains("DATABASE STATISTICS");
    }

    @Test
    void generateReport_shouldContainBatchJobStatistics() {
        when(jobExplorer.getJobNames()).thenReturn(List.of());

        String report = systemStatsReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(report).contains("BATCH JOB STATISTICS");
        assertThat(report).contains("BATCH JOBS:");
        assertThat(report).contains("SUCCESS RATE:");
    }

    @Test
    void generateReport_shouldContainResourceUtilization() {
        when(jobExplorer.getJobNames()).thenReturn(List.of());

        String report = systemStatsReportService.generateReport(
                LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(report).contains("RESOURCE UTILIZATION SUMMARY");
        assertThat(report).contains("TOTAL MEMORY:");
        assertThat(report).contains("MEMORY UTILIZATION:");
    }
}
