package com.coggtm.portfolio.api;

import com.coggtm.portfolio.domain.TransactionRecord;
import com.coggtm.portfolio.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for transaction inquiry.
 * Maps to COBOL INQONLN transaction history inquiry screen.
 */
@RestController
@RequestMapping("/api/portfolios/{portfolioId}")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionRecord>> getTransactions(
            @PathVariable String portfolioId) {
        List<TransactionRecord> transactions = transactionRepository.findByPortfolioId(portfolioId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransactionRecord>> getHistory(
            @PathVariable String portfolioId) {
        // TODO: Return position history from POSHIST table once PositionHistoryService is implemented
        List<TransactionRecord> transactions = transactionRepository.findByPortfolioId(portfolioId);
        return ResponseEntity.ok(transactions);
    }
}
