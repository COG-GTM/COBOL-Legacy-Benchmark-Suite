package com.portfolio.controller;

import com.portfolio.domain.HistoryRecord;
import com.portfolio.domain.Transaction;
import com.portfolio.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * History Controller - migrated from COBOL INQHIST
 * REST API for transaction and audit history inquiries
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<HistoryRecord>> getHistoryByPortfolio(@PathVariable String portfolioId) {
        return ResponseEntity.ok(historyService.getHistoryByPortfolio(portfolioId));
    }

    @GetMapping("/portfolio/{portfolioId}/paged")
    public ResponseEntity<Page<HistoryRecord>> getHistoryByPortfolioPaged(
            @PathVariable String portfolioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(historyService.getHistoryByPortfolioPaged(portfolioId, page, size));
    }

    @GetMapping("/transactions/{portfolioId}")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@PathVariable String portfolioId) {
        return ResponseEntity.ok(historyService.getTransactionHistory(portfolioId));
    }

    @GetMapping("/transactions/{portfolioId}/paged")
    public ResponseEntity<Page<Transaction>> getTransactionHistoryPaged(
            @PathVariable String portfolioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(historyService.getTransactionHistoryPaged(portfolioId, page, size));
    }

    @GetMapping("/transactions/{portfolioId}/range")
    public ResponseEntity<List<Transaction>> getTransactionsByDateRange(
            @PathVariable String portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(historyService.getTransactionsByDateRange(portfolioId, startDate, endDate));
    }

    @GetMapping("/range")
    public ResponseEntity<List<HistoryRecord>> getHistoryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(historyService.getHistoryByDateRange(startDate, endDate));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<HistoryRecord>> getHistoryByUser(@PathVariable String userId) {
        return ResponseEntity.ok(historyService.getHistoryByUser(userId));
    }

    @PostMapping("/load")
    public ResponseEntity<HistoryService.HistoryLoadResult> loadHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate processDate) {
        return ResponseEntity.ok(historyService.loadHistoryFromTransactions(processDate));
    }
}
