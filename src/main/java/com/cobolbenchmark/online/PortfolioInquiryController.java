package com.cobolbenchmark.online;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portfolio Inquiry Controller - migrated from INQPORT.cbl.
 * Replaces EXEC CICS READ FILE('POSFILE') with REST endpoint.
 * Replaces EXEC CICS SEND MAP('POSMAP') with JSON response.
 */
@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio Inquiry", description = "Portfolio position inquiry - migrated from INQPORT.cbl")
public class PortfolioInquiryController {

    private final PortfolioInquiryService portfolioInquiryService;

    public PortfolioInquiryController(PortfolioInquiryService portfolioInquiryService) {
        this.portfolioInquiryService = portfolioInquiryService;
    }

    @GetMapping("/{portfolioId}/positions")
    @Operation(summary = "Get portfolio positions", description = "Retrieve positions for a portfolio - replaces CICS INQPORT")
    public ResponseEntity<PositionResponse> getPositions(@PathVariable String portfolioId) {
        PositionResponse response = portfolioInquiryService.getPortfolioPositions(portfolioId);
        return ResponseEntity.ok(response);
    }
}
