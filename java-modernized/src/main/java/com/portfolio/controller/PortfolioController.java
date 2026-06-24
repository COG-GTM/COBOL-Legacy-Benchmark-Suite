package com.portfolio.controller;

import com.portfolio.dto.PortfolioRequest;
import com.portfolio.dto.PortfolioResponse;
import com.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for portfolio operations.
 * Maps COBOL PORTMSTR.cbl commands to HTTP endpoints:
 * <pre>
 *   EVALUATE TRUE
 *       WHEN CREATE-PORT (value 'C')  -> POST   /api/portfolio
 *       WHEN READ-PORT   (value 'R')  -> GET    /api/portfolio/{id}
 *       WHEN UPDATE-PORT (value 'U')  -> PUT    /api/portfolio/{id}
 *       WHEN DELETE-PORT (value 'D')  -> DELETE /api/portfolio/{id}
 *   END-EVALUATE
 * </pre>
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /**
     * Create a new portfolio.
     * Mirrors PORTMSTR.cbl 2000-CREATE-PORTFOLIO and PORTADD.cbl 2100-VALIDATE-AND-ADD.
     */
    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @Valid @RequestBody PortfolioRequest request) {
        PortfolioResponse response = portfolioService.createPortfolio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Read a portfolio by ID.
     * Mirrors PORTMSTR.cbl 3000-READ-PORTFOLIO (keyed READ).
     */
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> readPortfolio(@PathVariable("id") String id) {
        PortfolioResponse response = portfolioService.readPortfolio(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing portfolio.
     * Mirrors PORTMSTR.cbl 4000-UPDATE-PORTFOLIO and PORTUPDT.cbl 2200-APPLY-UPDATE.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable("id") String id,
            @Valid @RequestBody PortfolioRequest request) {
        PortfolioResponse response = portfolioService.updatePortfolio(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a portfolio.
     * Mirrors PORTMSTR.cbl 5000-DELETE-PORTFOLIO and PORTDEL.cbl 2200-DELETE-RECORD.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable("id") String id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * List all portfolios (sequential read).
     * Mirrors PORTREAD.cbl 2000-PROCESS (READ PORTFOLIO-FILE NEXT RECORD).
     */
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> listPortfolios(
            @RequestParam(value = "status", required = false) String status) {
        List<PortfolioResponse> response;
        if (status != null && !status.isBlank()) {
            response = portfolioService.findByStatus(status);
        } else {
            response = portfolioService.findAllPortfolios();
        }
        return ResponseEntity.ok(response);
    }
}
