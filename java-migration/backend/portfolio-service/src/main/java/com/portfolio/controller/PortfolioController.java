package com.portfolio.controller;

import com.portfolio.dto.PortfolioResponse;
import com.portfolio.dto.PortfolioUpdateRequest;
import com.portfolio.model.enums.ClientType;
import com.portfolio.model.enums.PortfolioStatus;
import com.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(@PathVariable String portfolioId) {
        log.info("Fetching portfolio: {}", portfolioId);
        PortfolioResponse portfolio = portfolioService.getPortfolioById(portfolioId);
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/account/{accountNo}")
    public ResponseEntity<PortfolioResponse> getPortfolioByAccountNo(@PathVariable String accountNo) {
        log.info("Fetching portfolio by account: {}", accountNo);
        PortfolioResponse portfolio = portfolioService.getPortfolioByAccountNo(accountNo);
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios() {
        log.info("Fetching all portfolios");
        List<PortfolioResponse> portfolios = portfolioService.getAllPortfolios();
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PortfolioResponse>> getPortfoliosByStatus(@PathVariable PortfolioStatus status) {
        log.info("Fetching portfolios with status: {}", status);
        List<PortfolioResponse> portfolios = portfolioService.getPortfoliosByStatus(status);
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PortfolioResponse>> searchPortfolios(@RequestParam String clientName) {
        log.info("Searching portfolios by client name: {}", clientName);
        List<PortfolioResponse> portfolios = portfolioService.searchPortfoliosByName(clientName);
        return ResponseEntity.ok(portfolios);
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(@RequestBody Map<String, String> request) {
        log.info("Creating new portfolio: {}", request.get("portfolioId"));
        PortfolioResponse portfolio = portfolioService.createPortfolio(
                request.get("portfolioId"),
                request.get("accountNo"),
                request.get("clientName"),
                request.get("clientType") != null ? ClientType.valueOf(request.get("clientType")) : ClientType.INDIVIDUAL,
                request.get("userId")
        );
        return ResponseEntity.ok(portfolio);
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable String portfolioId,
            @Valid @RequestBody PortfolioUpdateRequest request) {
        log.info("Updating portfolio: {}", portfolioId);
        PortfolioResponse portfolio = portfolioService.updatePortfolio(portfolioId, request);
        return ResponseEntity.ok(portfolio);
    }

    @PutMapping("/{portfolioId}/status")
    public ResponseEntity<PortfolioResponse> updatePortfolioStatus(
            @PathVariable String portfolioId,
            @RequestBody Map<String, String> request) {
        log.info("Updating status for portfolio: {}", portfolioId);
        PortfolioStatus status = PortfolioStatus.valueOf(request.get("status"));
        String userId = request.get("userId");
        PortfolioResponse portfolio = portfolioService.updatePortfolioStatus(portfolioId, status, userId);
        return ResponseEntity.ok(portfolio);
    }

    @PutMapping("/{portfolioId}/name")
    public ResponseEntity<PortfolioResponse> updatePortfolioName(
            @PathVariable String portfolioId,
            @RequestBody Map<String, String> request) {
        log.info("Updating client name for portfolio: {}", portfolioId);
        String clientName = request.get("clientName");
        String userId = request.get("userId");
        PortfolioResponse portfolio = portfolioService.updatePortfolioName(portfolioId, clientName, userId);
        return ResponseEntity.ok(portfolio);
    }

    @PutMapping("/{portfolioId}/value")
    public ResponseEntity<PortfolioResponse> updatePortfolioValue(
            @PathVariable String portfolioId,
            @RequestBody Map<String, String> request) {
        log.info("Updating total value for portfolio: {}", portfolioId);
        BigDecimal totalValue = new BigDecimal(request.get("totalValue"));
        String userId = request.get("userId");
        PortfolioResponse portfolio = portfolioService.updatePortfolioValue(portfolioId, totalValue, userId);
        return ResponseEntity.ok(portfolio);
    }
}
