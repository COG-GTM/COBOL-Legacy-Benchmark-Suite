package com.portfolio.batch;

import com.portfolio.service.ReturnCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class HistoryLoadStepListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(HistoryLoadStepListener.class);

    private final ReturnCodeService returnCodeService;

    public HistoryLoadStepListener(ReturnCodeService returnCodeService) {
        this.returnCodeService = returnCodeService;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        returnCodeService.initialize("HISTLD00");
        log.info("History load step starting");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long skipCount = stepExecution.getSkipCount();

        log.info("History load step completed - Read: {}, Written: {}, Skipped: {}",
                readCount, writeCount, skipCount);

        int returnCode = skipCount > 0 ? 4 : 0;
        returnCodeService.setCode("HISTLD00", returnCode);
        returnCodeService.logCode("HISTLD00");

        return stepExecution.getExitStatus();
    }
}
