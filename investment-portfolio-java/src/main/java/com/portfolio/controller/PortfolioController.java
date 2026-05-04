package com.portfolio.controller;

import com.portfolio.dto.PortfolioCreateRequest;
import com.portfolio.dto.PortfolioUpdateRequest;
import com.portfolio.dto.TransactionRequest;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.entity.TransactionHistory;
import com.portfolio.service.PortfolioService;
import com.portfolio.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolio Management", description = "CRUD operations for portfolio management")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final TransactionService transactionService;

    public PortfolioController(PortfolioService portfolioService,
                               TransactionService transactionService) {
        this.portfolioService = portfolioService;
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Create a new portfolio")
    public ResponseEntity<PortfolioMaster> createPortfolio(
            @Valid @RequestBody PortfolioCreateRequest request) {
        PortfolioMaster created = portfolioService.createPortfolio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get portfolio by ID")
    public ResponseEntity<PortfolioMaster> getPortfolio(@PathVariable("id") String id) {
        PortfolioMaster portfolio = portfolioService.readPortfolio(id);
        return ResponseEntity.ok(portfolio);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing portfolio")
    public ResponseEntity<PortfolioMaster> updatePortfolio(
            @PathVariable("id") String id,
            @Valid @RequestBody PortfolioUpdateRequest request) {
        PortfolioMaster updated = portfolioService.updatePortfolio(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a portfolio")
    public ResponseEntity<Void> deletePortfolio(@PathVariable("id") String id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List all portfolios")
    public ResponseEntity<List<PortfolioMaster>> listPortfolios() {
        return ResponseEntity.ok(portfolioService.findAll());
    }

    @GetMapping("/active")
    @Operation(summary = "List active portfolios")
    public ResponseEntity<List<PortfolioMaster>> listActivePortfolios() {
        return ResponseEntity.ok(portfolioService.getActivePortfolios());
    }

    @PostMapping("/{id}/transactions")
    @Operation(summary = "Process a transaction for a portfolio")
    public ResponseEntity<TransactionHistory> processTransaction(
            @PathVariable("id") String id,
            @Valid @RequestBody TransactionRequest request) {
        request.setPortfolioId(id);
        TransactionHistory result = transactionService.processTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
