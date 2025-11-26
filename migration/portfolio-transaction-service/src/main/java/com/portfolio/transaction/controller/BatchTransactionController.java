package com.portfolio.transaction.controller;

import com.portfolio.transaction.domain.dto.BatchResponse;
import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResponse;
import com.portfolio.transaction.service.TransactionOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/batch")
public class BatchTransactionController {

    private final TransactionOrchestrationService orchestrationService;
    private static final int MAX_ERRORS = 100;

    public BatchTransactionController(TransactionOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<BatchResponse> processBatch(
            @RequestBody List<TransactionRequest> requests) {

        List<TransactionResponse> results = new ArrayList<>();
        int errorCount = 0;
        int processedCount = 0;

        for (TransactionRequest request : requests) {
            if (errorCount > MAX_ERRORS) {
                break;
            }

            TransactionResponse response = orchestrationService.processTransaction(request);
            results.add(response);

            if (response.isSuccess()) {
                processedCount++;
            } else {
                errorCount++;
            }
        }

        BatchResponse batchResponse = new BatchResponse();
        batchResponse.setTotalRead(requests.size());
        batchResponse.setTotalProcessed(processedCount);
        batchResponse.setTotalErrors(errorCount);
        batchResponse.setResults(results);
        batchResponse.setTerminatedEarly(errorCount > MAX_ERRORS);

        return ResponseEntity.ok(batchResponse);
    }
}
