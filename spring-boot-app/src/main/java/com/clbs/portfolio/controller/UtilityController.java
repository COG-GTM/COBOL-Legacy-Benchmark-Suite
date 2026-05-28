package com.clbs.portfolio.controller;

import com.clbs.portfolio.service.maintenance.MaintenanceResult;
import com.clbs.portfolio.service.maintenance.MaintenanceService;
import com.clbs.portfolio.service.monitoring.MonitoringService;
import com.clbs.portfolio.service.validation.DataValidationService;
import com.clbs.portfolio.service.validation.ValidationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/utilities")
public class UtilityController {

    private final DataValidationService dataValidationService;
    private final MaintenanceService maintenanceService;
    private final MonitoringService monitoringService;

    public UtilityController(DataValidationService dataValidationService,
                              MaintenanceService maintenanceService,
                              MonitoringService monitoringService) {
        this.dataValidationService = dataValidationService;
        this.maintenanceService = maintenanceService;
        this.monitoringService = monitoringService;
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestParam List<String> types) {
        Map<String, ValidationResult> results = dataValidationService.validate(types);

        Map<String, Object> response = new LinkedHashMap<>();
        for (Map.Entry<String, ValidationResult> entry : results.entrySet()) {
            ValidationResult vr = entry.getValue();
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("recordsRead", vr.getRecordsRead());
            detail.put("recordsValid", vr.getRecordsValid());
            detail.put("recordsError", vr.getRecordsError());
            detail.put("hasErrors", vr.hasErrors());
            detail.put("errors", vr.getErrors().stream()
                    .map(e -> Map.of("type", e.errorType(), "key", e.key(),
                            "description", e.description()))
                    .collect(Collectors.toList()));
            response.put(entry.getKey(), detail);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/maintenance")
    public ResponseEntity<Map<String, Object>> maintenance(
            @RequestParam List<String> functions) {
        Map<String, MaintenanceResult> results = maintenanceService.executeMaintenance(functions);

        Map<String, Object> response = new LinkedHashMap<>();
        for (Map.Entry<String, MaintenanceResult> entry : results.entrySet()) {
            MaintenanceResult mr = entry.getValue();
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("recordsProcessed", mr.getRecordsProcessed());
            detail.put("recordsAffected", mr.getRecordsAffected());
            detail.put("errorsEncountered", mr.getErrorsEncountered());
            detail.put("details", mr.getDetails());
            response.put(entry.getKey(), detail);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monitoring/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(monitoringService.getCurrentMetrics());
    }

    @GetMapping("/monitoring/alerts")
    public ResponseEntity<List<MonitoringService.Alert>> checkAlerts() {
        return ResponseEntity.ok(monitoringService.checkThresholds());
    }
}
