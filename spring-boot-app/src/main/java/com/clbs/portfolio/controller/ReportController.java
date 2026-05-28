package com.clbs.portfolio.controller;

import com.clbs.portfolio.service.report.AuditReportService;
import com.clbs.portfolio.service.report.PositionReportService;
import com.clbs.portfolio.service.report.SystemStatsReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final PositionReportService positionReportService;
    private final AuditReportService auditReportService;
    private final SystemStatsReportService systemStatsReportService;

    public ReportController(PositionReportService positionReportService,
                             AuditReportService auditReportService,
                             SystemStatsReportService systemStatsReportService) {
        this.positionReportService = positionReportService;
        this.auditReportService = auditReportService;
        this.systemStatsReportService = systemStatsReportService;
    }

    @GetMapping("/positions")
    public ResponseEntity<String> getPositionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "text") String format) {
        String report = positionReportService.generateReport(date, format);
        MediaType mediaType = "csv".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv")
                : MediaType.TEXT_PLAIN;
        return ResponseEntity.ok().contentType(mediaType).body(report);
    }

    @GetMapping("/audit")
    public ResponseEntity<String> getAuditReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "text") String format) {
        String report = auditReportService.generateReport(startDate, endDate, format);
        MediaType mediaType = "csv".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv")
                : MediaType.TEXT_PLAIN;
        return ResponseEntity.ok().contentType(mediaType).body(report);
    }

    @GetMapping("/statistics")
    public ResponseEntity<String> getStatisticsReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String report = systemStatsReportService.generateReport(startDate, endDate);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(report);
    }
}
