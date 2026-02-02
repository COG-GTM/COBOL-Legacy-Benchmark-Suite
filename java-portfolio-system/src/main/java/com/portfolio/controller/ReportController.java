package com.portfolio.controller;

import com.portfolio.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Report Controller - migrated from COBOL RPTPOS00, RPTAUD00, RPTSTA00
 * REST API for generating reports
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/position/{portfolioId}")
    public ResponseEntity<ReportService.PositionReport> getPositionReport(@PathVariable String portfolioId) {
        ReportService.PositionReport report = reportService.generatePositionReport(portfolioId);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    @GetMapping("/audit")
    public ResponseEntity<ReportService.AuditReport> getAuditReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(reportService.generateAuditReport(startTime, endTime));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ReportService.StatisticsReport> getStatisticsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        if (reportDate == null) {
            reportDate = LocalDate.now();
        }
        return ResponseEntity.ok(reportService.generateStatisticsReport(reportDate));
    }
}
