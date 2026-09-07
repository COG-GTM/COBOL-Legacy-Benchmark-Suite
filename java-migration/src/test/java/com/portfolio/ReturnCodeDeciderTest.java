package com.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.portfolio.batch.ReturnCodeDecider;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;

class ReturnCodeDeciderTest {
  private final ReturnCodeDecider decider = new ReturnCodeDecider();

  @Test
  void zeroContinues() {
    assertEquals("CONTINUE", decide(0, BatchStatus.COMPLETED).getName());
  }

  @Test
  void warningCodeFourContinues() {
    assertEquals("CONTINUE", decide(4, BatchStatus.COMPLETED).getName());
  }

  @Test
  void errorCodeEightStopsWithError() {
    assertEquals("ERROR", decide(8, BatchStatus.COMPLETED).getName());
  }

  @Test
  void failedPreviousStepStopsWithWarning() {
    assertEquals("WARNING", decide(0, BatchStatus.FAILED).getName());
  }

  private FlowExecutionStatus decide(int returnCode, BatchStatus status) {
    JobExecution jobExecution = new JobExecution(1L);
    jobExecution.getExecutionContext().putInt("returnCode", returnCode);
    StepExecution stepExecution = new StepExecution("previous", jobExecution);
    stepExecution.setStatus(status);
    return decider.decide(jobExecution, stepExecution);
  }
}
