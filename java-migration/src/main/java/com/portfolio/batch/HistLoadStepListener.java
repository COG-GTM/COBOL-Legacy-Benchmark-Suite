package com.portfolio.batch;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class HistLoadStepListener implements StepExecutionListener {
  private final DuplicateSkippingHistoryWriter historyWriter;

  public HistLoadStepListener(DuplicateSkippingHistoryWriter historyWriter) {
    this.historyWriter = historyWriter;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    historyWriter.resetDuplicateCount();
    stepExecution.getExecutionContext().putInt("recordsRead", 0);
    stepExecution.getExecutionContext().putInt("recordsWritten", 0);
    stepExecution.getExecutionContext().putInt("duplicates", 0);
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    int duplicates = historyWriter.getDuplicateCount();
    stepExecution.getExecutionContext().putInt("recordsRead", (int) stepExecution.getReadCount());
    stepExecution
        .getExecutionContext()
        .putInt("recordsWritten", Math.max(0, (int) stepExecution.getWriteCount() - duplicates));
    stepExecution.setWriteCount(Math.max(0, stepExecution.getWriteCount() - duplicates));
    stepExecution.getExecutionContext().putInt("returnCode", duplicates > 0 ? 4 : 0);
    return duplicates > 0 ? new ExitStatus("COMPLETED", "RC=4") : ExitStatus.COMPLETED;
  }
}
