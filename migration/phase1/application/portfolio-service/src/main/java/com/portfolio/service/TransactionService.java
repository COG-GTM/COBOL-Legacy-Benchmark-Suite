package com.portfolio.service;

import com.portfolio.entity.Transaction;
import com.portfolio.entity.Transaction.TransactionStatus;
import com.portfolio.entity.Transaction.TransactionType;
import com.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Transaction operations.
 * Replaces COBOL TRNVAL00 (Transaction Validation) and POSUPD00 (Position Update) programs.
 * 
 * @see src/programs/batch/TRNVAL00.cbl
 * @see src/programs/batch/POSUPD00.cbl
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PositionService positionService;
    private final AuditService auditService;

    public Optional<Transaction> findByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    public Optional<Transaction> findById(UUID id) {
        return transactionRepository.findById(id);
    }

    public Page<Transaction> findByPortfolioId(String portfolioId, Pageable pageable) {
        return transactionRepository.findByPortfolioId(portfolioId, pageable);
    }

    public List<Transaction> findByPortfolioIdAndDateRange(String portfolioId, 
                                                           LocalDate startDate, 
                                                           LocalDate endDate) {
        return transactionRepository.findByPortfolioIdAndTransactionDateBetween(
                portfolioId, startDate, endDate);
    }

    public Page<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate, pageable);
    }

    public List<Transaction> findPendingTransactions() {
        return transactionRepository.findPendingTransactions();
    }

    @Transactional
    public Transaction create(Transaction transaction, String userId) {
        log.info("Creating transaction: type={}, portfolio={}, investment={}", 
                 transaction.getTransactionType(), 
                 transaction.getPortfolioId(), 
                 transaction.getInvestmentId());
        
        validateTransaction(transaction);
        
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(generateTransactionId(transaction));
        }
        
        if (transaction.getTotalAmount() == null) {
            BigDecimal fees = transaction.getFees() != null ? transaction.getFees() : BigDecimal.ZERO;
            transaction.setTotalAmount(transaction.getAmount().add(fees));
        }
        
        transaction.setStatus(TransactionStatus.PENDING);
        
        Transaction saved = transactionRepository.save(transaction);
        
        auditService.logTransactionAction(saved.getTransactionId(), "CREATE", userId);
        
        return saved;
    }

    @Transactional
    public Transaction process(String transactionId, String userId) {
        log.info("Processing transaction: {}", transactionId);
        
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction is not in PENDING status");
        }
        
        try {
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProcessDate(OffsetDateTime.now());
            transaction.setProcessUser(userId);
            transaction.setResultCode("0000");
            
            Transaction saved = transactionRepository.save(transaction);
            
            auditService.logTransactionAction(transactionId, "PROCESS", userId);
            
            return saved;
            
        } catch (Exception e) {
            log.error("Error processing transaction: {}", transactionId, e);
            
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setResultCode("9999");
            transaction.setProcessDate(OffsetDateTime.now());
            transaction.setProcessUser(userId);
            
            transactionRepository.save(transaction);
            
            auditService.logTransactionAction(transactionId, "FAIL", userId);
            
            throw e;
        }
    }

    @Transactional
    public Transaction reverse(String transactionId, String userId) {
        log.info("Reversing transaction: {}", transactionId);
        
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Only completed transactions can be reversed");
        }
        
        transaction.setStatus(TransactionStatus.REVERSED);
        transaction.setProcessDate(OffsetDateTime.now());
        transaction.setProcessUser(userId);
        
        Transaction saved = transactionRepository.save(transaction);
        
        auditService.logTransactionAction(transactionId, "REVERSE", userId);
        
        return saved;
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().isBlank()) {
            throw new IllegalArgumentException("Portfolio ID is required");
        }
        if (transaction.getInvestmentId() == null || transaction.getInvestmentId().isBlank()) {
            throw new IllegalArgumentException("Investment ID is required");
        }
        if (transaction.getTransactionType() == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (transaction.getQuantity() == null || transaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (transaction.getPrice() == null || transaction.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }

    private String generateTransactionId(Transaction transaction) {
        LocalDate date = transaction.getTransactionDate() != null ? 
                transaction.getTransactionDate() : LocalDate.now();
        LocalTime time = transaction.getTransactionTime() != null ? 
                transaction.getTransactionTime() : LocalTime.now();
        String seq = transaction.getSequenceNo() != null ? 
                transaction.getSequenceNo() : String.format("%06d", System.nanoTime() % 1000000);
        
        return String.format("%s%s%s", 
                date.toString().replace("-", ""),
                time.toString().replace(":", "").substring(0, 6),
                seq);
    }

    public BigDecimal sumAmountByPortfolioAndType(String portfolioId, TransactionType type) {
        BigDecimal sum = transactionRepository.sumAmountByPortfolioAndType(portfolioId, type);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public long countByDateAndStatus(LocalDate date, TransactionStatus status) {
        return transactionRepository.countByDateAndStatus(date, status);
    }
}
