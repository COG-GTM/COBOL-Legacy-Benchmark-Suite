package com.portfolio.service.transaction;

import com.portfolio.exception.InsufficientUnitsException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.exception.TransactionProcessingException;
import com.portfolio.model.dto.CommonConstants;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.entity.Transaction;
import com.portfolio.model.enums.AuditAction;
import com.portfolio.model.enums.AuditStatus;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.service.common.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionProcessingService {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessingService.class);
    private static final int MAX_ERROR_COUNT = 100;

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final AuditService auditService;

    public TransactionProcessingService(TransactionRepository transactionRepository,
                                        PortfolioRepository portfolioRepository,
                                        AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.portfolioRepository = portfolioRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TransactionResult processTransactions(List<Transaction> transactions) {
        int readCount = 0;
        int processCount = 0;
        int errorCount = 0;

        for (Transaction txn : transactions) {
            if (errorCount > MAX_ERROR_COUNT) {
                log.error("Error threshold exceeded ({} errors). Stopping.", errorCount);
                break;
            }

            readCount++;
            try {
                validateTransaction(txn);
                updatePositions(txn);
                updateAuditTrail(txn, true);
                processCount++;
            } catch (Exception e) {
                errorCount++;
                updateAuditTrail(txn, false);
                log.warn("Transaction error: {}", e.getMessage());
            }
        }

        log.info("Transactions Read: {}", readCount);
        log.info("Transactions Processed: {}", processCount);
        log.info("Errors Encountered: {}", errorCount);

        return new TransactionResult(readCount, processCount, errorCount);
    }

    private void validateTransaction(Transaction txn) {
        if (txn.getPortfolioId() == null || txn.getPortfolioId().isBlank()) {
            throw new TransactionProcessingException("Portfolio ID is required");
        }

        if (!portfolioRepository.existsById(txn.getPortfolioId())) {
            throw new PortfolioNotFoundException(txn.getPortfolioId());
        }

        String type = txn.getTransactionType();
        if (!CommonConstants.TRN_TYPE_BUY.equals(type)
                && !CommonConstants.TRN_TYPE_SELL.equals(type)
                && !CommonConstants.TRN_TYPE_TRANSFER.equals(type)
                && !CommonConstants.TRN_TYPE_FEE.equals(type)) {
            throw new TransactionProcessingException("Invalid Transaction Type: " + type);
        }

        if (txn.getQuantity() == null || txn.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionProcessingException("Quantity must be greater than zero");
        }

        if (!CommonConstants.TRN_TYPE_TRANSFER.equals(type)) {
            if (txn.getPrice() == null || txn.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TransactionProcessingException("Price must be greater than zero");
            }
            if (txn.getAmount() == null || txn.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TransactionProcessingException("Amount must be greater than zero");
            }
        }
    }

    private void updatePositions(Transaction txn) {
        String type = txn.getTransactionType();

        switch (type) {
            case CommonConstants.TRN_TYPE_BUY -> processBuy(txn);
            case CommonConstants.TRN_TYPE_SELL -> processSell(txn);
            case CommonConstants.TRN_TYPE_TRANSFER -> processTransfer();
            case CommonConstants.TRN_TYPE_FEE -> processFee(txn);
            default -> throw new TransactionProcessingException("Unknown transaction type: " + type);
        }
    }

    private void processBuy(Transaction txn) {
        Portfolio portfolio = portfolioRepository.findById(txn.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(txn.getPortfolioId()));

        BigDecimal currentValue = portfolio.getTotalValue() != null ? portfolio.getTotalValue() : BigDecimal.ZERO;
        portfolio.setTotalValue(currentValue.add(txn.getAmount()));
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolioRepository.save(portfolio);

        txn.setStatus('D');
        txn.setProcessDate(LocalDateTime.now());
        transactionRepository.save(txn);
    }

    private void processSell(Transaction txn) {
        Portfolio portfolio = portfolioRepository.findById(txn.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(txn.getPortfolioId()));

        BigDecimal currentValue = portfolio.getTotalValue() != null ? portfolio.getTotalValue() : BigDecimal.ZERO;
        if (currentValue.compareTo(txn.getAmount()) < 0) {
            throw new InsufficientUnitsException(txn.getPortfolioId());
        }

        portfolio.setTotalValue(currentValue.subtract(txn.getAmount()));
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolioRepository.save(portfolio);

        txn.setStatus('D');
        txn.setProcessDate(LocalDateTime.now());
        transactionRepository.save(txn);
    }

    private void processTransfer() {
        throw new UnsupportedOperationException("Transfer processing not implemented");
    }

    private void processFee(Transaction txn) {
        Portfolio portfolio = portfolioRepository.findById(txn.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(txn.getPortfolioId()));

        BigDecimal currentValue = portfolio.getTotalValue() != null ? portfolio.getTotalValue() : BigDecimal.ZERO;
        portfolio.setTotalValue(currentValue.subtract(txn.getAmount()));
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolioRepository.save(portfolio);

        txn.setStatus('D');
        txn.setProcessDate(LocalDateTime.now());
        transactionRepository.save(txn);
    }

    private void updateAuditTrail(Transaction txn, boolean success) {
        AuditAction action;
        switch (txn.getTransactionType()) {
            case CommonConstants.TRN_TYPE_BUY -> action = AuditAction.CREATE;
            case CommonConstants.TRN_TYPE_SELL -> action = AuditAction.DELETE;
            default -> action = AuditAction.UPDATE;
        }

        AuditStatus status = success ? AuditStatus.SUCCESS : AuditStatus.FAILURE;
        String message = String.format("Transaction: %s Amount: %s Units: %s",
                txn.getTransactionType(), txn.getAmount(), txn.getQuantity());

        auditService.logTransaction(txn.getPortfolioId(), null, action, status,
                txn.getProcessUser(), "PORTTRAN", null, null, message);
    }

    public record TransactionResult(int readCount, int processCount, int errorCount) {
    }
}
