package com.cobolbenchmark.batch;

import com.cobolbenchmark.common.BatchConstants;
import com.cobolbenchmark.common.RecordNotFoundException;
import com.cobolbenchmark.db.BatchControlRepository;
import com.cobolbenchmark.model.BatchControlKey;
import com.cobolbenchmark.model.BatchControlRecord;
import com.cobolbenchmark.model.BatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Batch Control Service - migrated from BCHCTL00.cbl.
 * Operations: INIT, CHEK, UPDT, TERM.
 * Replaces MOVE LS-RETURN-CODE TO RETURN-CODE / GOBACK with ExitStatus.
 */
@Service
@Transactional
public class BatchControlService {

    private static final Logger logger = LoggerFactory.getLogger(BatchControlService.class);
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private final BatchControlRepository batchControlRepository;

    public BatchControlService(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    /**
     * INIT function - initialize batch control record.
     * From BCHCTL00.cbl: P200-INIT-CONTROL paragraph.
     */
    public BatchControlRecord initializeControl(String jobName, String processDate, int sequenceNo) {
        logger.info("Initializing batch control: job={} date={} seq={}", jobName, processDate, sequenceNo);

        BatchControlKey key = new BatchControlKey(jobName, processDate, sequenceNo);
        BatchControlRecord record = batchControlRepository.findById(key).orElse(null);

        if (record == null) {
            record = new BatchControlRecord();
            record.setJobName(jobName);
            record.setProcessDate(processDate);
            record.setSequenceNo(sequenceNo);
        }

        record.setStatus(BatchStatus.ACTIVE.getCode());
        record.setStartTime(LocalDateTime.now().format(TS_FORMAT));
        record.setReturnCode(0);
        record.setRecordsRead(0);
        record.setRecordsWritten(0);
        record.setErrorCount(0);

        return batchControlRepository.save(record);
    }

    /**
     * CHEK function - check batch control status.
     * From BCHCTL00.cbl: P300-CHECK-STATUS paragraph.
     */
    public BatchControlRecord checkStatus(String jobName, String processDate, int sequenceNo) {
        BatchControlKey key = new BatchControlKey(jobName, processDate, sequenceNo);
        return batchControlRepository.findById(key)
                .orElseThrow(() -> new RecordNotFoundException("BatchControl",
                        jobName + "/" + processDate + "/" + sequenceNo));
    }

    /**
     * UPDT function - update batch control record.
     * From BCHCTL00.cbl: P400-UPDATE-CONTROL paragraph.
     */
    public BatchControlRecord updateControl(BatchControlRecord record) {
        logger.debug("Updating batch control: job={} reads={} writes={} errors={}",
                record.getJobName(), record.getRecordsRead(), record.getRecordsWritten(), record.getErrorCount());
        record.setAttemptTs(LocalDateTime.now().format(TS_FORMAT));
        return batchControlRepository.save(record);
    }

    /**
     * TERM function - terminate batch control.
     * From BCHCTL00.cbl: P500-TERMINATE paragraph.
     * Replace RETURN-CODE with Spring Batch ExitStatus.
     */
    public ExitStatus terminateControl(String jobName, String processDate, int sequenceNo, int returnCode) {
        logger.info("Terminating batch control: job={} rc={}", jobName, returnCode);

        BatchControlKey key = new BatchControlKey(jobName, processDate, sequenceNo);
        BatchControlRecord record = batchControlRepository.findById(key)
                .orElseThrow(() -> new RecordNotFoundException("BatchControl",
                        jobName + "/" + processDate + "/" + sequenceNo));

        record.setReturnCode(returnCode);
        record.setEndTime(LocalDateTime.now().format(TS_FORMAT));

        // Determine final status based on return code
        BatchConstants.ReturnCode classification = BatchConstants.ReturnCode.classify(returnCode);
        switch (classification) {
            case SUCCESS:
            case WARNING:
                record.setStatus(BatchStatus.DONE.getCode());
                break;
            default:
                record.setStatus(BatchStatus.ERROR.getCode());
                break;
        }

        batchControlRepository.save(record);

        // Map return code to Spring Batch ExitStatus
        return mapToExitStatus(returnCode);
    }

    /**
     * Find all active batch jobs.
     */
    public List<BatchControlRecord> findActiveJobs() {
        return batchControlRepository.findByStatus(BatchStatus.ACTIVE.getCode());
    }

    /**
     * Map COBOL return code to Spring Batch ExitStatus.
     */
    private ExitStatus mapToExitStatus(int returnCode) {
        if (returnCode == 0) {
            return ExitStatus.COMPLETED;
        } else if (returnCode <= BatchConstants.RC_WARNING) {
            return new ExitStatus("COMPLETED_WITH_WARNINGS");
        } else {
            return ExitStatus.FAILED;
        }
    }
}
