package com.portfolio.controller;

import com.portfolio.dto.TransactionRequest;
import com.portfolio.dto.TransactionResponse;
import com.portfolio.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for transaction processing.
 * Mirrors PORTTRAN.cbl transaction processing loop:
 * <pre>
 *   PERFORM 2000-PROCESS-TRANSACTIONS
 *       UNTIL END-OF-FILE
 *       OR WS-ERROR-COUNT > 100
 * </pre>
 */
@RestController
@RequestMapping("/api/portfolio")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Process a single transaction.
     * Mirrors PORTTRAN.cbl 2200-UPDATE-POSITIONS dispatching to:
     * 2210-PROCESS-BUY, 2220-PROCESS-SELL, 2230-PROCESS-TRANSFER, 2240-PROCESS-FEE
     */
    @PostMapping("/transaction")
    public ResponseEntity<TransactionResponse> processTransaction(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.processTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get transactions for a portfolio.
     * Mirrors sequential read of TRANHIST VSAM file filtered by portfolio.
     */
    @GetMapping("/{portfolioId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable("portfolioId") String portfolioId) {
        List<TransactionResponse> response = transactionService.findByPortfolioId(portfolioId);
        return ResponseEntity.ok(response);
    }
}
