package com.clbs.portfolio.controller;

import com.clbs.portfolio.service.report.AuditReportService;
import com.clbs.portfolio.service.report.PositionReportService;
import com.clbs.portfolio.service.report.SystemStatsReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PositionReportService positionReportService;

    @MockBean
    private AuditReportService auditReportService;

    @MockBean
    private SystemStatsReportService systemStatsReportService;

    @Test
    void getPositionReport_text() throws Exception {
        when(positionReportService.generateReport(any(LocalDate.class), eq("text")))
                .thenReturn("DAILY POSITION REPORT");

        mockMvc.perform(get("/api/reports/positions")
                        .param("date", "2024-01-15")
                        .param("format", "text"))
                .andExpect(status().isOk())
                .andExpect(content().string("DAILY POSITION REPORT"));
    }

    @Test
    void getPositionReport_csv() throws Exception {
        when(positionReportService.generateReport(any(LocalDate.class), eq("csv")))
                .thenReturn("Portfolio ID,Investment ID");

        mockMvc.perform(get("/api/reports/positions")
                        .param("date", "2024-01-15")
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(content().string("Portfolio ID,Investment ID"));
    }

    @Test
    void getAuditReport() throws Exception {
        when(auditReportService.generateReport(any(LocalDate.class), any(LocalDate.class), eq("text")))
                .thenReturn("SYSTEM AUDIT REPORT");

        mockMvc.perform(get("/api/reports/audit")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-15")
                        .param("format", "text"))
                .andExpect(status().isOk())
                .andExpect(content().string("SYSTEM AUDIT REPORT"));
    }

    @Test
    void getStatisticsReport() throws Exception {
        when(systemStatsReportService.generateReport(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn("SYSTEM STATISTICS REPORT");

        mockMvc.perform(get("/api/reports/statistics")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(content().string("SYSTEM STATISTICS REPORT"));
    }

    @Test
    void getPositionReport_missingDate_returns400() throws Exception {
        mockMvc.perform(get("/api/reports/positions"))
                .andExpect(status().isBadRequest());
    }
}
