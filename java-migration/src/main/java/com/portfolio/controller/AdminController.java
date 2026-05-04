package com.portfolio.controller;

import com.portfolio.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SystemMonitorService monitorService;
    private final DataValidationService validationService;
    private final FileMaintenanceService maintenanceService;
    private final DatabaseStatisticsService statsService;

    public AdminController(SystemMonitorService monitorService,
                           DataValidationService validationService,
                           FileMaintenanceService maintenanceService,
                           DatabaseStatisticsService statsService) {
        this.monitorService = monitorService;
        this.validationService = validationService;
        this.maintenanceService = maintenanceService;
        this.statsService = statsService;
    }

    @GetMapping("/monitor")
    public String systemMonitor(Model model) {
        model.addAttribute("systemStatus", monitorService.getSystemStatus());
        return "admin-monitor";
    }

    @GetMapping("/validate")
    @ResponseBody
    public Map<String, Object> validateData() {
        List<String> issues = validationService.validateAllData();
        return Map.of(
                "issueCount", issues.size(),
                "issues", issues,
                "status", issues.isEmpty() ? "VALID" : "ISSUES_FOUND"
        );
    }

    @PostMapping("/maintenance")
    @ResponseBody
    public Map<String, Object> runMaintenance(@RequestParam(defaultValue = "90") int retentionDays) {
        return maintenanceService.performMaintenance(retentionDays);
    }

    @GetMapping("/statistics")
    @ResponseBody
    public Map<String, Object> getStatistics() {
        return Map.of(
                "tableCounts", statsService.getTableCounts(),
                "transactionStats", statsService.getTransactionStats(),
                "errorStats", statsService.getErrorStats()
        );
    }
}
