package com.portfolio.controller;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.Position;
import com.portfolio.service.AuditService;
import com.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Portfolio Controller - migrated from COBOL INQONLN and INQPORT
 * REST API for portfolio inquiries and management
 */
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<Portfolio>> getAllPortfolios() {
        return ResponseEntity.ok(portfolioService.getAllPortfolios());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Portfolio>> getActivePortfolios() {
        return ResponseEntity.ok(portfolioService.getActivePortfolios());
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<Portfolio> getPortfolio(
            @PathVariable String portfolioId,
            Authentication authentication) {
        Portfolio portfolio = portfolioService.getPortfolioById(portfolioId);
        
        if (authentication != null) {
            auditService.logInquiry(authentication.getName(), portfolioId);
        }
        
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/account/{accountNo}")
    public ResponseEntity<Portfolio> getPortfolioByAccount(
            @PathVariable String accountNo,
            Authentication authentication) {
        Portfolio portfolio = portfolioService.getPortfolioByAccountNo(accountNo);
        
        if (authentication != null) {
            auditService.logInquiry(authentication.getName(), portfolio.getPortfolioId());
        }
        
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/{portfolioId}/positions")
    public ResponseEntity<List<Position>> getPortfolioPositions(
            @PathVariable String portfolioId,
            Authentication authentication) {
        List<Position> positions = portfolioService.getPortfolioPositions(portfolioId);
        
        if (authentication != null) {
            auditService.logInquiry(authentication.getName(), portfolioId);
        }
        
        return ResponseEntity.ok(positions);
    }

    @GetMapping("/{portfolioId}/summary")
    public ResponseEntity<PortfolioService.PortfolioSummary> getPortfolioSummary(
            @PathVariable String portfolioId,
            Authentication authentication) {
        PortfolioService.PortfolioSummary summary = portfolioService.getPortfolioSummary(portfolioId);
        
        if (authentication != null) {
            auditService.logInquiry(authentication.getName(), portfolioId);
        }
        
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(
            @RequestBody Portfolio portfolio,
            Authentication authentication) {
        if (authentication != null) {
            portfolio.setLastUser(authentication.getName());
        }
        
        Portfolio created = portfolioService.createPortfolio(portfolio);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<Portfolio> updatePortfolio(
            @PathVariable String portfolioId,
            @RequestBody Portfolio updates,
            Authentication authentication) {
        if (authentication != null) {
            updates.setLastUser(authentication.getName());
        }
        
        Portfolio updated = portfolioService.updatePortfolio(portfolioId, updates);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{portfolioId}/close")
    public ResponseEntity<Void> closePortfolio(
            @PathVariable String portfolioId,
            Authentication authentication) {
        portfolioService.closePortfolio(portfolioId);
        return ResponseEntity.ok().build();
    }
}
