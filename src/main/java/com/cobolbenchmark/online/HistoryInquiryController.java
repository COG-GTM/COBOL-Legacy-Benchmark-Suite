package com.cobolbenchmark.online;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * History Inquiry Controller - migrated from INQHIST.cbl.
 * Retrieves transaction history from DB2 POSHIST table.
 * Replaces EXEC CICS SEND MAP('HISMAP') with JSON response.
 */
@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "History Inquiry", description = "Transaction history inquiry - migrated from INQHIST.cbl")
public class HistoryInquiryController {

    private final HistoryInquiryService historyInquiryService;

    public HistoryInquiryController(HistoryInquiryService historyInquiryService) {
        this.historyInquiryService = historyInquiryService;
    }

    @GetMapping("/{portfolioId}/history")
    @Operation(summary = "Get transaction history", description = "Retrieve transaction history for a portfolio - replaces CICS INQHIST")
    public ResponseEntity<HistoryResponse> getHistory(@PathVariable String portfolioId) {
        HistoryResponse response = historyInquiryService.getTransactionHistory(portfolioId);
        return ResponseEntity.ok(response);
    }
}
