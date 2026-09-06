package com.cog.portfolio.batch;

import com.cog.portfolio.common.ReturnCode;
import com.cog.portfolio.domain.ReturnCodeLog;
import com.cog.portfolio.repository.ReturnCodeLogRepository;
import java.time.LocalDateTime;
import java.util.function.LongSupplier;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * RTNCDE00.cbl: derives the program return code from the step outcome and
 * writes an RTNCODES row. Skipped/soft-failed items give RC=4 (warning), a
 * failed step gives RC=12; the {@link ExitStatus} carries the code to the
 * {@link ReturnCodeDecider}.
 */
public class ReturnCodeStepListener implements StepExecutionListener {

    public static final String RC_KEY = "returnCode";

    private final String programId;
    private final ReturnCodeLogRepository repository;
    private final LongSupplier softFailures;

    public ReturnCodeStepListener(String programId, ReturnCodeLogRepository repository, LongSupplier softFailures) {
        this.programId = programId;
        this.repository = repository;
        this.softFailures = softFailures;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ReturnCode rc;
        if (stepExecution.getStatus().isUnsuccessful()) {
            rc = ReturnCode.SEVERE;
        } else if (softFailures.getAsLong() > 0 || stepExecution.getSkipCount() > 0) {
            rc = ReturnCode.WARNING;
        } else {
            rc = ReturnCode.SUCCESS;
        }
        stepExecution.getExecutionContext().putInt(RC_KEY, rc.code());
        repository.save(new ReturnCodeLog(new ReturnCodeLog.Key(LocalDateTime.now(), programId), rc.code(),
                rc.code(), statusCode(rc), stepExecution.getStepName() + " read=" + stepExecution.getReadCount()
                + " written=" + stepExecution.getWriteCount() + " skipped=" + stepExecution.getSkipCount()));
        return rc.toExitStatus();
    }

    /** RTNCODES.STATUS_CODE (S/W/E/F) as grouped by RTNANA00. */
    static String statusCode(ReturnCode rc) {
        return switch (rc) {
            case SUCCESS -> "S";
            case WARNING -> "W";
            case ERROR -> "E";
            case SEVERE, CRITICAL -> "F";
        };
    }
}
