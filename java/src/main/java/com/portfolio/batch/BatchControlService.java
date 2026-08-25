package com.portfolio.batch;

import com.portfolio.common.FileProcessingException;
import com.portfolio.model.copybook.BatchControlConstants;
import com.portfolio.domain.BatchControl;
import com.portfolio.repository.BatchControlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Batch control file handling from HISTLD00 (1300-INIT-CHECKPOINTS,
 * 2310-UPDATE-CHECKPOINT) against the BCHCTL migration table.
 *
 * <p>Status values are from {@code src/copybook/batch/BCHCON.cpy}:
 * 'R' = ready, 'A' = active, 'W' = waiting, 'D' = done, 'E' = error.
 */
@Service
public class BatchControlService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** DB2-style 26-character timestamp, matching BCT-ATTEMPT-TS / BCT-COMPLETE-TS PIC X(26). */
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final BatchControlRepository repository;

    public BatchControlService(BatchControlRepository repository) {
        this.repository = repository;
    }

    /**
     * HISTLD00 1300-INIT-CHECKPOINTS: read the control record for the job and
     * mark it active. INVALID KEY (record not found) becomes a
     * {@link FileProcessingException} with FILE STATUS '23'.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchControl.Key markActive(String jobName, String processDate) {
        BatchControl control = find(jobName, processDate);
        control.setStatus(BatchControlConstants.STAT_ACTIVE);
        control.setStartTime(LocalTime.now().format(TIME_FMT));
        control.setAttemptTimestamp(LocalDateTime.now().format(TS_FMT));
        control.setRestartCount(control.getRestartCount() + 1);
        repository.save(control);
        return control.getKey();
    }

    /** HISTLD00 2310-UPDATE-CHECKPOINT: persist read/written counters. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCheckpoint(String jobName, String processDate,
                                 long recordsRead, long recordsWritten) {
        BatchControl control = find(jobName, processDate);
        control.setRecordsRead(recordsRead);
        control.setRecordsWritten(recordsWritten);
        repository.save(control);
    }

    /** Final control update at job end: status, counters, and return code. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markComplete(String jobName, String processDate,
                             long recordsRead, long recordsWritten, int returnCode,
                             boolean jobFailed) {
        BatchControl control = find(jobName, processDate);
        control.setStatus(jobFailed || returnCode > HistoryLoadStats.MAX_ERRORS
                ? BatchControlConstants.STAT_ERROR
                : BatchControlConstants.STAT_DONE);
        control.setRecordsRead(recordsRead);
        control.setRecordsWritten(recordsWritten);
        control.setReturnCode(returnCode);
        control.setEndTime(LocalTime.now().format(TIME_FMT));
        control.setCompleteTimestamp(LocalDateTime.now().format(TS_FMT));
        repository.save(control);
    }

    private BatchControl find(String jobName, String processDate) {
        return repository.findById(new BatchControl.Key(jobName, processDate, 1))
                .orElseThrow(() -> new FileProcessingException(
                        "Control record not found for job " + jobName
                                + " date " + processDate, "23"));
    }
}
