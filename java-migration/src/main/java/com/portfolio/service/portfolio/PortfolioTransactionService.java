package com.portfolio.service.portfolio;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.Position;
import com.portfolio.domain.Transaction;
import com.portfolio.exception.ProcessingException;
import com.portfolio.exception.ValidationException;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.service.common.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Portfolio Transaction Service - migrated from COBOL PORTTRAN.cbl.
 * Handles transaction processing for portfolios.
 */
@Service
public class PortfolioTransactionService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioTransactionService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final AuditService auditService;

    public PortfolioTransactionService(TransactionRepository transactionRepository,
                                       PortfolioRepository portfolioRepository,
                                       PositionRepository positionRepository,
                                       AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Transaction processTransaction(Transaction transaction) {
        validateTransaction(transaction);

        Portfolio portfolio = portfolioRepository.findById(transaction.getPortfolioId())
                .orElseThrow(() -> new ProcessingException(
                        "Portfolio not found: " + transaction.getPortfolioId()));

        if (!portfolio.isActive()) {
            throw new ValidationException("Portfolio is not active: " + portfolio.getPortfolioId());
        }

        LocalDateTime now = LocalDateTime.now();
        String txnId = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", System.nanoTime() % 1000000);
        transaction.setTransactionId(txnId);
        transaction.setTransactionDate(LocalDate.now());
        transaction.setTransactionTime(now.format(TIME_FMT));
        transaction.setProcessDate(now);
        transaction.setStatus("D");

        BigDecimal amount = transaction.getQuantity().multiply(transaction.getPrice());
        transaction.setAmount(amount);

        updatePortfolioBalance(portfolio, transaction);

        Transaction saved = transactionRepository.save(transaction);
        portfolioRepository.save(portfolio);

        auditService.logTransaction(transaction.getProcessUser(), "PORTTRAN",
                portfolio.getPortfolioId(), "CREATE", "SUCC",
                "Transaction processed: " + transaction.getTransactionType());

        log.info("Transaction processed: {} for portfolio {}", saved.getTransactionId(),
                portfolio.getPortfolioId());
        return saved;
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().trim().isEmpty()) {
            throw new ValidationException("Portfolio ID is required");
        }
        if (transaction.getTransactionType() == null) {
            throw new ValidationException("Transaction type is required");
        }
        if (transaction.getQuantity() == null || transaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity must be positive");
        }
        if (transaction.getPrice() == null || transaction.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Price must be non-negative");
        }
    }

    private void updatePortfolioBalance(Portfolio portfolio, Transaction transaction) {
        BigDecimal amount = transaction.getAmount();

        if (transaction.isBuy()) {
            portfolio.setCashBalance(portfolio.getCashBalance().subtract(amount));
            portfolio.setTotalValue(portfolio.getTotalValue().add(amount));
        } else if (transaction.isSell()) {
            portfolio.setCashBalance(portfolio.getCashBalance().add(amount));
            portfolio.setTotalValue(portfolio.getTotalValue().subtract(amount));
        } else if (transaction.isFee()) {
            portfolio.setCashBalance(portfolio.getCashBalance().subtract(amount));
        }

        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolio.setLastTransDate(LocalDate.now());
    }
}
