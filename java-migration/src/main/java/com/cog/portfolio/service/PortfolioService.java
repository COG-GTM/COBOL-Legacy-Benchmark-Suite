package com.cog.portfolio.service;

import com.cog.portfolio.common.PortfolioException;
import com.cog.portfolio.common.ScaleConverter;
import com.cog.portfolio.domain.AuditAction;
import com.cog.portfolio.domain.Portfolio;
import com.cog.portfolio.domain.PortfolioId;
import com.cog.portfolio.dto.PortfolioRequest;
import com.cog.portfolio.repository.PortfolioRepository;
import com.cog.portfolio.validation.PortfolioValidator;
import com.cog.portfolio.validation.ValidationResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PORTMSTR.cbl (CRUD dispatcher) plus the batch wrappers PORTADD.cbl,
 * PORTDEL.cbl and PORTREAD.cbl, all over the PORTFOLIO table.
 */
@Service
public class PortfolioService {

    public static final String PROGRAM = "PORTMSTR";
    public static final String MSG_DUPLICATE = "Portfolio ID already exists";
    public static final String MSG_NOT_FOUND = "Portfolio not found";
    public static final String MSG_NOT_FOUND_UPDATE = "Portfolio not found for update";
    public static final String MSG_NOT_FOUND_DELETE = "Portfolio not found for deletion";
    public static final String MSG_NAME_REQUIRED = "Portfolio Name is required";
    public static final String MSG_INVALID_STATUS = "Invalid Portfolio Status";

    private final PortfolioRepository repository;
    private final PortfolioValidator validator;
    private final AuditService auditService;

    public PortfolioService(PortfolioRepository repository, PortfolioValidator validator, AuditService auditService) {
        this.repository = repository;
        this.validator = validator;
        this.auditService = auditService;
    }

    /** PORTMSTR 2000-CREATE-PORTFOLIO / PORTADD. */
    @Transactional
    public Portfolio create(PortfolioRequest request) {
        validate(request);
        PortfolioId id = new PortfolioId(request.portfolioId(), request.accountNo());
        if (repository.existsById(id)) {
            throw PortfolioException.duplicate(MSG_DUPLICATE);
        }
        Portfolio portfolio = new Portfolio(id, request.clientName(), request.clientType(), request.status());
        portfolio.setCreateDate(LocalDate.now());
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setTotalValue(money(request.totalValue()));
        portfolio.setCashBalance(money(request.cashBalance()));
        portfolio.setLastUser(PROGRAM);
        Portfolio saved = repository.save(portfolio);
        auditService.recordSuccess(PROGRAM, AuditAction.CREATE, id.getPortfolioId(), id.getAccountNo(),
                null, image(saved), "Portfolio created successfully");
        return saved;
    }

    /** PORTMSTR 3000-READ-PORTFOLIO. */
    @Transactional(readOnly = true)
    public Portfolio read(String portfolioId, String accountNo) {
        return repository.findById(new PortfolioId(portfolioId, accountNo))
                .orElseThrow(() -> PortfolioException.notFound(MSG_NOT_FOUND));
    }

    /** PORTREAD: sequential read of the whole master file. */
    @Transactional(readOnly = true)
    public List<Portfolio> readAll() {
        return repository.findAllByOrderByIdPortfolioIdAsc();
    }

    /** PORTMSTR 4000-UPDATE-PORTFOLIO (full record rewrite + audit). */
    @Transactional
    public Portfolio update(PortfolioRequest request) {
        validate(request);
        PortfolioId id = new PortfolioId(request.portfolioId(), request.accountNo());
        Portfolio portfolio = repository.findById(id)
                .orElseThrow(() -> PortfolioException.notFound(MSG_NOT_FOUND_UPDATE));
        String before = image(portfolio);
        portfolio.setClientName(request.clientName());
        portfolio.setClientType(request.clientType());
        portfolio.setStatus(request.status());
        if (request.totalValue() != null) {
            portfolio.setTotalValue(money(request.totalValue()));
        }
        if (request.cashBalance() != null) {
            portfolio.setCashBalance(money(request.cashBalance()));
        }
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setLastUser(PROGRAM);
        Portfolio saved = repository.save(portfolio);
        auditService.recordSuccess(PROGRAM, AuditAction.UPDATE, id.getPortfolioId(), id.getAccountNo(),
                before, image(saved), "Portfolio updated successfully");
        return saved;
    }

    /** PORTMSTR 5000-DELETE-PORTFOLIO / PORTDEL. */
    @Transactional
    public void delete(String portfolioId, String accountNo) {
        PortfolioId id = new PortfolioId(portfolioId, accountNo);
        Portfolio portfolio = repository.findById(id)
                .orElseThrow(() -> PortfolioException.notFound(MSG_NOT_FOUND_DELETE));
        String before = image(portfolio);
        repository.delete(portfolio);
        auditService.recordSuccess(PROGRAM, AuditAction.DELETE, portfolioId, accountNo,
                before, null, "Portfolio deleted successfully");
    }

    /** PORTMSTR 2100-VALIDATE-PORTFOLIO + PORTADD 2100-VALIDATE-RECORD. */
    void validate(PortfolioRequest request) {
        ValidationResult idResult = validator.validatePortfolioId(request.portfolioId());
        if (!idResult.isValid()) {
            throw PortfolioException.validation(idResult.message());
        }
        ValidationResult acctResult = validator.validateAccountNo(request.accountNo());
        if (!acctResult.isValid()) {
            throw PortfolioException.validation(acctResult.message());
        }
        if (request.clientName() == null || request.clientName().isBlank()) {
            throw PortfolioException.validation(MSG_NAME_REQUIRED);
        }
        if (request.status() == null) {
            throw PortfolioException.validation(MSG_INVALID_STATUS);
        }
        if (request.totalValue() != null && !validator.validateAmount(request.totalValue()).isValid()) {
            throw PortfolioException.validation(ValidationResult.ERR_AMOUNT);
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return ScaleConverter.toMoneyScale(value == null ? BigDecimal.ZERO : value);
    }

    static String image(Portfolio p) {
        return p.getId() + " " + p.getClientName() + " " + p.getClientType().code() + " " + p.getStatus().code()
                + " tv=" + p.getTotalValue() + " cb=" + p.getCashBalance();
    }
}
