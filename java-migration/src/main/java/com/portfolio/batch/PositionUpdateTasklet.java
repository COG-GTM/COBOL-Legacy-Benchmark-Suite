package com.portfolio.batch;

// PROVISIONAL – requiere validación humana: POSUPD00 sin fuente COBOL original

import com.portfolio.domain.InvestmentPosition;
import com.portfolio.domain.InvestmentPositionId;
import com.portfolio.domain.PositionStatus;
import com.portfolio.domain.Transaction;
import com.portfolio.domain.TransactionStatus;
import com.portfolio.domain.TransactionType;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.service.BusinessException;
import com.portfolio.service.PortfolioTransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class PositionUpdateTasklet implements Tasklet {
  private final TransactionRepository transactionRepository;
  private final InvestmentPositionRepository positionRepository;
  private final PortfolioTransactionService portfolioTransactionService;

  public PositionUpdateTasklet(
      TransactionRepository transactionRepository,
      InvestmentPositionRepository positionRepository,
      PortfolioTransactionService portfolioTransactionService) {
    this.transactionRepository = transactionRepository;
    this.positionRepository = positionRepository;
    this.portfolioTransactionService = portfolioTransactionService;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    int recordsRead = 0;
    int errors = 0;
    int returnCode = 0;
    List<Transaction> transactions = transactionRepository.findByStatus(TransactionStatus.PENDING);
    for (Transaction transaction : transactions) {
      recordsRead++;
      contribution.incrementReadCount();
      try {
        portfolioTransactionService.process(transaction);
        upsertPosition(transaction);
        contribution.incrementWriteCount(1);
      } catch (BusinessException | UnsupportedOperationException exception) {
        errors++;
        returnCode = Math.max(returnCode, 4);
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
        contribution.incrementWriteCount(1);
      } catch (Exception exception) {
        errors++;
        returnCode = Math.max(returnCode, 8);
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
        contribution.incrementWriteCount(1);
      }
    }
    var executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
    executionContext.putInt("recordsRead", recordsRead);
    executionContext.putInt("recordsWritten", recordsRead - errors);
    executionContext.putInt("errors", errors);
    executionContext.putInt("returnCode", returnCode);
    contribution.setExitStatus(new ExitStatus("COMPLETED", "RC=" + returnCode));
    return RepeatStatus.FINISHED;
  }

  private void upsertPosition(Transaction transaction) {
    LocalDate positionDate =
        transaction.getTransactionDate() == null
            ? LocalDate.now()
            : transaction.getTransactionDate();
    InvestmentPositionId positionId =
        new InvestmentPositionId(
            transaction.getPortfolioId(), transaction.getInvestmentId(), positionDate);
    Optional<InvestmentPosition> existing = positionRepository.findById(positionId);
    if (existing.isEmpty() && transaction.getTransactionType() == TransactionType.FEE) {
      return;
    }
    InvestmentPosition position = existing.orElseGet(() -> newPosition(positionId, transaction));
    BigDecimal quantity = zeroIfNull(position.getQuantity());
    BigDecimal costBasis = zeroIfNull(position.getCostBasis());
    BigDecimal transactionQuantity = zeroIfNull(transaction.getQuantity());
    BigDecimal amount = zeroIfNull(transaction.getAmount());
    switch (transaction.getTransactionType()) {
      case BUY -> {
        quantity = quantity.add(transactionQuantity);
        costBasis = costBasis.add(amount);
      }
      case SELL -> {
        quantity = quantity.subtract(transactionQuantity);
        costBasis = costBasis.subtract(amount);
      }
      case FEE -> costBasis = costBasis.subtract(amount);
      case TRANSFER ->
          throw new UnsupportedOperationException("Transfer processing not implemented");
    }
    position.setQuantity(quantity.setScale(4, RoundingMode.HALF_UP));
    position.setCostBasis(costBasis.setScale(2, RoundingMode.HALF_UP));
    position.setMarketValue(
        position
            .getQuantity()
            .multiply(zeroIfNull(transaction.getPrice()))
            .setScale(2, RoundingMode.HALF_UP));
    position.setLastMaintDate(LocalDateTime.now());
    position.setLastMaintUser("BATCH");
    positionRepository.save(position);
  }

  private InvestmentPosition newPosition(InvestmentPositionId positionId, Transaction transaction) {
    InvestmentPosition position = new InvestmentPosition();
    position.setId(positionId);
    position.setQuantity(BigDecimal.ZERO.setScale(4));
    position.setCostBasis(BigDecimal.ZERO.setScale(2));
    position.setMarketValue(BigDecimal.ZERO.setScale(2));
    position.setCurrencyCode(transaction.getCurrencyCode());
    position.setStatus(PositionStatus.ACTIVE);
    return position;
  }

  private BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
