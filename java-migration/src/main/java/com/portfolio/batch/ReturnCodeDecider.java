package com.portfolio.batch;

import com.portfolio.common.BatchConstants;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

public class ReturnCodeDecider implements JobExecutionDecider {
  @Override
  public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
    if (stepExecution != null && stepExecution.getStatus() != BatchStatus.COMPLETED) {
      return new FlowExecutionStatus("WARNING");
    }
    int returnCode = jobExecution.getExecutionContext().getInt("returnCode", 0);
    return returnCode > BatchConstants.MAX_DEP_RC
        ? new FlowExecutionStatus("ERROR")
        : new FlowExecutionStatus("CONTINUE");
  }
}
