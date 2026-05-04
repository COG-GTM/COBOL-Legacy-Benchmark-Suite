package com.portfolio.service;

import com.portfolio.audit.AuditService;
import com.portfolio.dto.PortfolioCreateRequest;
import com.portfolio.dto.PortfolioUpdateRequest;
import com.portfolio.entity.AuditAction;
import com.portfolio.entity.AuditStatus;
import com.portfolio.entity.AuditType;
import com.portfolio.entity.ClientType;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.entity.PortfolioStatus;
import com.portfolio.exception.DuplicatePortfolioException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.repository.PortfolioMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioMasterRepository portfolioRepository;
    private final PortfolioValidator validator;
    private final AuditService auditService;

    public PortfolioService(PortfolioMasterRepository portfolioRepository,
                            PortfolioValidator validator,
                            AuditService auditService) {
        this.portfolioRepository = portfolioRepository;
        this.validator = validator;
        this.auditService = auditService;
    }

    @Transactional
    public PortfolioMaster createPortfolio(PortfolioCreateRequest request) {
        validator.validatePortfolioId(request.getPortfolioId());
        validator.validateName(request.getPortfolioName());
        validator.validateStatus(request.getStatus());

        if (portfolioRepository.existsById(request.getPortfolioId())) {
            throw new DuplicatePortfolioException(request.getPortfolioId());
        }

        PortfolioMaster portfolio = new PortfolioMaster();
        portfolio.setPortfolioId(request.getPortfolioId());
        portfolio.setAccountNo(request.getAccountNo());
        portfolio.setClientName(request.getClientName());
        if (request.getClientType() != null && !request.getClientType().isEmpty()) {
            portfolio.setClientType(ClientType.fromCode(request.getClientType().charAt(0)));
        }
        portfolio.setPortfolioName(request.getPortfolioName());
        portfolio.setStatus(PortfolioStatus.fromCode(request.getStatus().charAt(0)));
        portfolio.setCreateDate(LocalDate.now());
        portfolio.setOpenDate(LocalDate.now());
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolio.setLastUser("SYSTEM");
        portfolio.setTotalValue(BigDecimal.ZERO);
        portfolio.setCashBalance(BigDecimal.ZERO);
        portfolio.setAccountType(request.getAccountType());
        portfolio.setBranchId(request.getBranchId());
        portfolio.setClientId(request.getClientId());
        portfolio.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "USD");
        portfolio.setRiskLevel(request.getRiskLevel());

        PortfolioMaster saved = portfolioRepository.save(portfolio);

        auditService.logAudit("BATCH", "SYSTEM", "PORTMSTR", null,
                AuditType.TRANSACTION, AuditAction.CREATE, AuditStatus.SUCCESS,
                saved.getPortfolioId(), saved.getAccountNo(),
                null, saved.getPortfolioId() + ":" + saved.getPortfolioName(),
                "Portfolio created");

        return saved;
    }

    @Transactional(readOnly = true)
    public PortfolioMaster readPortfolio(String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }

    @Transactional
    public PortfolioMaster updatePortfolio(String portfolioId, PortfolioUpdateRequest request) {
        PortfolioMaster existing = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        String beforeImage = existing.getPortfolioId() + ":" + existing.getPortfolioName()
                + ":" + existing.getStatus();

        if (request.getClientName() != null) {
            existing.setClientName(request.getClientName());
        }
        if (request.getClientType() != null && !request.getClientType().isEmpty()) {
            existing.setClientType(ClientType.fromCode(request.getClientType().charAt(0)));
        }
        if (request.getPortfolioName() != null) {
            validator.validateName(request.getPortfolioName());
            existing.setPortfolioName(request.getPortfolioName());
        }
        if (request.getStatus() != null) {
            validator.validateStatus(request.getStatus());
            existing.setStatus(PortfolioStatus.fromCode(request.getStatus().charAt(0)));
        }
        if (request.getCurrencyCode() != null) {
            existing.setCurrencyCode(request.getCurrencyCode());
        }
        if (request.getRiskLevel() != null) {
            existing.setRiskLevel(request.getRiskLevel());
        }

        existing.setLastMaintDate(LocalDateTime.now());
        existing.setLastUser("SYSTEM");

        PortfolioMaster updated = portfolioRepository.save(existing);

        String afterImage = updated.getPortfolioId() + ":" + updated.getPortfolioName()
                + ":" + updated.getStatus();

        auditService.logAudit("BATCH", "SYSTEM", "PORTMSTR", null,
                AuditType.TRANSACTION, AuditAction.UPDATE, AuditStatus.SUCCESS,
                updated.getPortfolioId(), updated.getAccountNo(),
                beforeImage, afterImage, "Portfolio updated");

        return updated;
    }

    @Transactional
    public void deletePortfolio(String portfolioId) {
        PortfolioMaster existing = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        portfolioRepository.delete(existing);

        auditService.logAudit("BATCH", "SYSTEM", "PORTMSTR", null,
                AuditType.TRANSACTION, AuditAction.DELETE, AuditStatus.SUCCESS,
                portfolioId, existing.getAccountNo(),
                existing.getPortfolioId() + ":" + existing.getPortfolioName(),
                null, "Portfolio deleted");
    }

    @Transactional(readOnly = true)
    public List<PortfolioMaster> getActivePortfolios() {
        return portfolioRepository.findActivePortfolios(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<PortfolioMaster> findAll() {
        return portfolioRepository.findAll();
    }
}
