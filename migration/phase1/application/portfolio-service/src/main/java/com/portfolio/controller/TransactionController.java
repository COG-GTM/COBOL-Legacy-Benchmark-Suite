package com.portfolio.controller;

import com.portfolio.entity.Transaction;
import com.portfolio.entity.Transaction.TransactionStatus;
import com.portfolio.entity.Transaction.TransactionType;
import com.portfolio.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Transaction operations.
 * Replaces COBOL batch programs TRNVAL00 and POSUPD00.
 * 
 * @see src/programs/batch/TRNVAL00.cbl
 * @see src/programs/batch/POSUPD00.cbl
 */
@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Transaction management operations")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieve a transaction by its UUID")
    public ResponseEntity<Transaction> findById(@PathVariable UUID id) {
        return transactionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/transaction-id/{transactionId}")
    @Operation(summary = "Get transaction by transaction ID", description = "Retrieve a transaction by its business ID")
    public ResponseEntity<Transaction> findByTransactionId(@PathVariable String transactionId) {
        return transactionService.findByTransactionId(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/portfolio/{portfolioId}")
    @Operation(summary = "Get transactions by portfolio", description = "Retrieve transactions for a portfolio")
    public ResponseEntity<Page<Transaction>> findByPortfolioId(
            @PathVariable String portfolioId,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.findByPortfolioId(portfolioId, pageable));
    }

    @GetMapping("/portfolio/{portfolioId}/date-range")
    @Operation(summary = "Get transactions by date range", description = "Retrieve transactions for a portfolio within date range")
    public ResponseEntity<List<Transaction>> findByPortfolioIdAndDateRange(
            @PathVariable String portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(transactionService.findByPortfolioIdAndDateRange(portfolioId, startDate, endDate));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get transactions by date range", description = "Retrieve all transactions within date range")
    public ResponseEntity<Page<Transaction>> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.findByDateRange(startDate, endDate, pageable));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending transactions", description = "Retrieve all pending transactions")
    public ResponseEntity<List<Transaction>> findPendingTransactions() {
        return ResponseEntity.ok(transactionService.findPendingTransactions());
    }

    @PostMapping
    @Operation(summary = "Create transaction", description = "Create a new transaction")
    public ResponseEntity<Transaction> create(
            @Valid @RequestBody Transaction transaction,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        Transaction created = transactionService.create(transaction, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{transactionId}/process")
    @Operation(summary = "Process transaction", description = "Process a pending transaction")
    public ResponseEntity<Transaction> process(
            @PathVariable String transactionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        Transaction processed = transactionService.process(transactionId, userId);
        return ResponseEntity.ok(processed);
    }

    @PostMapping("/{transactionId}/reverse")
    @Operation(summary = "Reverse transaction", description = "Reverse a completed transaction")
    public ResponseEntity<Transaction> reverse(
            @PathVariable String transactionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        Transaction reversed = transactionService.reverse(transactionId, userId);
        return ResponseEntity.ok(reversed);
    }

    @GetMapping("/portfolio/{portfolioId}/sum/{type}")
    @Operation(summary = "Sum transactions by type", description = "Calculate sum of transaction amounts by type")
    public ResponseEntity<BigDecimal> sumAmountByPortfolioAndType(
            @PathVariable String portfolioId,
            @PathVariable TransactionType type) {
        return ResponseEntity.ok(transactionService.sumAmountByPortfolioAndType(portfolioId, type));
    }

    @GetMapping("/count")
    @Operation(summary = "Count transactions", description = "Count transactions by date and status")
    public ResponseEntity<Long> countByDateAndStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionService.countByDateAndStatus(date, status));
    }
}
