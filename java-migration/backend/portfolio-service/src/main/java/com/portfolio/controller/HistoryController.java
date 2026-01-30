package com.portfolio.controller;

import com.portfolio.dto.BatchProcessingResult;
import com.portfolio.dto.HistoryLoadRequest;
import com.portfolio.model.entity.PositionHistory;
import com.portfolio.service.HistoryLoadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HistoryController {

    private final HistoryLoadService historyLoadService;

    @PostMapping("/load")
    public ResponseEntity<BatchProcessingResult> loadHistoryBatch(
            @Valid @RequestBody List<HistoryLoadRequest> requests) {
        log.info("Received history load request with {} records", requests.size());
        BatchProcessingResult result = historyLoadService.loadHistoryBatch(requests);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/load/single")
    public ResponseEntity<PositionHistory> loadSingleRecord(
            @Valid @RequestBody HistoryLoadRequest request) {
        log.info("Loading single history record for portfolio: {}", request.getPortfolioId());
        PositionHistory history = historyLoadService.loadSingleRecord(request);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<PositionHistory>> getHistoryByPortfolio(
            @PathVariable String portfolioId) {
        log.info("Fetching history for portfolio: {}", portfolioId);
        List<PositionHistory> history = historyLoadService.getHistoryByPortfolioId(portfolioId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/account/{accountNo}")
    public ResponseEntity<List<PositionHistory>> getHistoryByAccount(
            @PathVariable String accountNo) {
        log.info("Fetching history for account: {}", accountNo);
        List<PositionHistory> history = historyLoadService.getHistoryByAccountNo(accountNo);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<PositionHistory>> getHistoryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Fetching history between {} and {}", startDate, endDate);
        List<PositionHistory> history = historyLoadService.getHistoryByDateRange(startDate, endDate);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/portfolio/{portfolioId}/date-range")
    public ResponseEntity<List<PositionHistory>> getHistoryByPortfolioAndDateRange(
            @PathVariable String portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Fetching history for portfolio {} between {} and {}", portfolioId, startDate, endDate);
        List<PositionHistory> history = historyLoadService.getHistoryByPortfolioIdAndDateRange(
                portfolioId, startDate, endDate);
        return ResponseEntity.ok(history);
    }
}
