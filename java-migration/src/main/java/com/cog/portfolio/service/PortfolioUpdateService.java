package com.cog.portfolio.service;

import com.cog.portfolio.common.PortfolioException;
import com.cog.portfolio.common.ScaleConverter;
import com.cog.portfolio.domain.AuditAction;
import com.cog.portfolio.domain.Portfolio;
import com.cog.portfolio.domain.PortfolioId;
import com.cog.portfolio.domain.PortfolioStatus;
import com.cog.portfolio.dto.PortfolioUpdateRequest;
import com.cog.portfolio.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * PORTUPDT.cbl: applies a file of field-level updates (S=status, N=name, V=total value)
 * to portfolio records, counting successes and errors instead of aborting.
 */
@Service
public class PortfolioUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioUpdateService.class);
    public static final String PROGRAM = "PORTUPDT";

    public record UpdateSummary(int updated, int errors) {
    }

    private final PortfolioRepository repository;
    private final AuditService auditService;

    public PortfolioUpdateService(PortfolioRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /** 2000-PROCESS loop: one record failing does not stop the run. */
    public UpdateSummary applyAll(List<PortfolioUpdateRequest> updates) {
        int updated = 0;
        int errors = 0;
        for (PortfolioUpdateRequest update : updates) {
            try {
                apply(update);
                updated++;
            } catch (PortfolioException e) {
                errors++;
                log.warn("Update failed for {}/{}: {}", update.portfolioId(), update.accountNo(), e.getMessage());
            }
        }
        log.info("Updates processed: {} Errors occurred: {}", updated, errors);
        return new UpdateSummary(updated, errors);
    }

    /** 2100-PROCESS-UPDATE + 2200-APPLY-UPDATE. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Portfolio apply(PortfolioUpdateRequest update) {
        Portfolio portfolio = repository.findById(new PortfolioId(update.portfolioId(), update.accountNo()))
                .orElseThrow(() -> PortfolioException.notFound("Record not found: " + update.portfolioId()));
        String before = PortfolioService.image(portfolio);
        switch (update.updateType()) {
            case STATUS -> portfolio.setStatus(parseStatus(update.newValue()));
            case NAME -> portfolio.setClientName(update.newValue().trim());
            case VALUE -> portfolio.setTotalValue(parseValue(update.newValue()));
        }
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setLastUser(PROGRAM);
        Portfolio saved = repository.save(portfolio);
        auditService.recordSuccess(PROGRAM, AuditAction.UPDATE, update.portfolioId(), update.accountNo(),
                before, PortfolioService.image(saved), "Field " + update.updateType().code() + " updated");
        return saved;
    }

    private static PortfolioStatus parseStatus(String value) {
        try {
            return PortfolioStatus.fromCode(value);
        } catch (IllegalArgumentException e) {
            throw PortfolioException.validation("Invalid Portfolio Status: " + value);
        }
    }

    /** MOVE UPDT-NEW-VALUE TO WS-NUMERIC-WORK (PIC S9(13)V99). */
    private static BigDecimal parseValue(String value) {
        try {
            return ScaleConverter.toMoneyScale(new BigDecimal(value.trim()));
        } catch (NumberFormatException e) {
            throw PortfolioException.validation("Invalid numeric value: " + value);
        }
    }
}
