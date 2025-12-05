package com.portfolio.modernization.controller;

import com.portfolio.modernization.model.entity.Portfolio;
import com.portfolio.modernization.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/portfolios")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfolio management APIs")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    @Operation(summary = "Get all portfolios")
    public ResponseEntity<List<Portfolio>> getAllPortfolios() {
        return ResponseEntity.ok(portfolioService.findAll());
    }

    @GetMapping("/{portfolioId}")
    @Operation(summary = "Get portfolio by ID")
    public ResponseEntity<Portfolio> getPortfolioById(@PathVariable String portfolioId) {
        return portfolioService.findById(portfolioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Get portfolios by client ID")
    public ResponseEntity<List<Portfolio>> getPortfoliosByClientId(@PathVariable String clientId) {
        return ResponseEntity.ok(portfolioService.findByClientId(clientId));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active portfolios")
    public ResponseEntity<List<Portfolio>> getActivePortfolios() {
        return ResponseEntity.ok(portfolioService.findActivePortfolios());
    }

    @PostMapping
    @Operation(summary = "Create a new portfolio")
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody Portfolio portfolio) {
        return ResponseEntity.ok(portfolioService.save(portfolio));
    }

    @PutMapping("/{portfolioId}")
    @Operation(summary = "Update an existing portfolio")
    public ResponseEntity<Portfolio> updatePortfolio(@PathVariable String portfolioId, @RequestBody Portfolio portfolio) {
        return portfolioService.findById(portfolioId)
                .map(existing -> {
                    portfolio.setPortfolioId(portfolioId);
                    return ResponseEntity.ok(portfolioService.save(portfolio));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{portfolioId}")
    @Operation(summary = "Delete a portfolio")
    public ResponseEntity<Void> deletePortfolio(@PathVariable String portfolioId) {
        return portfolioService.findById(portfolioId)
                .map(existing -> {
                    portfolioService.deleteById(portfolioId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
