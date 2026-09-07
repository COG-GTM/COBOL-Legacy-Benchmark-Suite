package com.portfolio.batch;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.PositionHistory;
import com.portfolio.domain.PositionHistoryId;
import com.portfolio.domain.Transaction;
import com.portfolio.domain.TransactionType;
import com.portfolio.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class TransactionToPositionHistoryProcessor
    implements ItemProcessor<Transaction, PositionHistory> {
  private final PortfolioRepository portfolioRepository;
  private final Map<String, Portfolio> portfolioCache = new HashMap<>();

  public TransactionToPositionHistoryProcessor(PortfolioRepository portfolioRepository) {
    this.portfolioRepository = portfolioRepository;
  }

  @BeforeStep
  public void clearPortfolioCache(StepExecution stepExecution) {
    portfolioCache.clear();
  }

  @Override
  public PositionHistory process(Transaction transaction) {
    Portfolio portfolio =
        portfolioCache.computeIfAbsent(
            transaction.getPortfolioId(),
            portfolioId ->
                portfolioRepository
                    .findById(portfolioId)
                    .orElseThrow(
                        () -> new IllegalStateException("Portfolio not found: " + portfolioId)));
    LocalDate transactionDate =
        transaction.getTransactionDate() == null
            ? LocalDate.now()
            : transaction.getTransactionDate();
    LocalTime transactionTime =
        transaction.getTransactionTime() == null
            ? LocalTime.MIDNIGHT
            : transaction.getTransactionTime();
    LocalDateTime now = LocalDateTime.now();
    BigDecimal amount = zeroIfNull(transaction.getAmount()).setScale(2, RoundingMode.HALF_UP);

    PositionHistory history = new PositionHistory();
    history.setId(
        new PositionHistoryId(
            portfolio.getAccountNo(),
            portfolio.getPortfolioId(),
            transactionDate,
            transactionTime));
    history.setTransType(transaction.getTransactionType().getCode());
    history.setSecurityId(transaction.getInvestmentId());
    history.setQuantity(scale(transaction.getQuantity(), 3));
    history.setPrice(scale(transaction.getPrice(), 3));
    history.setAmount(amount);
    history.setFees(BigDecimal.ZERO.setScale(2));
    history.setTotalAmount(amount);
    history.setCostBasis(
        transaction.getTransactionType() == TransactionType.BUY
            ? amount
            : BigDecimal.ZERO.setScale(2));
    history.setGainLoss(BigDecimal.ZERO.setScale(2));
    history.setProcessDate(now.toLocalDate());
    history.setProcessTime(now.toLocalTime());
    history.setProgramId("HISTLD00");
    history.setUserId("BATCH");
    history.setAuditTimestamp(now);
    return history;
  }

  private BigDecimal scale(BigDecimal value, int scale) {
    return zeroIfNull(value).setScale(scale, RoundingMode.HALF_UP);
  }

  private BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
