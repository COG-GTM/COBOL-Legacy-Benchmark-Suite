package com.portfolio.portmstr.service;

import com.portfolio.portmstr.dto.PortfolioRequest;
import com.portfolio.portmstr.dto.PortfolioResponse;
import com.portfolio.portmstr.exception.DuplicatePortfolioException;
import com.portfolio.portmstr.exception.PortfolioNotFoundException;
import com.portfolio.portmstr.model.PortfolioMaster;
import com.portfolio.portmstr.model.enums.ClientType;
import com.portfolio.portmstr.model.enums.PortfolioStatus;
import com.portfolio.portmstr.repository.PortfolioMasterRepository;
import com.portfolio.portmstr.validation.PortfolioValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Portfolio Master service.
 * Direct translation of COBOL PORTMSTR.cbl PROCEDURE DIVISION.
 *
 * COBOL paragraph mapping:
 *   0000-MAIN           -> dispatch methods via controller
 *   1000-INITIALIZE     -> handled by Spring DI and constructor
 *   2000-CREATE-PORTFOLIO -> createPortfolio()
 *   2100-VALIDATE-PORTFOLIO -> delegated to PortfolioValidator
 *   3000-READ-PORTFOLIO   -> readPortfolio()
 *   4000-UPDATE-PORTFOLIO -> updatePortfolio()
 *   5000-DELETE-PORTFOLIO -> deletePortfolio()
 *   6000-TERMINATE        -> handled by Spring transaction management
 *   9000-ERROR            -> handled by GlobalExceptionHandler
 *   2100-HANDLE-VSAM-ERROR -> mapped to exception types
 *   2100-LOG-PORTFOLIO-UPDATE -> delegated to AuditService
 */
@Service
public class PortfolioMasterService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioMasterService.class);
    private static final int RC_SUCCESS = 0;

    private final PortfolioMasterRepository portfolioRepository;
    private final PortfolioValidator validator;
    private final AuditService auditService;
    private final ErrorLoggingService errorLoggingService;

    public PortfolioMasterService(PortfolioMasterRepository portfolioRepository,
                                  PortfolioValidator validator,
                                  AuditService auditService,
                                  ErrorLoggingService errorLoggingService) {
        this.portfolioRepository = portfolioRepository;
        this.validator = validator;
        this.auditService = auditService;
        this.errorLoggingService = errorLoggingService;
    }

    /**
     * Create a new portfolio record.
     * Translates COBOL 2000-CREATE-PORTFOLIO paragraph.
     *
     * COBOL flow:
     * 1. MOVE LS-PORTFOLIO TO PORTFOLIO-RECORD
     * 2. PERFORM 2100-VALIDATE-PORTFOLIO
     * 3. WRITE PORTFOLIO-RECORD
     * 4. Check for PORT-DUP-KEY (file status 22)
     */
    @Transactional
    public PortfolioResponse createPortfolio(PortfolioRequest request) {
        validator.validatePortfolioRequest(request);

        if (portfolioRepository.existsByPortfolioId(request.portfolioId())) {
            throw new DuplicatePortfolioException(request.portfolioId());
        }

        PortfolioMaster portfolio = mapRequestToEntity(request);
        portfolio.setCreateDate(LocalDate.now());
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setLastMaintTimestamp(LocalDateTime.now());

        portfolioRepository.save(portfolio);

        auditService.logPortfolioCreate(portfolio, "SYSTEM");

        log.info("Portfolio created: {}", portfolio.getPortfolioId());
        return mapEntityToResponse(portfolio, RC_SUCCESS, null);
    }

    /**
     * Read a portfolio record.
     * Translates COBOL 3000-READ-PORTFOLIO paragraph.
     *
     * COBOL flow:
     * 1. MOVE LS-PORTFOLIO TO PORTFOLIO-RECORD (set key)
     * 2. READ PORTFOLIO-FILE
     * 3. EVALUATE file status (00=success, 23=not-found)
     */
    @Transactional(readOnly = true)
    public PortfolioResponse readPortfolio(String portfolioId) {
        PortfolioMaster portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        return mapEntityToResponse(portfolio, RC_SUCCESS, null);
    }

    /**
     * Update a portfolio record.
     * Translates COBOL 4000-UPDATE-PORTFOLIO paragraph.
     *
     * COBOL flow:
     * 1. MOVE LS-PORTFOLIO TO PORTFOLIO-RECORD
     * 2. PERFORM 2100-VALIDATE-PORTFOLIO
     * 3. REWRITE PORTFOLIO-RECORD
     * 4. Check PORT-NOT-FOUND
     * 5. PERFORM 2100-LOG-PORTFOLIO-UPDATE
     */
    @Transactional
    public PortfolioResponse updatePortfolio(String portfolioId, PortfolioRequest request) {
        PortfolioMaster existing = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        validator.validatePortfolioRequest(request);

        // Capture before-image for audit (COBOL WS-BEFORE-IMAGE)
        PortfolioMaster beforeImage = copyPortfolio(existing);

        // Apply updates
        existing.setClientName(request.clientName());
        existing.setClientType(ClientType.fromCode(request.clientType().charAt(0)));
        existing.setStatus(PortfolioStatus.fromCode(request.status().charAt(0)));
        existing.setTotalValue(request.totalValue());
        existing.setCashBalance(request.cashBalance());
        existing.setCurrencyCode(request.currencyCode());
        existing.setAccountNo(request.accountNo());
        existing.setLastMaintDate(LocalDate.now());
        existing.setLastMaintTimestamp(LocalDateTime.now());

        portfolioRepository.save(existing);

        // Audit logging (COBOL 2100-LOG-PORTFOLIO-UPDATE)
        auditService.logPortfolioUpdate(beforeImage, existing, "SYSTEM");

        log.info("Portfolio updated: {}", portfolioId);
        return mapEntityToResponse(existing, RC_SUCCESS, null);
    }

    /**
     * Delete a portfolio record.
     * Translates COBOL 5000-DELETE-PORTFOLIO paragraph.
     *
     * COBOL flow:
     * 1. MOVE LS-PORTFOLIO TO PORTFOLIO-RECORD
     * 2. DELETE PORTFOLIO-FILE
     * 3. Check PORT-NOT-FOUND
     */
    @Transactional
    public PortfolioResponse deletePortfolio(String portfolioId) {
        PortfolioMaster portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        portfolioRepository.delete(portfolio);

        auditService.logPortfolioDelete(portfolio, "SYSTEM", "03");

        log.info("Portfolio deleted: {}", portfolioId);
        return mapEntityToResponse(portfolio, RC_SUCCESS, "Portfolio deleted successfully");
    }

    /**
     * List all portfolios.
     * Translates COBOL PORTREAD.cbl sequential read loop.
     */
    @Transactional(readOnly = true)
    public List<PortfolioResponse> listPortfolios() {
        return portfolioRepository.findAll().stream()
                .map(p -> mapEntityToResponse(p, RC_SUCCESS, null))
                .toList();
    }

    /**
     * List active portfolios.
     * Translates DB2 view ACTIVE_PORTFOLIOS query.
     */
    @Transactional(readOnly = true)
    public List<PortfolioResponse> listActivePortfolios() {
        return portfolioRepository.findActivePortfolios().stream()
                .map(p -> mapEntityToResponse(p, RC_SUCCESS, null))
                .toList();
    }

    private PortfolioMaster mapRequestToEntity(PortfolioRequest request) {
        PortfolioMaster portfolio = new PortfolioMaster();
        portfolio.setPortfolioId(request.portfolioId());
        portfolio.setAccountNo(request.accountNo());
        portfolio.setClientName(request.clientName());
        portfolio.setClientType(ClientType.fromCode(request.clientType().charAt(0)));
        portfolio.setStatus(PortfolioStatus.fromCode(request.status().charAt(0)));
        portfolio.setTotalValue(request.totalValue() != null ? request.totalValue() : BigDecimal.ZERO);
        portfolio.setCashBalance(request.cashBalance() != null ? request.cashBalance() : BigDecimal.ZERO);
        portfolio.setCurrencyCode(request.currencyCode() != null ? request.currencyCode() : "USD");
        return portfolio;
    }

    private PortfolioResponse mapEntityToResponse(PortfolioMaster portfolio, int returnCode, String errorMessage) {
        return new PortfolioResponse(
                portfolio.getPortfolioId(),
                portfolio.getAccountNo(),
                portfolio.getClientName(),
                portfolio.getClientType() != null ? String.valueOf(portfolio.getClientType().getCode()) : null,
                portfolio.getStatus() != null ? String.valueOf(portfolio.getStatus().getCode()) : null,
                portfolio.getTotalValue(),
                portfolio.getCashBalance(),
                portfolio.getCurrencyCode(),
                portfolio.getCreateDate(),
                portfolio.getLastMaintDate(),
                portfolio.getLastMaintTimestamp(),
                portfolio.getLastUser(),
                returnCode,
                errorMessage);
    }

    private PortfolioMaster copyPortfolio(PortfolioMaster source) {
        PortfolioMaster copy = new PortfolioMaster();
        copy.setPortfolioId(source.getPortfolioId());
        copy.setAccountNo(source.getAccountNo());
        copy.setClientName(source.getClientName());
        copy.setClientType(source.getClientType());
        copy.setStatus(source.getStatus());
        copy.setTotalValue(source.getTotalValue());
        copy.setCashBalance(source.getCashBalance());
        copy.setCurrencyCode(source.getCurrencyCode());
        copy.setCreateDate(source.getCreateDate());
        copy.setLastMaintDate(source.getLastMaintDate());
        copy.setLastMaintTimestamp(source.getLastMaintTimestamp());
        return copy;
    }
}
