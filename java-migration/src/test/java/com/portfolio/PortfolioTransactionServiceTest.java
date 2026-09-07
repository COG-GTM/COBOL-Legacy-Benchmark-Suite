package com.portfolio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.portfolio.config.ErrorLoggingService;
import com.portfolio.domain.Portfolio;
import com.portfolio.domain.Transaction;
import com.portfolio.domain.TransactionType;
import com.portfolio.dto.TransactionDto;
import com.portfolio.repository.AuditLogRepository;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.service.BusinessException;
import com.portfolio.service.PortfolioTransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PortfolioTransactionServiceTest {
  @Mock PortfolioRepository portfolioRepository;
  @Mock TransactionRepository transactionRepository;
  @Mock AuditLogRepository auditLogRepository;
  @Mock ErrorLoggingService errorLoggingService;
  private PortfolioTransactionService transactionService;
  Portfolio portfolio;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    transactionService =
        new PortfolioTransactionService(
            portfolioRepository, transactionRepository, auditLogRepository, errorLoggingService);
    portfolio = new Portfolio();
    portfolio.setPortfolioId("PORT0001");
    portfolio.setAccountNo("1234567890");
    portfolio.setTotalUnits(new BigDecimal("10.0000"));
    portfolio.setTotalCost(new BigDecimal("100.00"));
    when(portfolioRepository.findById("PORT0001")).thenReturn(java.util.Optional.of(portfolio));
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(portfolioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private Transaction createTransaction(
      TransactionType transactionType,
      BigDecimal quantity,
      BigDecimal price,
      BigDecimal transactionAmount) {
    Transaction transaction = new Transaction();
    transaction.setTransactionId("id");
    transaction.setPortfolioId("PORT0001");
    transaction.setTransactionType(transactionType);
    transaction.setQuantity(quantity);
    transaction.setPrice(price);
    transaction.setAmount(transactionAmount);
    transaction.setTransactionDate(LocalDate.now());
    return transaction;
  }

  @Test
  void buyAddsUnitsAndCost() {
    transactionService.process(
        createTransaction(
            TransactionType.BUY, new BigDecimal("2"), new BigDecimal("10"), new BigDecimal("20")));
    assertEquals(new BigDecimal("12.0000"), portfolio.getTotalUnits());
    assertEquals(new BigDecimal("120.00"), portfolio.getTotalCost());
  }

  @Test
  void sellWithInsufficientUnitsThrows() {
    BusinessException businessException =
        assertThrows(
            BusinessException.class,
            () ->
                transactionService.process(
                    createTransaction(
                        TransactionType.SELL,
                        new BigDecimal("11"),
                        BigDecimal.ONE,
                        BigDecimal.TEN)));
    assertEquals("Insufficient units for sale", businessException.getMessage());
  }

  @Test
  void sellSubtractsUnits() {
    transactionService.process(
        createTransaction(
            TransactionType.SELL, new BigDecimal("2"), BigDecimal.ONE, new BigDecimal("20")));
    assertEquals(new BigDecimal("8.0000"), portfolio.getTotalUnits());
  }

  @Test
  void feeSubtractsCost() {
    transactionService.process(
        createTransaction(
            TransactionType.FEE, BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("5")));
    assertEquals(new BigDecimal("95.00"), portfolio.getTotalCost());
  }

  @Test
  void transferIsNotImplemented() {
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            transactionService.process(
                createTransaction(
                    TransactionType.TRANSFER, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO)));
  }

  @Test
  void validateAmountsRejectsInvalidValues() {
    assertThrows(
        BusinessException.class,
        () ->
            transactionService.validateAmounts(
                createTransaction(
                    TransactionType.BUY, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE)));
    assertThrows(
        BusinessException.class,
        () ->
            transactionService.validateAmounts(
                createTransaction(
                    TransactionType.BUY, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE)));
    assertThrows(
        BusinessException.class,
        () ->
            transactionService.validateAmounts(
                createTransaction(
                    TransactionType.BUY, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO)));
    assertDoesNotThrow(
        () ->
            transactionService.validateAmounts(
                createTransaction(
                    TransactionType.TRANSFER, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO)));
  }

  @Test
  void createPendingUsesNextDailySequenceNumber() {
    TransactionDto transactionDto = new TransactionDto();
    transactionDto.setPortfolioId("PORT0001");
    transactionDto.setInvestmentId("STK000001");
    transactionDto.setTransactionType("BU");
    transactionDto.setQuantity(BigDecimal.ONE);
    transactionDto.setPrice(BigDecimal.TEN);
    transactionDto.setAmount(BigDecimal.TEN);
    transactionDto.setTransactionDate(LocalDate.of(2024, 1, 2));
    transactionDto.setTransactionTime(java.time.LocalTime.of(10, 15, 30));
    when(transactionRepository.countByPortfolioIdAndTransactionDate(
            "PORT0001", LocalDate.of(2024, 1, 2)))
        .thenReturn(7L);

    Transaction pendingTransaction = transactionService.createPending(transactionDto);

    assertEquals("000008", pendingTransaction.getSequenceNo());
    assertEquals("20240102101530PORT0001000008", pendingTransaction.getTransactionId());
  }
}
