package com.portfolio.service;

import com.portfolio.dto.PortfolioRequest;
import com.portfolio.dto.PortfolioResponse;
import com.portfolio.exception.DuplicatePortfolioException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.Portfolio;
import com.portfolio.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for portfolio operations.
 * Translated from COBOL programs:
 * <ul>
 *   <li>PORTMSTR.cbl — CRUD dispatcher (0000-MAIN EVALUATE)</li>
 *   <li>PORTADD.cbl — bulk add (2100-VALIDATE-AND-ADD)</li>
 *   <li>PORTUPDT.cbl — field-level update (2200-APPLY-UPDATE)</li>
 *   <li>PORTREAD.cbl — sequential read (2000-PROCESS)</li>
 *   <li>PORTDEL.cbl — delete with audit (2200-DELETE-RECORD)</li>
 * </ul>
 */
@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AuditService auditService;

    public PortfolioService(PortfolioRepository portfolioRepository, AuditService auditService) {
        this.portfolioRepository = portfolioRepository;
        this.auditService = auditService;
    }

    /**
     * Create a new portfolio record.
     * Mirrors PORTMSTR.cbl paragraph 2000-CREATE-PORTFOLIO:
     * <pre>
     *   MOVE LS-PORTFOLIO TO PORTFOLIO-RECORD
     *   PERFORM 2100-VALIDATE-PORTFOLIO
     *   WRITE PORTFOLIO-RECORD
     * </pre>
     * Also mirrors PORTADD.cbl paragraph 2100-VALIDATE-AND-ADD validation checks.
     */
    @Transactional
    public PortfolioResponse createPortfolio(PortfolioRequest request) {
        if (portfolioRepository.existsByPortId(request.getPortId())) {
            throw new DuplicatePortfolioException(request.getPortId());
        }

        Portfolio portfolio = new Portfolio();
        portfolio.setPortId(request.getPortId());
        portfolio.setAccountNo(request.getAccountNo());
        portfolio.setClientName(request.getClientName());
        portfolio.setClientType(request.getClientType());
        portfolio.setCreateDate(LocalDate.now());
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setStatus(request.getStatus());
        portfolio.setTotalValue(
                request.getTotalValue() != null ? request.getTotalValue() : BigDecimal.ZERO);
        portfolio.setCashBalance(
                request.getCashBalance() != null ? request.getCashBalance() : BigDecimal.ZERO);
        portfolio.setLastUser("SYSTEM");

        Portfolio saved = portfolioRepository.save(portfolio);

        auditService.logAction("TRAN", "CREATE", "SUCC",
                saved.getPortId(), saved.getAccountNo(),
                null, saved.toString(),
                "Portfolio created successfully");

        return PortfolioResponse.fromEntity(saved);
    }

    /**
     * Read a portfolio by ID.
     * Mirrors PORTMSTR.cbl paragraph 3000-READ-PORTFOLIO:
     * <pre>
     *   READ PORTFOLIO-FILE
     *   WHEN PORT-SUCCESS  MOVE PORTFOLIO-RECORD TO LS-PORTFOLIO
     *   WHEN PORT-NOT-FOUND  'Portfolio not found'
     * </pre>
     */
    @Transactional(readOnly = true)
    public PortfolioResponse readPortfolio(String portId) {
        Portfolio portfolio = portfolioRepository.findById(portId)
                .orElseThrow(() -> new PortfolioNotFoundException(portId));
        return PortfolioResponse.fromEntity(portfolio);
    }

    /**
     * Update an existing portfolio.
     * Mirrors PORTMSTR.cbl paragraph 4000-UPDATE-PORTFOLIO:
     * <pre>
     *   PERFORM 2100-VALIDATE-PORTFOLIO
     *   REWRITE PORTFOLIO-RECORD
     *   PERFORM 2100-LOG-PORTFOLIO-UPDATE
     * </pre>
     * Also mirrors PORTUPDT.cbl paragraph 2200-APPLY-UPDATE field-level updates.
     */
    @Transactional
    public PortfolioResponse updatePortfolio(String portId, PortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portId)
                .orElseThrow(() -> new PortfolioNotFoundException(portId));

        String beforeImage = portfolio.toString();

        if (request.getAccountNo() != null) {
            portfolio.setAccountNo(request.getAccountNo());
        }
        if (request.getClientName() != null) {
            portfolio.setClientName(request.getClientName());
        }
        if (request.getClientType() != null) {
            portfolio.setClientType(request.getClientType());
        }
        if (request.getStatus() != null) {
            portfolio.setStatus(request.getStatus());
        }
        if (request.getTotalValue() != null) {
            portfolio.setTotalValue(request.getTotalValue());
        }
        if (request.getCashBalance() != null) {
            portfolio.setCashBalance(request.getCashBalance());
        }
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setLastUser("SYSTEM");

        Portfolio saved = portfolioRepository.save(portfolio);

        auditService.logAction("TRAN", "UPDATE", "SUCC",
                saved.getPortId(), saved.getAccountNo(),
                beforeImage, saved.toString(),
                "Portfolio updated successfully");

        return PortfolioResponse.fromEntity(saved);
    }

    /**
     * Delete a portfolio by ID.
     * Mirrors PORTMSTR.cbl paragraph 5000-DELETE-PORTFOLIO:
     * <pre>
     *   DELETE PORTFOLIO-FILE
     *   WHEN PORT-NOT-FOUND  'Portfolio not found for deletion'
     * </pre>
     * Also mirrors PORTDEL.cbl paragraph 2200-DELETE-RECORD with audit trail.
     */
    @Transactional
    public void deletePortfolio(String portId) {
        Portfolio portfolio = portfolioRepository.findById(portId)
                .orElseThrow(() -> new PortfolioNotFoundException(portId));

        String beforeImage = portfolio.toString();
        portfolioRepository.delete(portfolio);

        auditService.logAction("TRAN", "DELETE", "SUCC",
                portId, portfolio.getAccountNo(),
                beforeImage, null,
                "Portfolio deleted successfully");
    }

    /**
     * List all portfolios (sequential read).
     * Mirrors PORTREAD.cbl paragraph 2000-PROCESS:
     * <pre>
     *   READ PORTFOLIO-FILE NEXT RECORD
     *       AT END SET END-OF-FILE TO TRUE
     *       NOT AT END PERFORM 2100-DISPLAY-RECORD
     * </pre>
     */
    @Transactional(readOnly = true)
    public List<PortfolioResponse> findAllPortfolios() {
        return portfolioRepository.findAll().stream()
                .map(PortfolioResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Find portfolios by status.
     * Mirrors the 88-level conditions on PORT-STATUS: A=Active, C=Closed, S=Suspended.
     */
    @Transactional(readOnly = true)
    public List<PortfolioResponse> findByStatus(String status) {
        return portfolioRepository.findByStatus(status).stream()
                .map(PortfolioResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
