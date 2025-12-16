package com.portfolio.controller;

import com.portfolio.entity.Position;
import com.portfolio.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Position operations.
 * Replaces VSAM POSFILE access via CICS file control.
 * 
 * @see src/cics/PORTDFN.csd - POSFILE definition
 */
@RestController
@RequestMapping("/v1/positions")
@RequiredArgsConstructor
@Tag(name = "Position", description = "Portfolio position management operations")
public class PositionController {

    private final PositionService positionService;

    @GetMapping("/{id}")
    @Operation(summary = "Get position by ID", description = "Retrieve a position by its UUID")
    public ResponseEntity<Position> findById(@PathVariable UUID id) {
        return positionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/portfolio/{portfolioId}")
    @Operation(summary = "Get positions by portfolio", description = "Retrieve all positions for a portfolio")
    public ResponseEntity<Page<Position>> findByPortfolioId(
            @PathVariable String portfolioId,
            Pageable pageable) {
        return ResponseEntity.ok(positionService.findByPortfolioId(portfolioId, pageable));
    }

    @GetMapping("/portfolio/{portfolioId}/current")
    @Operation(summary = "Get current positions", description = "Retrieve current active positions for a portfolio")
    public ResponseEntity<List<Position>> findCurrentPositions(@PathVariable String portfolioId) {
        return ResponseEntity.ok(positionService.findCurrentPositions(portfolioId));
    }

    @GetMapping("/portfolio/{portfolioId}/latest")
    @Operation(summary = "Get latest positions", description = "Retrieve latest position snapshot for a portfolio")
    public ResponseEntity<List<Position>> findLatestPositions(@PathVariable String portfolioId) {
        return ResponseEntity.ok(positionService.findLatestPositions(portfolioId));
    }

    @GetMapping("/investment/{investmentId}")
    @Operation(summary = "Get positions by investment", description = "Retrieve all positions for an investment")
    public ResponseEntity<List<Position>> findByInvestmentId(@PathVariable String investmentId) {
        return ResponseEntity.ok(positionService.findByInvestmentId(investmentId));
    }

    @GetMapping("/key")
    @Operation(summary = "Get position by key", description = "Retrieve position by composite key")
    public ResponseEntity<Position> findByKey(
            @RequestParam String portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate positionDate,
            @RequestParam String investmentId) {
        return positionService.findByKey(portfolioId, positionDate, investmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create position", description = "Create a new position")
    public ResponseEntity<Position> create(
            @Valid @RequestBody Position position,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        Position created = positionService.create(position, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping
    @Operation(summary = "Update position", description = "Update an existing position")
    public ResponseEntity<Position> update(
            @RequestParam String portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate positionDate,
            @RequestParam String investmentId,
            @Valid @RequestBody Position position,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        Position updated = positionService.update(portfolioId, positionDate, investmentId, position, userId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/close")
    @Operation(summary = "Close position", description = "Close an existing position")
    public ResponseEntity<Void> close(
            @RequestParam String portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate positionDate,
            @RequestParam String investmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        positionService.closePosition(portfolioId, positionDate, investmentId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/portfolio/{portfolioId}/total-value")
    @Operation(summary = "Get total market value", description = "Calculate total market value for a portfolio")
    public ResponseEntity<BigDecimal> calculateTotalMarketValue(@PathVariable String portfolioId) {
        return ResponseEntity.ok(positionService.calculateTotalMarketValue(portfolioId));
    }

    @GetMapping("/portfolio/{portfolioId}/investment-count")
    @Operation(summary = "Count investments", description = "Count distinct investments in a portfolio")
    public ResponseEntity<Long> countDistinctInvestments(@PathVariable String portfolioId) {
        return ResponseEntity.ok(positionService.countDistinctInvestments(portfolioId));
    }
}
