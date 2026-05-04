package com.portfolio.service;

import com.portfolio.audit.AuditService;
import com.portfolio.dto.TransactionRequest;
import com.portfolio.entity.AuditAction;
import com.portfolio.entity.AuditStatus;
import com.portfolio.entity.AuditType;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.entity.TransactionHistory;
import com.portfolio.entity.TransactionStatus;
import com.portfolio.entity.TransactionType;
import com.portfolio.exception.InsufficientUnitsException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.exception.ValidationException;
import com.portfolio.repository.PortfolioMasterRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private final PortfolioMasterRepository portfolioRepository;
    private final TransactionHistoryRepository transactionRepository;
    private final AuditService auditService;

    public TransactionService(PortfolioMasterRepository portfolioRepository,
                              TransactionHistoryRepository transactionRepository,
                              AuditService auditService) {
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TransactionHistory processTransaction(TransactionRequest request) {
        PortfolioMaster portfolio = portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(request.getPortfolioId()));

        TransactionType transactionType = parseTransactionType(request.getTransactionType());
        validateTransaction(request, transactionType);

        TransactionHistory transaction = createTransactionRecord(request, transactionType);

        switch (transactionType) {
            case BUY -> processBuy(portfolio, request);
            case SELL -> processSell(portfolio, request);
            case TRANSFER -> processTransfer(request);
            case FEE -> processFee(portfolio, request);
        }

        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolio.setLastUser("SYSTEM");
        portfolioRepository.save(portfolio);

        transaction.setStatus(TransactionStatus.DONE);
        transaction.setProcessDate(LocalDateTime.now());
        transaction.setProcessUser("SYSTEM");
        TransactionHistory saved = transactionRepository.save(transaction);

        auditService.logAudit("BATCH", "SYSTEM", "PORTTRAN", null,
                AuditType.TRANSACTION, AuditAction.CREATE, AuditStatus.SUCCESS,
                portfolio.getPortfolioId(), portfolio.getAccountNo(),
                null, transactionType + ":" + request.getAmount(),
                "Transaction processed: " + transactionType);

        return saved;
    }

    private void processBuy(PortfolioMaster portfolio, TransactionRequest request) {
        BigDecimal currentValue = portfolio.getTotalValue() != null
                ? portfolio.getTotalValue() : BigDecimal.ZERO;
        portfolio.setTotalValue(currentValue.add(request.getAmount()));
        log.info("BUY: portfolio={} amount={} newTotal={}",
                portfolio.getPortfolioId(), request.getAmount(), portfolio.getTotalValue());
    }

    private void processSell(PortfolioMaster portfolio, TransactionRequest request) {
        BigDecimal currentValue = portfolio.getTotalValue() != null
                ? portfolio.getTotalValue() : BigDecimal.ZERO;
        if (currentValue.compareTo(request.getAmount()) < 0) {
            throw new InsufficientUnitsException(
                    "Insufficient portfolio value. Current: " + currentValue
                            + ", Requested: " + request.getAmount());
        }
        portfolio.setTotalValue(currentValue.subtract(request.getAmount()));
        log.info("SELL: portfolio={} amount={} newTotal={}",
                portfolio.getPortfolioId(), request.getAmount(), portfolio.getTotalValue());
    }

    private void processTransfer(TransactionRequest request) {
        throw new UnsupportedOperationException("Transfer processing not implemented");
    }

    private void processFee(PortfolioMaster portfolio, TransactionRequest request) {
        BigDecimal currentValue = portfolio.getTotalValue() != null
                ? portfolio.getTotalValue() : BigDecimal.ZERO;
        portfolio.setTotalValue(currentValue.subtract(request.getAmount()));
        log.info("FEE: portfolio={} amount={} newTotal={}",
                portfolio.getPortfolioId(), request.getAmount(), portfolio.getTotalValue());
    }

    private TransactionType parseTransactionType(String type) {
        try {
            return TransactionType.fromCode(type);
        } catch (IllegalArgumentException e) {
            try {
                return TransactionType.valueOf(type);
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("Invalid transaction type: " + type
                        + ". Must be BU, SL, TR, or FE");
            }
        }
    }

    private void validateTransaction(TransactionRequest request, TransactionType type) {
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity must be greater than 0");
        }
        if (type != TransactionType.TRANSFER) {
            if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Price must be greater than 0 for non-transfer transactions");
            }
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than 0");
        }
    }

    private TransactionHistory createTransactionRecord(TransactionRequest request,
                                                       TransactionType type) {
        TransactionHistory txn = new TransactionHistory();
        txn.setTransactionId(generateTransactionId());
        txn.setPortfolioId(request.getPortfolioId());
        txn.setTransactionDate(LocalDate.now());
        txn.setTransactionTime(LocalTime.now());
        txn.setInvestmentId(request.getInvestmentId());
        txn.setTransactionType(type);
        txn.setQuantity(request.getQuantity());
        txn.setPrice(request.getPrice());
        txn.setAmount(request.getAmount());
        txn.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        txn.setStatus(TransactionStatus.PENDING);
        return txn;
    }

    private String generateTransactionId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long seq = SEQUENCE.incrementAndGet() % 1000000;
        return datePart + String.format("%06d", seq);
    }
}
