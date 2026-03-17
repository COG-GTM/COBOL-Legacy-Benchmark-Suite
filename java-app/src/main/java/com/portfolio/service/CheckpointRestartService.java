package com.portfolio.service;

import com.portfolio.model.BatchControlKey;
import com.portfolio.model.BatchControlRecord;
import com.portfolio.repository.BatchControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Checkpoint/Restart Service.
 * Replaces: CKPRST.cpy copybook functionality.
 * Uses Spring Batch's ExecutionContext for checkpoint data persistence.
 */
@Service
public class CheckpointRestartService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointRestartService.class);

    private final BatchControlRepository batchControlRepository;

    public CheckpointRestartService(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    /**
     * Saves a checkpoint for the current batch job.
     * Replaces CKPRST checkpoint save logic.
     */
    public void saveCheckpoint(String jobName, String processDate, int sequenceNo,
                               String stepName, String lastProcessedKey) {
        BatchControlKey key = new BatchControlKey(jobName, processDate, sequenceNo);
        Optional<BatchControlRecord> recordOpt = batchControlRepository.findById(key);

        if (recordOpt.isPresent()) {
            BatchControlRecord record = recordOpt.get();
            record.setStepName(stepName);
            record.setAttemptTs(LocalDateTime.now());
            batchControlRepository.save(record);
            log.debug("Checkpoint saved: job={}, step={}, key={}",
                    jobName, stepName, lastProcessedKey);
        } else {
            log.warn("Cannot save checkpoint - control record not found: {}/{}/{}",
                    jobName, processDate, sequenceNo);
        }
    }

    /**
     * Gets the last checkpoint for a job.
     * Replaces CKPRST checkpoint retrieval logic.
     */
    public Optional<BatchControlRecord> getLastCheckpoint(String jobName, String processDate) {
        var records = batchControlRepository.findByKeyJobNameAndKeyProcessDate(jobName, processDate);
        return records.stream()
                .filter(r -> "A".equals(r.getStatus()) || "W".equals(r.getStatus()))
                .findFirst();
    }

    /**
     * Saves checkpoint data from a Spring Batch chunk context.
     */
    public void saveCheckpointFromContext(ChunkContext chunkContext, String lastProcessedKey) {
        String jobName = chunkContext.getStepContext().getJobName();
        String stepName = chunkContext.getStepContext().getStepName();

        // Retrieve processDate from JobExecutionContext (set during INIT step)
        // to avoid midnight-crossing mismatches with the batch control record key
        org.springframework.batch.item.ExecutionContext jobCtx = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();
        String processDate = jobCtx.getString("processDate", null);
        if (processDate == null) {
            // Fallback if not running within a batch job that stores processDate
            processDate = LocalDateTime.now().toLocalDate()
                    .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        }

        saveCheckpoint(jobName, processDate, 1, stepName, lastProcessedKey);
    }
}
