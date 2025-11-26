package com.portfolio.transaction.controller;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResponse;
import com.portfolio.transaction.domain.entity.Transaction;
import com.portfolio.transaction.repository.TransactionRepository;
import com.portfolio.transaction.service.TransactionOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionOrchestrationService orchestrationService;
    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionOrchestrationService orchestrationService,
                                 TransactionRepository transactionRepository) {
        this.orchestrationService = orchestrationService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> processTransaction(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = orchestrationService.processTransaction(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.unprocessableEntity().body(response);
        }
    }

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<Transaction>> getTransactionHistory(
            @PathVariable String portfolioId) {
        List<Transaction> transactions = 
            transactionRepository.findByPortfolioIdOrderByProcessedAtDesc(portfolioId);
        return ResponseEntity.ok(transactions);
    }
}
