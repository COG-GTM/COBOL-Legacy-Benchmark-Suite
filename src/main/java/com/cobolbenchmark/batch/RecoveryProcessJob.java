package com.cobolbenchmark.batch;

import com.cobolbenchmark.common.BatchConstants;
import com.cobolbenchmark.common.RecordNotFoundException;
import com.cobolbenchmark.db.BatchControlRepository;
import com.cobolbenchmark.model.BatchControlKey;
import com.cobolbenchmark.model.BatchControlRecord;
import com.cobolbenchmark.model.BatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recovery Process Job - migrated from RCVPRC00.cbl.
 * Operations: INIT, RECV, TERM.
 * Recovery actions: WS-ACTION-RESTART, WS-ACTION-BYPASS, WS-ACTION-TERMINATE.
 * Strategy pattern for restart/bypass/terminate decision logic.
 */
@Service
@Transactional
public class RecoveryProcessJob {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryProcessJob.class);

    private final BatchControlRepository batchControlRepository;

    public RecoveryProcessJob(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    /**
     * Recovery action enum - from RCVPRC00.cbl WS-ACTION values.
     */
    public enum RecoveryAction {
        RESTART,
        BYPASS,
        TERMINATE
    }

    /**
     * INIT function - initialize recovery process.
     * From RCVPRC00.cbl: P200-INIT-RECOVERY paragraph.
     */
    public BatchControlRecord initializeRecovery(String jobName, String processDate, int sequenceNo) {
        logger.info("Initializing recovery for job: {} date: {} seq: {}", jobName, processDate, sequenceNo);

        BatchControlKey key = new BatchControlKey(jobName, processDate, sequenceNo);
        return batchControlRepository.findById(key)
                .orElseThrow(() -> new RecordNotFoundException("BatchControl",
                        jobName + "/" + processDate + "/" + sequenceNo));
    }

    /**
     * RECV function - determine and execute recovery action.
     * From RCVPRC00.cbl: 2110-DETERMINE-ACTION + 2120-EXECUTE-RECOVERY paragraphs.
     * EVALUATE TRUE / WHEN WS-ACTION-RESTART / WHEN WS-ACTION-BYPASS / WHEN WS-ACTION-TERMINATE.
     */
    public RecoveryAction determineAction(BatchControlRecord record) {
        logger.info("Determining recovery action for job: {} status: {} rc: {} restarts: {}/{}",
                record.getJobName(), record.getStatus(), record.getReturnCode(),
                record.getRestartCount(), record.getMaxRestarts());

        // If job completed successfully, no recovery needed
        if (BatchStatus.DONE.getCode().equals(record.getStatus())) {
            return RecoveryAction.BYPASS;
        }

        // If max restarts exceeded, terminate
        if (record.getRestartCount() >= record.getMaxRestarts()) {
            logger.warn("Max restarts exceeded for job: {}", record.getJobName());
            return RecoveryAction.TERMINATE;
        }

        // If return code is severe/critical, terminate
        BatchConstants.ReturnCode classification = BatchConstants.ReturnCode.classify(record.getReturnCode());
        if (classification == BatchConstants.ReturnCode.SEVERE
                || classification == BatchConstants.ReturnCode.CRITICAL) {
            logger.warn("Severe/critical return code for job: {} rc: {}", record.getJobName(), record.getReturnCode());
            return RecoveryAction.TERMINATE;
        }

        // Otherwise, restart
        return RecoveryAction.RESTART;
    }

    /**
     * Execute recovery action - from RCVPRC00.cbl: 2120-EXECUTE-RECOVERY paragraph.
     */
    public int executeRecovery(BatchControlRecord record, RecoveryAction action) {
        logger.info("Executing recovery action {} for job: {}", action, record.getJobName());

        switch (action) {
            case RESTART:
                record.setRestartCount(record.getRestartCount() + 1);
                record.setStatus(BatchStatus.READY.getCode());
                record.setReturnCode(0);
                batchControlRepository.save(record);
                return BatchConstants.RC_SUCCESS;

            case BYPASS:
                record.setStatus(BatchStatus.DONE.getCode());
                batchControlRepository.save(record);
                return BatchConstants.RC_WARNING;

            case TERMINATE:
                record.setStatus(BatchStatus.ERROR.getCode());
                batchControlRepository.save(record);
                return BatchConstants.RC_ERROR;

            default:
                return BatchConstants.RC_SEVERE;
        }
    }

    /**
     * TERM function - terminate recovery process.
     * From RCVPRC00.cbl: P500-TERMINATE paragraph.
     */
    public int terminate(BatchControlRecord record) {
        logger.info("Terminating recovery process for job: {}", record.getJobName());
        RecoveryAction action = determineAction(record);
        return executeRecovery(record, action);
    }
}
