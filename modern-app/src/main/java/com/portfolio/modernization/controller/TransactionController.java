package com.portfolio.modernization.controller;

import com.portfolio.modernization.model.entity.Transaction;
import com.portfolio.modernization.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Transaction management APIs")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Get all transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable String transactionId) {
        return transactionService.findById(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/portfolio/{portfolioId}")
    @Operation(summary = "Get transactions by portfolio ID")
    public ResponseEntity<List<Transaction>> getTransactionsByPortfolioId(@PathVariable String portfolioId) {
        return ResponseEntity.ok(transactionService.findByPortfolioId(portfolioId));
    }

    @GetMapping("/portfolio/{portfolioId}/range")
    @Operation(summary = "Get transactions by portfolio ID and date range")
    public ResponseEntity<List<Transaction>> getTransactionsByPortfolioIdAndDateRange(
            @PathVariable String portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(transactionService.findByPortfolioIdAndDateRange(portfolioId, startDate, endDate));
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.save(transaction));
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple transactions")
    public ResponseEntity<List<Transaction>> createTransactions(@RequestBody List<Transaction> transactions) {
        return ResponseEntity.ok(transactionService.saveAll(transactions));
    }
}
