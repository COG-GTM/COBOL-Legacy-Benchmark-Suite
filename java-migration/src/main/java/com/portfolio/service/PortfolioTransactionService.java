package com.portfolio.service;

import com.portfolio.entity.Portfolio;
import com.portfolio.entity.PositionRecord;
import com.portfolio.entity.TransactionRecord;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.util.CommonConstants;
import com.portfolio.util.PortfolioValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PortfolioTransactionService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioTransactionService.class);
    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final AuditProcessor auditProcessor;
    private final DatabaseErrorHandler errorHandler;
    private final AtomicLong sequenceCounter = new AtomicLong(1);

    public PortfolioTransactionService(TransactionRepository transactionRepository,
                                       PortfolioRepository portfolioRepository,
                                       PositionRepository positionRepository,
                                       AuditProcessor auditProcessor,
                                       DatabaseErrorHandler errorHandler) {
        this.transactionRepository = transactionRepository;
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.auditProcessor = auditProcessor;
        this.errorHandler = errorHandler;
    }

    @Transactional
    public TransactionRecord processTransaction(TransactionRecord transaction) {
        List<String> errors = PortfolioValidation.validateTransaction(transaction);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + String.join(", ", errors));
        }

        Portfolio portfolio = portfolioRepository.findById(transaction.getPortfolioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid Portfolio ID: " + transaction.getPortfolioId()));

        if (!portfolio.isActive()) {
            throw new IllegalArgumentException("Portfolio is not active: " + transaction.getPortfolioId());
        }

        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(generateTransactionId());
        }
        transaction.setTransactionDate(LocalDate.now());
        transaction.setTransactionTime(LocalTime.now());
        transaction.setProcessDate(LocalDateTime.now());
        transaction.setProcessUser("SYSTEM");
        transaction.setStatus("D");

        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            if (transaction.getPrice() != null) {
                transaction.setAmount(transaction.getQuantity().multiply(transaction.getPrice()));
            } else {
                transaction.setAmount(BigDecimal.ZERO);
            }
        }

        updatePosition(transaction);
        updatePortfolioValue(portfolio, transaction);

        TransactionRecord saved = transactionRepository.save(transaction);
        auditProcessor.logTransaction("SYSTEM", transaction.getPortfolioId(),
                CommonConstants.AUDIT_ACTION_CREATE,
                transaction.getTransactionType() + " transaction processed: " + saved.getTransactionId());
        log.info("Transaction processed: {} for portfolio {}", saved.getTransactionId(), saved.getPortfolioId());
        return saved;
    }

    private void updatePosition(TransactionRecord transaction) {
        List<PositionRecord> existingPositions = positionRepository
                .findByPortfolioIdAndPositionDate(
                        transaction.getPortfolioId(), LocalDate.now());

        PositionRecord position = existingPositions.stream()
                .filter(p -> p.getInvestmentId().equals(transaction.getInvestmentId()))
                .findFirst()
                .orElse(null);

        if (position == null) {
            position = new PositionRecord();
            position.setPortfolioId(transaction.getPortfolioId());
            position.setInvestmentId(transaction.getInvestmentId());
            position.setPositionDate(LocalDate.now());
            position.setQuantity(BigDecimal.ZERO);
            position.setCostBasis(BigDecimal.ZERO);
            position.setMarketValue(BigDecimal.ZERO);
            position.setCurrencyCode(transaction.getCurrencyCode());
            position.setStatus(CommonConstants.STATUS_ACTIVE);
        }

        switch (transaction.getTransactionType()) {
            case "BU" -> {
                position.setQuantity(position.getQuantity().add(transaction.getQuantity()));
                position.setCostBasis(position.getCostBasis().add(transaction.getAmount()));
                position.setMarketValue(position.getQuantity().multiply(transaction.getPrice()));
            }
            case "SL" -> {
                position.setQuantity(position.getQuantity().subtract(transaction.getQuantity()));
                BigDecimal costReduction = transaction.getAmount();
                position.setCostBasis(position.getCostBasis().subtract(costReduction));
                position.setMarketValue(position.getQuantity().multiply(transaction.getPrice()));
                if (position.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    position.setStatus(CommonConstants.STATUS_CLOSED);
                }
            }
            case "TR" -> {
                position.setQuantity(position.getQuantity().add(transaction.getQuantity()));
            }
            case "FE" -> {
                position.setCostBasis(position.getCostBasis().add(transaction.getAmount()));
            }
        }

        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("SYSTEM");
        positionRepository.save(position);
    }

    private void updatePortfolioValue(Portfolio portfolio, TransactionRecord transaction) {
        List<PositionRecord> positions = positionRepository.findActivePositions(portfolio.getPortfolioId());
        BigDecimal totalValue = positions.stream()
                .map(PositionRecord::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        portfolio.setTotalValue(totalValue);
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolioRepository.save(portfolio);
    }

    public List<TransactionRecord> getTransactionHistory(String portfolioId) {
        return transactionRepository.findHistoryByPortfolioId(portfolioId);
    }

    public List<TransactionRecord> getPendingTransactions() {
        return transactionRepository.findByStatus(CommonConstants.STATUS_PENDING);
    }

    private String generateTransactionId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return datePart + String.format("%06d", sequenceCounter.getAndIncrement());
    }
}
