package com.portfolio.domain.service;

import com.portfolio.domain.command.TransactionCommand;
import com.portfolio.domain.exception.ValidationException;
import com.portfolio.domain.model.Portfolio;
import com.portfolio.domain.model.PortfolioStatus;
import com.portfolio.domain.model.TransactionType;
import com.portfolio.domain.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Ports PORTTRAN.cbl paragraphs 2110-CHECK-PORTFOLIO, 2120-CHECK-TRANSACTION-TYPE,
 * 2130-CHECK-AMOUNTS and PORTVALD.cbl / PORTVAL.cpy validation rules.
 *
 * Validation codes from PORTVAL.cpy:
 *   0 = success, 1 = invalid ID, 2 = invalid account, 3 = invalid type, 4 = invalid amount
 */
@Service
public class TransactionValidator {

    private static final String PORTFOLIO_ID_PREFIX = "PORT";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");

    private final PortfolioRepository portfolioRepository;

    public TransactionValidator(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public void validate(TransactionCommand command) {
        checkPortfolio(command);
        checkTransactionType(command);
        checkAmounts(command);
    }

    /** 2110-CHECK-PORTFOLIO: ID not blank, starts with "PORT", exists and is active */
    private void checkPortfolio(TransactionCommand command) {
        String portfolioId = command.portfolioId();

        if (portfolioId == null || portfolioId.isBlank()) {
            throw new ValidationException(1, "Portfolio ID is required");
        }

        if (!portfolioId.startsWith(PORTFOLIO_ID_PREFIX)) {
            throw new ValidationException(1, "Invalid Portfolio ID format");
        }

        Optional<Portfolio> portfolio = portfolioRepository.findById(portfolioId);
        if (portfolio.isEmpty()) {
            throw new ValidationException(1, "Invalid Portfolio ID: " + portfolioId);
        }

        if (portfolio.get().getStatus() != PortfolioStatus.ACTIVE) {
            throw new ValidationException(1, "Portfolio is not active: " + portfolioId);
        }
    }

    /** 2120-CHECK-TRANSACTION-TYPE: must be BU, SL, TR, or FE */
    private void checkTransactionType(TransactionCommand command) {
        if (command.type() == null) {
            throw new ValidationException(3, "Transaction type is required");
        }
    }

    /** 2130-CHECK-AMOUNTS: amount in range, quantity > 0 for BU/SL, price > 0 */
    private void checkAmounts(TransactionCommand command) {
        BigDecimal amount = command.amount();
        if (amount != null && (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0)) {
            throw new ValidationException(4, "Amount outside valid range");
        }

        TransactionType type = command.type();
        if (type == TransactionType.BUY || type == TransactionType.SELL) {
            if (command.quantity() == null || command.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(4, "Quantity must be greater than zero");
            }
        }

        if (type != TransactionType.TRANSFER) {
            if (command.price() == null || command.price().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(4, "Price must be greater than zero");
            }
        }
    }
}
