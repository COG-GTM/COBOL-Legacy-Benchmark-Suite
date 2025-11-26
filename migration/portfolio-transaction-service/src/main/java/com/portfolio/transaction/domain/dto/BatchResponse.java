package com.portfolio.transaction.domain.dto;

import java.util.List;

public class BatchResponse {

    private int totalRead;
    private int totalProcessed;
    private int totalErrors;
    private boolean terminatedEarly;
    private List<TransactionResponse> results;

    public BatchResponse() {
    }

    public int getTotalRead() {
        return totalRead;
    }

    public void setTotalRead(int totalRead) {
        this.totalRead = totalRead;
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(int totalErrors) {
        this.totalErrors = totalErrors;
    }

    public boolean isTerminatedEarly() {
        return terminatedEarly;
    }

    public void setTerminatedEarly(boolean terminatedEarly) {
        this.terminatedEarly = terminatedEarly;
    }

    public List<TransactionResponse> getResults() {
        return results;
    }

    public void setResults(List<TransactionResponse> results) {
        this.results = results;
    }
}
