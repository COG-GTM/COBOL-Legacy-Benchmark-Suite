package com.cobolbenchmark.online;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inquiry Controller - migrated from INQONLN.cbl.
 * Replaces EXEC CICS RECEIVE MAP('INQMAP') with REST endpoint accepting JSON.
 * Replaces EXEC CICS SEND MAP('POSMAP') with JSON response output.
 * Replaces EXEC CICS HANDLE CONDITION ERROR with @ControllerAdvice.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Inquiry", description = "Portfolio inquiry operations - migrated from INQONLN.cbl")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    /**
     * Process portfolio inquiry.
     * Replaces EXEC CICS RECEIVE MAP('INQMAP') + EXEC CICS SEND MAP('POSMAP').
     */
    @PostMapping("/inquiry")
    @Operation(summary = "Portfolio inquiry", description = "Query portfolio positions - replaces CICS INQONLN transaction")
    public ResponseEntity<PositionResponse> inquiry(@RequestBody InquiryRequest request) {
        PositionResponse response = inquiryService.processInquiry(request);
        return ResponseEntity.ok(response);
    }
}
