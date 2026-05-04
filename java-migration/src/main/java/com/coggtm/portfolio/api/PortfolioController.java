package com.coggtm.portfolio.api;

import com.coggtm.portfolio.domain.Portfolio;
import com.coggtm.portfolio.exception.PortfolioException;
import com.coggtm.portfolio.service.PortfolioService;
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

/**
 * REST controller for portfolio operations.
 * Maps to COBOL INQONLN portfolio inquiry and PORTMSTR.cbl CRUD.
 */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Portfolio> getPortfolio(@PathVariable String id) {
        return portfolioService.readPortfolio(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PortfolioException("Portfolio not found: " + id));
    }

    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody Portfolio portfolio) {
        Portfolio created = portfolioService.createPortfolio(portfolio);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Portfolio> updatePortfolio(@PathVariable String id,
                                                     @RequestBody Portfolio portfolio) {
        portfolio.setPortfolioId(id);
        Portfolio updated = portfolioService.updatePortfolio(portfolio);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable String id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }
}
