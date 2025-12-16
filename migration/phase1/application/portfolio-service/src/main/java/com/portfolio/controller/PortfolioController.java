package com.portfolio.controller;

import com.portfolio.entity.Portfolio;
import com.portfolio.entity.Portfolio.PortfolioStatus;
import com.portfolio.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Portfolio operations.
 * Replaces CICS INQPORT transaction functionality.
 * 
 * @see src/programs/online/INQPORT.cbl
 */
@RestController
@RequestMapping("/v1/portfolios")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfolio management operations")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    @Operation(summary = "List all portfolios", description = "Retrieve paginated list of portfolios")
    public ResponseEntity<Page<Portfolio>> findAll(Pageable pageable) {
        return ResponseEntity.ok(portfolioService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get portfolio by ID", description = "Retrieve a portfolio by its UUID")
    public ResponseEntity<Portfolio> findById(@PathVariable UUID id) {
        return portfolioService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/portfolio-id/{portfolioId}")
    @Operation(summary = "Get portfolio by portfolio ID", description = "Retrieve a portfolio by its business ID")
    public ResponseEntity<Portfolio> findByPortfolioId(@PathVariable String portfolioId) {
        return portfolioService.findByPortfolioId(portfolioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Get portfolios by client", description = "Retrieve all portfolios for a client")
    public ResponseEntity<List<Portfolio>> findByClientId(@PathVariable String clientId) {
        return ResponseEntity.ok(portfolioService.findByClientId(clientId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get portfolios by status", description = "Retrieve portfolios filtered by status")
    public ResponseEntity<Page<Portfolio>> findByStatus(
            @PathVariable PortfolioStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(portfolioService.findByStatus(status, pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active portfolios", description = "Retrieve all active portfolios")
    public ResponseEntity<List<Portfolio>> findActivePortfolios() {
        return ResponseEntity.ok(portfolioService.findActivePortfolios());
    }

    @PostMapping
    @Operation(summary = "Create portfolio", description = "Create a new portfolio")
    public ResponseEntity<Portfolio> create(
            @Valid @RequestBody Portfolio portfolio,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        Portfolio created = portfolioService.create(portfolio, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{portfolioId}")
    @Operation(summary = "Update portfolio", description = "Update an existing portfolio")
    public ResponseEntity<Portfolio> update(
            @PathVariable String portfolioId,
            @Valid @RequestBody Portfolio portfolio,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        Portfolio updated = portfolioService.update(portfolioId, portfolio, userId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{portfolioId}/close")
    @Operation(summary = "Close portfolio", description = "Close an existing portfolio")
    public ResponseEntity<Void> close(
            @PathVariable String portfolioId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        portfolioService.close(portfolioId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{portfolioId}/value")
    @Operation(summary = "Get portfolio value", description = "Calculate total market value of portfolio")
    public ResponseEntity<BigDecimal> calculateTotalValue(@PathVariable String portfolioId) {
        return ResponseEntity.ok(portfolioService.calculateTotalValue(portfolioId));
    }

    @GetMapping("/{portfolioId}/cost-basis")
    @Operation(summary = "Get portfolio cost basis", description = "Calculate total cost basis of portfolio")
    public ResponseEntity<BigDecimal> calculateTotalCostBasis(@PathVariable String portfolioId) {
        return ResponseEntity.ok(portfolioService.calculateTotalCostBasis(portfolioId));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "Count portfolios by status", description = "Get count of portfolios by status")
    public ResponseEntity<Long> countByStatus(@PathVariable PortfolioStatus status) {
        return ResponseEntity.ok(portfolioService.countByStatus(status));
    }
}
