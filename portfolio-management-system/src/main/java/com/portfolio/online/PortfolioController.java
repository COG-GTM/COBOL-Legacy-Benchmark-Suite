package com.portfolio.online;

import com.portfolio.model.PositionRecord;
import com.portfolio.model.SecurityLogRecord;
import com.portfolio.model.TransactionRecord;
import com.portfolio.support.Db2RecoveryService;
import com.portfolio.support.Db2StatisticsService;
import com.portfolio.support.PositionRecordRepository;
import com.portfolio.support.SecurityLogRepository;
import com.portfolio.support.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Portfolio REST Controller.
 * Migrated from COBOL CICS online layer:
 *   INQONLN (main CICS controller) -> PortfolioController base at /api/portfolio/**
 *   INQPORT (VSAM direct read) -> GET /api/portfolio/{id}/positions
 *   INQHIST (DB2 cursor-based) -> GET /api/portfolio/{id}/history?page=&size=
 *   CURSMGR (cursor/pagination) -> Spring Data Page<T>
 *   BMS Maps (MENMAP, POSMAP, HISMAP, ERRMAP) -> JSON responses
 *
 * CICS transaction PINQ (PORTDFN.csd lines 6-11) is replaced by REST endpoints.
 * DB2RECV recovery (3 retries, 2s) applied via @Retryable from Phase 1.
 * SECMGR security applied via Spring Security filter chain from Phase 1.
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    private final PositionRecordRepository positionRepository;
    private final TransactionRecordRepository transactionRepository;
    private final SecurityLogRepository securityLogRepository;
    private final Db2RecoveryService recoveryService;
    private final Db2StatisticsService statisticsService;

    public PortfolioController(
            PositionRecordRepository positionRepository,
            TransactionRecordRepository transactionRepository,
            SecurityLogRepository securityLogRepository,
            Db2RecoveryService recoveryService,
            Db2StatisticsService statisticsService) {
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.securityLogRepository = securityLogRepository;
        this.recoveryService = recoveryService;
        this.statisticsService = statisticsService;
    }

    /**
     * Get positions for a portfolio.
     * Replaces INQPORT (VSAM direct read from POSFILE).
     * Reads from POSITION_MASTER DB2 table.
     */
    @GetMapping("/{id}/positions")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getPositions(@PathVariable("id") String portfolioId) {
        log.info("Portfolio inquiry: positions for {}", portfolioId);

        auditAccess(portfolioId, "READ", "positions");

        List<PositionRecord> positions = recoveryService.executeWithRetry(
                () -> positionRepository.findByPortfolioId(portfolioId));

        statisticsService.recordQuery();

        if (positions.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "portfolioId", portfolioId,
                    "positions", List.of(),
                    "message", "No positions found for portfolio " + portfolioId
            ));
        }

        return ResponseEntity.ok(Map.of(
                "portfolioId", portfolioId,
                "positions", positions,
                "count", positions.size()
        ));
    }

    /**
     * Get transaction history for a portfolio with pagination.
     * Replaces INQHIST (DB2 cursor-based) + CURSMGR (array fetch/pagination).
     * Spring Data Pageable replaces CURSMGR cursor management.
     */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getHistory(
            @PathVariable("id") String portfolioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Portfolio inquiry: history for {} (page={}, size={})", portfolioId, page, size);

        auditAccess(portfolioId, "READ", "history");

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionRecord> history = recoveryService.executeWithRetry(
                () -> transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(
                        portfolioId, pageable));

        statisticsService.recordQuery();

        return ResponseEntity.ok(Map.of(
                "portfolioId", portfolioId,
                "transactions", history.getContent(),
                "page", history.getNumber(),
                "size", history.getSize(),
                "totalElements", history.getTotalElements(),
                "totalPages", history.getTotalPages()
        ));
    }

    /**
     * Log audit access.
     * Replaces SECMGR P300-LOG-ACCESS.
     */
    private void auditAccess(String resourceName, String accessType, String details) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth != null ? auth.getName() : "UNKNOWN";

            SecurityLogRecord auditLog = new SecurityLogRecord();
            auditLog.setAuditTimestamp(LocalDateTime.now());
            auditLog.setUserId(userId);
            auditLog.setProgram("INQONLN");
            auditLog.setAccessType(accessType);
            auditLog.setResourceName(resourceName);
            auditLog.setDetails(details);
            auditLog.setResponseCode(0);

            securityLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }
}
