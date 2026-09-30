package com.portfolio.batch;

// PROVISIONAL – requiere validación humana: TRNVAL00 sin fuente COBOL original

import com.portfolio.config.ErrorLoggingService;
import com.portfolio.domain.ErrorSeverity;
import com.portfolio.domain.ErrorType;
import com.portfolio.domain.Transaction;
import com.portfolio.domain.TransactionStatus;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.service.BusinessException;
import com.portfolio.service.PortfolioTransactionService;
import com.portfolio.service.PortfolioValidationService;
import java.util.List;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class TransactionValidationTasklet implements Tasklet {
  private final TransactionRepository transactionRepository;
  private final PortfolioValidationService validationService;
  private final PortfolioTransactionService transactionService;
  private final ErrorLoggingService errorLoggingService;

  public TransactionValidationTasklet(
      TransactionRepository transactionRepository,
      PortfolioValidationService validationService,
      PortfolioTransactionService transactionService,
      ErrorLoggingService errorLoggingService) {
    this.transactionRepository = transactionRepository;
    this.validationService = validationService;
    this.transactionService = transactionService;
    this.errorLoggingService = errorLoggingService;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    int recordsRead = 0;
    int invalidRecords = 0;
    List<Transaction> transactions = transactionRepository.findByStatus(TransactionStatus.PENDING);
    for (Transaction transaction : transactions) {
      recordsRead++;
      contribution.incrementReadCount();
      try {
        validationService.requirePortfolioId(transaction.getPortfolioId());
        transactionService.validateInvestment(transaction);
        validationService.requireAmount(transaction.getAmount());
        transactionService.validateAmounts(transaction);
      } catch (BusinessException exception) {
        invalidRecords++;
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
        errorLoggingService.log(
            "TRNVAL00",
            ErrorType.APPLICATION,
            ErrorSeverity.WARNING,
            exception.getCode(),
            exception.getMessage(),
            transaction.getTransactionId());
        contribution.incrementWriteCount(1);
      }
    }
    int returnCode = invalidRecords > 0 ? 4 : 0;
    chunkContext
        .getStepContext()
        .getStepExecution()
        .getExecutionContext()
        .putInt("recordsRead", recordsRead);
    chunkContext
        .getStepContext()
        .getStepExecution()
        .getExecutionContext()
        .putInt("invalidRecords", invalidRecords);
    chunkContext
        .getStepContext()
        .getStepExecution()
        .getExecutionContext()
        .putInt("recordsWritten", recordsRead - invalidRecords);
    chunkContext
        .getStepContext()
        .getStepExecution()
        .getExecutionContext()
        .putInt("returnCode", returnCode);
    contribution.setExitStatus(new ExitStatus("COMPLETED", "RC=" + returnCode));
    return RepeatStatus.FINISHED;
  }
}
