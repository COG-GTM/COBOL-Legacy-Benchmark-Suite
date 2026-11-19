package com.clbs.portfolio.service;

import com.clbs.portfolio.domain.Portfolio;
import com.clbs.portfolio.domain.PortfolioKey;
import com.clbs.portfolio.exception.DuplicateException;
import com.clbs.portfolio.exception.InvalidRequestException;
import com.clbs.portfolio.exception.NotFoundException;
import com.clbs.portfolio.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static com.clbs.portfolio.service.CobolText.isSpaces;
import static com.clbs.portfolio.service.CobolText.pic;
import static com.clbs.portfolio.service.CobolText.trimRight;

/** Portfolio maintenance: PORTADD (create), PORTREAD (browse), PORTUPDT (update) and PORTDEL (delete). */
@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);
    private static final int CLIENT_NAME_LENGTH = 30;

    private final PortfolioRepository portfolioRepository;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final Clock clock;

    public PortfolioService(PortfolioRepository portfolioRepository, AuditService auditService,
                            CurrentUser currentUser, Clock clock) {
        this.portfolioRepository = portfolioRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /**
     * PORTADD 2100-VALIDATE-AND-ADD: ID and client name are required, new portfolios must be active, create and
     * last-maintenance dates are set to today, and a duplicate key is rejected.
     */
    @Transactional
    public Portfolio create(Portfolio candidate) {
        if (isSpaces(candidate.getPortfolioId())) {
            throw new InvalidRequestException("Portfolio ID is required");
        }
        if (isSpaces(candidate.getClientName())) {
            throw new InvalidRequestException("Client name is required");
        }
        if (!Portfolio.STATUS_ACTIVE.equals(candidate.getStatus())) {
            throw new InvalidRequestException("New portfolios must have status 'A'");
        }
        if (portfolioRepository.existsById(candidate.getKey())) {
            throw new DuplicateException("Duplicate record: " + candidate.getPortfolioId());
        }
        LocalDate today = LocalDate.now(clock);
        candidate.setCreateDate(today);
        candidate.setLastMaintDate(today);
        if (candidate.getLastUser() == null) {
            candidate.setLastUser(pic(currentUser.name(), 8).stripTrailing());
        }
        Portfolio saved = portfolioRepository.save(candidate);
        auditService.record("PORTADD", AuditService.TYPE_USER, "CREATE", AuditService.STATUS_SUCCESS,
                saved.getPortfolioId(), saved.getAccountNo(), null, null, saved.toString(), "Portfolio created");
        log.info("Portfolio {}/{} created", saved.getPortfolioId(), saved.getAccountNo());
        return saved;
    }

    /** PORTREAD: every portfolio in key order. */
    @Transactional(readOnly = true)
    public List<Portfolio> findAll() {
        return portfolioRepository.findAllByOrderByKeyPortfolioIdAscKeyAccountNoAsc();
    }

    @Transactional(readOnly = true)
    public Portfolio get(PortfolioKey key) {
        return portfolioRepository.findById(key)
                .orElseThrow(() -> new NotFoundException("Record not found: " + key.portfolioId() + key.accountNo()));
    }

    /**
     * PORTUPDT 2200-APPLY-UPDATE. {@code S} stores the first character of the new value as the status, {@code N}
     * the first 30 characters as the client name, {@code V} the value as the total value. Any other action code
     * rewrites the record unchanged, as the COBOL EVALUATE has no WHEN OTHER branch.
     */
    @Transactional
    public Portfolio update(PortfolioKey key, char action, String newValue) {
        Portfolio portfolio = get(key);
        String before = portfolio.toString();
        String value = pic(newValue, 50);
        switch (UpdateAction.fromCode(action).orElse(null)) {
            case STATUS -> portfolio.setStatus(value.substring(0, 1));
            case NAME -> portfolio.setClientName(trimRight(value.substring(0, CLIENT_NAME_LENGTH)));
            case VALUE -> portfolio.setTotalValue(parseAmount(value));
            case null -> log.warn("Unknown update action '{}' for {}/{}; record rewritten unchanged", action,
                    key.portfolioId(), key.accountNo());
        }
        Portfolio saved = portfolioRepository.save(portfolio);
        auditService.record("PORTUPDT", AuditService.TYPE_USER, "UPDATE", AuditService.STATUS_SUCCESS,
                key.portfolioId(), key.accountNo(), null, before, saved.toString(), "Portfolio updated successfully");
        return saved;
    }

    /** PORTDEL 2200-DELETE-RECORD and 2300-WRITE-AUDIT. */
    @Transactional
    public Portfolio delete(PortfolioKey key, String reasonCode) {
        Portfolio portfolio = get(key);
        portfolioRepository.delete(portfolio);
        auditService.record("PORTDEL", AuditService.TYPE_USER, "DELETE", AuditService.STATUS_SUCCESS,
                key.portfolioId(), key.accountNo(), reasonCode, portfolio.toString(), null,
                "Portfolio deleted, status " + portfolio.getStatus());
        return portfolio;
    }

    private static BigDecimal parseAmount(String value) {
        BigDecimal amount = CobolText.toAmount(value);
        if (amount == null) {
            throw new InvalidRequestException("Total value must be numeric: " + value.strip());
        }
        return amount;
    }
}
