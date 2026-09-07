package com.portfolio.service;

import com.portfolio.config.ErrorLoggingService;
import com.portfolio.domain.*;
import com.portfolio.dto.TransactionDto;
import com.portfolio.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import org.springframework.dao.*;
import org.springframework.retry.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioTransactionService {
  public record TransactionResult(String transactionId, boolean success, String message) {}

  private final PortfolioRepository portfolioRepository;
  private final TransactionRepository transactionRepository;
  private final AuditLogRepository auditLogRepository;
  private final ErrorLoggingService errorLoggingService;

  public PortfolioTransactionService(
      PortfolioRepository portfolioRepository,
      TransactionRepository transactionRepository,
      AuditLogRepository auditLogRepository,
      ErrorLoggingService errorLoggingService) {
    this.portfolioRepository = portfolioRepository;
    this.transactionRepository = transactionRepository;
    this.auditLogRepository = auditLogRepository;
    this.errorLoggingService = errorLoggingService;
  }

  @Transactional
  @Retryable(
      retryFor = {TransientDataAccessException.class, CannotAcquireLockException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 100))
  public TransactionResult process(Transaction transaction) {
    Portfolio portfolio = null;
    String beforeImage = "";
    try {
      validatePortfolio(transaction);
      validateTransactionType(transaction);
      validateAmounts(transaction);
      portfolio =
          portfolioRepository
              .findById(transaction.getPortfolioId())
              .orElseThrow(
                  () ->
                      new BusinessException(
                          "E008", "Invalid Portfolio ID: " + transaction.getPortfolioId()));
      beforeImage = state(portfolio);
      updatePositions(portfolio, transaction);
      portfolio.setLastTransDate(
          transaction.getTransactionDate() == null
              ? LocalDate.now()
              : transaction.getTransactionDate());
      portfolio.setLastUser(transaction.getProcessUser());
      transaction.setStatus(TransactionStatus.DONE);
      transaction.setProcessDate(LocalDateTime.now());
      transactionRepository.save(transaction);
      portfolioRepository.save(portfolio);
      updateAuditTrail(transaction, portfolio, AuditStatus.SUCCESS, beforeImage);
      return new TransactionResult(transaction.getTransactionId(), true, "Processed");
    } catch (BusinessException ex) {
      if (transaction != null) {
        transaction.setStatus(TransactionStatus.FAILED);
        if (transaction.getTransactionId() != null) {
          transactionRepository.save(transaction);
        }
      }
      errorLoggingService.log(
          "PORTTRAN", ErrorType.APPLICATION, ErrorSeverity.WARNING, "PR", ex.getMessage(), "");
      if (portfolio != null) {
        updateAuditTrail(transaction, portfolio, AuditStatus.FAILURE, beforeImage);
      }
      throw ex;
    }
  }

  private void validatePortfolio(Transaction transaction) {
    if (transaction.getPortfolioId() == null || transaction.getPortfolioId().isBlank()) {
      throw new BusinessException("E008", "Portfolio ID is required");
    }
  }

  private void validateTransactionType(Transaction transaction) {
    if (transaction.getTransactionType() == null) {
      throw new BusinessException("E008", "Invalid Transaction Type: null");
    }
  }

  public void validateAmounts(Transaction transaction) {
    if (zeroIfNull(transaction.getQuantity()).compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("E008", "Quantity must be greater than zero");
    }
    if (transaction.getTransactionType() != TransactionType.TRANSFER
        && zeroIfNull(transaction.getPrice()).compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("E008", "Price must be greater than zero");
    }
    if (transaction.getTransactionType() != TransactionType.TRANSFER
        && zeroIfNull(transaction.getAmount()).compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("E008", "Amount must be greater than zero");
    }
  }

  private void updatePositions(Portfolio portfolio, Transaction transaction) {
    switch (transaction.getTransactionType()) {
      case BUY -> {
        portfolio.setTotalUnits(
            portfolio.getTotalUnits().add(zeroIfNull(transaction.getQuantity())));
        portfolio.setTotalCost(portfolio.getTotalCost().add(zeroIfNull(transaction.getAmount())));
      }
      case SELL -> {
        if (portfolio.getTotalUnits().compareTo(zeroIfNull(transaction.getQuantity())) < 0) {
          throw new BusinessException("E008", "Insufficient units for sale");
        }
        portfolio.setTotalUnits(
            portfolio.getTotalUnits().subtract(zeroIfNull(transaction.getQuantity())));
        portfolio.setTotalCost(
            portfolio.getTotalCost().subtract(zeroIfNull(transaction.getAmount())));
      }
      case FEE ->
          portfolio.setTotalCost(
              portfolio.getTotalCost().subtract(zeroIfNull(transaction.getAmount())));
      case TRANSFER ->
          throw new UnsupportedOperationException("Transfer processing not implemented");
    }
  }

  private void updateAuditTrail(
      Transaction transaction, Portfolio portfolio, AuditStatus auditStatus, String beforeImage) {
    AuditLog auditLog = new AuditLog();
    auditLog.setAudTimestamp(LocalDateTime.now());
    auditLog.setProgram("PORTTRAN");
    auditLog.setAudType(AuditType.TRAN);
    auditLog.setAction(
        switch (transaction.getTransactionType()) {
          case BUY -> AuditAction.CREATE;
          case SELL -> AuditAction.DELETE;
          default -> AuditAction.UPDATE;
        });
    auditLog.setStatus(auditStatus);
    auditLog.setPortfolioId(portfolio.getPortfolioId());
    auditLog.setAccountNo(portfolio.getAccountNo());
    auditLog.setBeforeImage(beforeImage);
    auditLog.setMessage(
        "Transaction: "
            + transaction.getTransactionType().getCode()
            + " Amount: "
            + transaction.getAmount()
            + " Units: "
            + transaction.getQuantity());
    auditLogRepository.save(auditLog);
  }

  private String state(Portfolio portfolio) {
    String portfolioState =
        portfolio.getPortfolioId()
            + "|"
            + portfolio.getTotalUnits()
            + "|"
            + portfolio.getTotalCost();
    return portfolioState.length() > 100 ? portfolioState.substring(0, 100) : portfolioState;
  }

  private BigDecimal zeroIfNull(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }

  @Transactional
  public Transaction createPending(TransactionDto transactionDto) {
    Transaction transaction = new Transaction();
    LocalDate transactionDate =
        transactionDto.getTransactionDate() == null
            ? LocalDate.now()
            : transactionDto.getTransactionDate();
    LocalTime transactionTime =
        transactionDto.getTransactionTime() == null
            ? LocalTime.now()
            : transactionDto.getTransactionTime();
    long sequenceNumber =
        transactionRepository.countByPortfolioIdAndTransactionDate(
                transactionDto.getPortfolioId(), transactionDate)
            + 1;
    String sequenceNo = String.format("%06d", sequenceNumber);
    transaction.setTransactionDate(transactionDate);
    transaction.setTransactionTime(transactionTime);
    transaction.setPortfolioId(transactionDto.getPortfolioId());
    transaction.setSequenceNo(sequenceNo);
    transaction.setTransactionId(
        transactionDate.format(DateTimeFormatter.BASIC_ISO_DATE)
            + transactionTime.format(DateTimeFormatter.ofPattern("HHmmss"))
            + transactionDto.getPortfolioId()
            + sequenceNo);
    transaction.setInvestmentId(transactionDto.getInvestmentId());
    transaction.setTransactionType(TransactionType.fromCode(transactionDto.getTransactionType()));
    transaction.setQuantity(transactionDto.getQuantity());
    transaction.setPrice(transactionDto.getPrice());
    transaction.setAmount(transactionDto.getAmount());
    transaction.setCurrencyCode(transactionDto.getCurrencyCode());
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setProcessUser(transactionDto.getProcessUser());
    return transactionRepository.save(transaction);
  }

  public TransactionResult applyPending(Transaction transaction) {
    return process(transaction);
  }
}
