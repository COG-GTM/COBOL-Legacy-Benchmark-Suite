package com.portfolio.controller;

import com.portfolio.dto.PositionResponse;
import com.portfolio.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PositionController {

    private final PositionService positionService;

    @GetMapping("/account/{accountNo}")
    public ResponseEntity<PositionResponse> getPositionByAccount(
            @PathVariable String accountNo,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId) {
        log.info("Fetching position for account: {}", accountNo);
        PositionResponse position = positionService.getPositionByAccountNo(accountNo, userId);
        return ResponseEntity.ok(position);
    }

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<PositionResponse>> getPositionsByPortfolio(
            @PathVariable String portfolioId,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId) {
        log.info("Fetching positions for portfolio: {}", portfolioId);
        List<PositionResponse> positions = positionService.getPositionsByPortfolioId(portfolioId, userId);
        return ResponseEntity.ok(positions);
    }

    @GetMapping("/portfolio/{portfolioId}/investment/{investmentId}")
    public ResponseEntity<PositionResponse> getPositionByPortfolioAndInvestment(
            @PathVariable String portfolioId,
            @PathVariable String investmentId,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId) {
        log.info("Fetching position for portfolio: {}, investment: {}", portfolioId, investmentId);
        return positionService.getPositionByPortfolioAndInvestment(portfolioId, investmentId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/portfolio/{portfolioId}/active")
    public ResponseEntity<List<PositionResponse>> getActivePositions(
            @PathVariable String portfolioId) {
        log.info("Fetching active positions for portfolio: {}", portfolioId);
        List<PositionResponse> positions = positionService.getActivePositions(portfolioId);
        return ResponseEntity.ok(positions);
    }

    @GetMapping
    public ResponseEntity<List<PositionResponse>> getAllPositions() {
        log.info("Fetching all positions");
        List<PositionResponse> positions = positionService.getAllPositions();
        return ResponseEntity.ok(positions);
    }
}
