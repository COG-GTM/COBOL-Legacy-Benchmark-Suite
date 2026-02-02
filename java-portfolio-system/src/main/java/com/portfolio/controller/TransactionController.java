package com.portfolio.controller;

import com.portfolio.domain.Transaction;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.service.AuditService;
import com.portfolio.service.PositionUpdateService;
import com.portfolio.service.TransactionValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Transaction Controller - migrated from COBOL TRNVAL00 and POSUPD00
 * REST API for transaction validation and processing
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final TransactionValidationService validationService;
    private final PositionUpdateService positionUpdateService;
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<Page<Transaction>> getTransactionsByPortfolio(
            @PathVariable String portfolioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Transaction> transactions = transactionRepository
                .findByPortfolioIdOrderByTransactionDateDesc(portfolioId, PageRequest.of(page, size));
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Transaction>> getPendingTransactions() {
        return ResponseEntity.ok(transactionRepository.findPendingTransactions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        return transactionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @RequestBody Transaction transaction,
            Authentication authentication) {
        if (authentication != null) {
            transaction.setProcessUser(authentication.getName());
        }
        
        Transaction saved = transactionRepository.save(transaction);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/validate")
    public ResponseEntity<TransactionValidationService.ValidationResult> validateTransaction(
            @RequestBody Transaction transaction,
            Authentication authentication) {
        if (authentication != null) {
            transaction.setProcessUser(authentication.getName());
        }
        
        TransactionValidationService.ValidationResult result = validationService.validateTransaction(transaction);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-batch")
    public ResponseEntity<TransactionValidationService.BatchValidationResult> validateBatch(
            @RequestBody List<Transaction> transactions,
            Authentication authentication) {
        if (authentication != null) {
            transactions.forEach(t -> t.setProcessUser(authentication.getName()));
        }
        
        TransactionValidationService.BatchValidationResult result = validationService.validateBatch(transactions);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<PositionUpdateService.UpdateResult> processTransaction(
            @PathVariable Long id,
            Authentication authentication) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (authentication != null) {
            transaction.setProcessUser(authentication.getName());
            auditService.logTransaction(authentication.getName(), transaction.getPortfolioId(),
                    transaction.getTransactionType().name(), true);
        }
        
        PositionUpdateService.UpdateResult result = positionUpdateService.processTransaction(transaction);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/process-pending")
    public ResponseEntity<PositionUpdateService.BatchUpdateResult> processPendingTransactions() {
        PositionUpdateService.BatchUpdateResult result = positionUpdateService.processPendingTransactions();
        return ResponseEntity.ok(result);
    }
}
