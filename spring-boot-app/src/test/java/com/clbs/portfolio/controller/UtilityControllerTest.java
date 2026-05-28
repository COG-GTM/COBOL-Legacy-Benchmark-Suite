package com.clbs.portfolio.controller;

import com.clbs.portfolio.service.maintenance.MaintenanceResult;
import com.clbs.portfolio.service.maintenance.MaintenanceService;
import com.clbs.portfolio.service.monitoring.MonitoringService;
import com.clbs.portfolio.service.validation.DataValidationService;
import com.clbs.portfolio.service.validation.ValidationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UtilityController.class)
class UtilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataValidationService dataValidationService;

    @MockBean
    private MaintenanceService maintenanceService;

    @MockBean
    private MonitoringService monitoringService;

    @Test
    void validate_returnsResults() throws Exception {
        Map<String, ValidationResult> results = new LinkedHashMap<>();
        ValidationResult vr = new ValidationResult("INTEGRITY");
        results.put("INTEGRITY", vr);
        when(dataValidationService.validate(anyList())).thenReturn(results);

        mockMvc.perform(post("/api/utilities/validate")
                        .param("types", "INTEGRITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.INTEGRITY").exists())
                .andExpect(jsonPath("$.INTEGRITY.hasErrors").value(false));
    }

    @Test
    void maintenance_returnsResults() throws Exception {
        Map<String, MaintenanceResult> results = new LinkedHashMap<>();
        MaintenanceResult mr = new MaintenanceResult("ARCHIVE");
        mr.setRecordsAffected(5);
        mr.addDetail("Archived 5 records");
        results.put("ARCHIVE", mr);
        when(maintenanceService.executeMaintenance(anyList())).thenReturn(results);

        mockMvc.perform(post("/api/utilities/maintenance")
                        .param("functions", "ARCHIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ARCHIVE").exists())
                .andExpect(jsonPath("$.ARCHIVE.recordsAffected").value(5));
    }

    @Test
    void getMetrics_returnsMetrics() throws Exception {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("activeJobs", 0);
        metrics.put("recordsProcessed", 100.0);
        when(monitoringService.getCurrentMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/utilities/monitoring/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeJobs").value(0));
    }

    @Test
    void checkAlerts_returnsAlerts() throws Exception {
        List<MonitoringService.Alert> alerts = List.of(
                new MonitoringService.Alert("INFO", "SYSTEM", "All metrics within normal thresholds")
        );
        when(monitoringService.checkThresholds()).thenReturn(alerts);

        mockMvc.perform(get("/api/utilities/monitoring/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].level").value("INFO"));
    }
}
