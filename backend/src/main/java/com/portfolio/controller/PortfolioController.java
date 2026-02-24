package com.portfolio.controller;

import com.portfolio.dto.PortfolioResponse;
import com.portfolio.dto.TransactionResponse;
import com.portfolio.service.AuditService;
import com.portfolio.service.PortfolioService;
import com.portfolio.service.TransactionHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

/**
 * Portfolio REST controller - replaces INQONLN routing + INQPORT + INQHIST.
 * Source: src/programs/online/INQONLN.cbl, INQPORT.cbl, INQHIST.cbl
 *
 * INQONLN routing:
 *   'INQP' → P300-PORTFOLIO-INQUIRY → GET /api/portfolios/{id}
 *   'INQH' → P400-HISTORY-INQUIRY  → GET /api/portfolios/{id}/history
 *
 * INQPORT:
 *   P200-GET-POSITION (VSAM READ) → getPortfolioById (JPA query)
 *
 * INQHIST:
 *   P200-GET-HISTORY (cursor fetch) → getHistory (paginated JPA query)
 */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final TransactionHistoryService historyService;
    private final AuditService auditService;

    public PortfolioController(PortfolioService portfolioService,
                               TransactionHistoryService historyService,
                               AuditService auditService) {
        this.portfolioService = portfolioService;
        this.historyService = historyService;
        this.auditService = auditService;
    }

    /**
     * Get all portfolios.
     */
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios() {
        return ResponseEntity.ok(portfolioService.getAllPortfolios());
    }

    /**
     * Get portfolio by ID with positions - replaces INQPORT P200-GET-POSITION.
     * Maps: CICS READ FILE('POSFILE') → JPA findById + findActivePositions
     */
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(
            @PathVariable("id") String portfolioId,
            Authentication authentication,
            HttpServletRequest request) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        auditService.logPortfolioAccess(username, portfolioId, request.getRemoteAddr());
        return ResponseEntity.ok(portfolioService.getPortfolioById(portfolioId));
    }

    /**
     * Get transaction history for portfolio with pagination.
     * Replaces INQHIST cursor-based array fetch (HISTORY_CURSOR, 3000 bytes).
     * Now uses standard Spring Data Pageable.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<Page<TransactionResponse>> getHistory(
            @PathVariable("id") String portfolioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Pageable pageable = PageRequest.of(page, size);

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(historyService.getHistoryByDateRange(portfolioId, startDate, endDate, pageable));
        }
        return ResponseEntity.ok(historyService.getHistoryByPortfolioId(portfolioId, pageable));
    }

    /**
     * Get active portfolios only.
     */
    @GetMapping("/active")
    public ResponseEntity<List<PortfolioResponse>> getActivePortfolios() {
        return ResponseEntity.ok(portfolioService.getActivePortfolios());
    }
}
