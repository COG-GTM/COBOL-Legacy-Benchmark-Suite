package com.cog.portfolio.batch;

import com.cog.portfolio.common.ReturnCode;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

/**
 * PRCSEQ00 / BCHCTL00 sequencing rule: the daily flow proceeds to the next
 * program only while the previous return code is {@code <= 4}.
 */
public class ReturnCodeDecider implements JobExecutionDecider {

    public static final FlowExecutionStatus CONTINUE = new FlowExecutionStatus("CONTINUE");
    public static final FlowExecutionStatus STOP = new FlowExecutionStatus("STOP");

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        if (stepExecution == null || stepExecution.getStatus().isUnsuccessful()) {
            return STOP;
        }
        int rc = stepExecution.getExecutionContext().getInt(ReturnCodeStepListener.RC_KEY, ReturnCode.SUCCESS.code());
        return rc <= ReturnCode.MAX_CONTINUE_CODE ? CONTINUE : STOP;
    }
}
