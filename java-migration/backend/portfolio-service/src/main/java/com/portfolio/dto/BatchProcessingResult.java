package com.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessingResult {

    private int recordsRead;
    private int recordsProcessed;
    private int recordsWritten;
    private int errorCount;
    private String status;
    private String message;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<TransactionResponse> processedTransactions = new ArrayList<>();

    public void incrementRead() {
        recordsRead++;
    }

    public void incrementProcessed() {
        recordsProcessed++;
    }

    public void incrementWritten() {
        recordsWritten++;
    }

    public void incrementError() {
        errorCount++;
    }

    public void addError(String error) {
        errors.add(error);
        incrementError();
    }

    public void addProcessedTransaction(TransactionResponse transaction) {
        processedTransactions.add(transaction);
        incrementProcessed();
    }
}
