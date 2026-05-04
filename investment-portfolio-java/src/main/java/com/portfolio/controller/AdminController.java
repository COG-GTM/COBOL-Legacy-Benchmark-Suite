package com.portfolio.controller;

import com.portfolio.service.DataValidationService;
import com.portfolio.service.MaintenanceService;
import com.portfolio.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administration", description = "System administration and maintenance endpoints")
public class AdminController {

    private final MaintenanceService maintenanceService;
    private final MonitoringService monitoringService;
    private final DataValidationService dataValidationService;

    public AdminController(MaintenanceService maintenanceService,
                           MonitoringService monitoringService,
                           DataValidationService dataValidationService) {
        this.maintenanceService = maintenanceService;
        this.monitoringService = monitoringService;
        this.dataValidationService = dataValidationService;
    }

    @PostMapping("/maintenance")
    @Operation(summary = "Run maintenance operations")
    public ResponseEntity<Map<String, Object>> runMaintenance() {
        Map<String, Object> result = maintenanceService.runMaintenance();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/maintenance")
    @Operation(summary = "Get maintenance status")
    public ResponseEntity<Map<String, Object>> getMaintenanceStatus() {
        return ResponseEntity.ok(maintenanceService.getStatus());
    }

    @GetMapping("/monitor")
    @Operation(summary = "Get system monitoring status")
    public ResponseEntity<Map<String, Object>> getMonitoringStatus() {
        return ResponseEntity.ok(monitoringService.getSystemStatus());
    }

    @PostMapping("/validate")
    @Operation(summary = "Run data validation checks")
    public ResponseEntity<Map<String, Object>> validateData() {
        Map<String, Object> result = dataValidationService.validateAll();
        return ResponseEntity.ok(result);
    }
}
