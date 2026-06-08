package com.portfolio.application;

import com.portfolio.domain.event.PortfolioCreatedEvent;
import com.portfolio.domain.exception.ValidationException;
import com.portfolio.domain.model.ClientType;
import com.portfolio.domain.model.Portfolio;
import com.portfolio.domain.model.PortfolioStatus;
import com.portfolio.domain.repository.PortfolioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CRUD operations on the Portfolio aggregate, porting PORTADD, PORTREAD,
 * PORTUPDT, PORTDEL, PORTMSTR programs.
 */
@Service
@Transactional
public class PortfolioManagementService {

    private final PortfolioRepository portfolioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PortfolioManagementService(PortfolioRepository portfolioRepository,
                                      ApplicationEventPublisher eventPublisher) {
        this.portfolioRepository = portfolioRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new portfolio (PORTADD / PORTMSTR CREATE).
     * Validates inputs per COBOL validation rules, persists, and emits PortfolioCreatedEvent.
     */
    public Portfolio createPortfolio(String portfolioId, String accountNumber,
                                     String clientName, ClientType clientType) {
        validatePortfolioId(portfolioId);
        validateAccountNumber(accountNumber);
        validateClientName(clientName);

        if (portfolioRepository.existsById(portfolioId)) {
            throw new ValidationException(22, "Portfolio ID already exists: " + portfolioId);
        }

        Portfolio portfolio = new Portfolio(portfolioId, accountNumber, clientName, clientType);
        portfolio = portfolioRepository.save(portfolio);

        eventPublisher.publishEvent(new PortfolioCreatedEvent(
                portfolio.getPortfolioId(),
                portfolio.getAccountNumber(),
                null,
                LocalDateTime.now()
        ));

        return portfolio;
    }

    /**
     * Read a portfolio by ID (PORTREAD / PORTMSTR READ).
     */
    @Transactional(readOnly = true)
    public Portfolio readPortfolio(String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ValidationException(23,
                        "Portfolio not found: " + portfolioId));
    }

    /**
     * Read a portfolio by account number.
     */
    @Transactional(readOnly = true)
    public Optional<Portfolio> readPortfolioByAccount(String accountNumber) {
        return portfolioRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Update portfolio fields (PORTUPDT / PORTMSTR UPDATE).
     * Cannot update a CLOSED portfolio.
     */
    public Portfolio updatePortfolio(String portfolioId, String clientName,
                                     ClientType clientType) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ValidationException(23,
                        "Portfolio not found: " + portfolioId));

        if (portfolio.getStatus() == PortfolioStatus.CLOSED) {
            throw new ValidationException(8,
                    "Cannot update a CLOSED portfolio: " + portfolioId);
        }

        portfolio.setClientName(clientName);
        portfolio.setClientType(clientType);
        portfolio.markMaintenance(null);

        return portfolioRepository.save(portfolio);
    }

    /**
     * Soft-delete a portfolio (PORTDEL): set status to CLOSED.
     * Cannot delete an already-CLOSED portfolio.
     */
    public Portfolio deletePortfolio(String portfolioId, String userId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ValidationException(23,
                        "Portfolio not found: " + portfolioId));

        if (portfolio.getStatus() == PortfolioStatus.CLOSED) {
            throw new ValidationException(8,
                    "Cannot delete an already CLOSED portfolio: " + portfolioId);
        }

        portfolio.setStatus(PortfolioStatus.CLOSED);
        portfolio.markMaintenance(userId);

        return portfolioRepository.save(portfolio);
    }

    /**
     * List all portfolios (sequential read from PORTREAD).
     */
    @Transactional(readOnly = true)
    public List<Portfolio> listPortfolios() {
        return portfolioRepository.findAll();
    }

    // --- Validation helpers (ported from PORTADD.cbl 2100-VALIDATE-AND-ADD) ---

    private void validatePortfolioId(String portfolioId) {
        if (portfolioId == null || portfolioId.isBlank()) {
            throw new ValidationException(8, "Portfolio ID must not be blank");
        }
        if (portfolioId.length() != 8) {
            throw new ValidationException(8,
                    "Portfolio ID must be exactly 8 characters, got: " + portfolioId.length());
        }
    }

    private void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new ValidationException(8, "Account number must not be blank");
        }
        if (accountNumber.length() != 10) {
            throw new ValidationException(8,
                    "Account number must be exactly 10 characters, got: " + accountNumber.length());
        }
    }

    private void validateClientName(String clientName) {
        if (clientName == null || clientName.isBlank()) {
            throw new ValidationException(8, "Client name must not be blank");
        }
    }
}
