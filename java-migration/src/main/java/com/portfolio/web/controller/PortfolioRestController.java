package com.portfolio.web.controller;

import com.portfolio.model.entity.Portfolio;
import com.portfolio.service.portfolio.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class PortfolioRestController {

    private final PortfolioService portfolioService;

    public PortfolioRestController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{portfolioId}")
    @PreAuthorize("hasRole('INQUIRY')")
    public ResponseEntity<Portfolio> getPortfolio(@PathVariable String portfolioId) {
        Portfolio portfolio = portfolioService.readPortfolio(portfolioId);
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping
    @PreAuthorize("hasRole('INQUIRY')")
    public ResponseEntity<List<Portfolio>> getActivePortfolios() {
        return ResponseEntity.ok(portfolioService.findActivePortfolios());
    }

    @PostMapping
    @PreAuthorize("hasRole('UPDATE')")
    public ResponseEntity<Portfolio> createPortfolio(@Valid @RequestBody Portfolio portfolio) {
        Portfolio created = portfolioService.createPortfolio(portfolio);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{portfolioId}")
    @PreAuthorize("hasRole('UPDATE')")
    public ResponseEntity<Portfolio> updatePortfolio(@PathVariable String portfolioId,
                                                     @Valid @RequestBody Portfolio portfolio) {
        portfolio.setPortfolioId(portfolioId);
        Portfolio updated = portfolioService.updatePortfolio(portfolio);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{portfolioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePortfolio(@PathVariable String portfolioId) {
        portfolioService.deletePortfolio(portfolioId);
        return ResponseEntity.noContent().build();
    }
}
