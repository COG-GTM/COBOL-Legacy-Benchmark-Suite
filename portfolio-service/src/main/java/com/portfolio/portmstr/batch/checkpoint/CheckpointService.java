package com.portfolio.portmstr.batch.checkpoint;

import com.portfolio.portmstr.model.BatchCheckpoint;
import com.portfolio.portmstr.model.enums.CheckpointStatus;
import com.portfolio.portmstr.repository.BatchCheckpointRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkpoint/Restart service.
 * Direct translation of COBOL CKPRST.cbl program and CKPRST.cpy copybook.
 *
 * Replaces the mainframe checkpoint/restart mechanism that allows
 * failed batch jobs to resume from the last committed checkpoint.
 *
 * COBOL procedure mapping:
 *   PROC-INIT              -> initializeCheckpoint()
 *   PROC-TAKE-CHECKPOINT   -> takeCheckpoint()
 *   PROC-COMMIT-CHECKPOINT -> commitCheckpoint()
 *   PROC-RESTART           -> getRestartInfo()
 */
@Service
public class CheckpointService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointService.class);

    private final BatchCheckpointRepository checkpointRepository;

    @Value("${portfolio.batch.commit-frequency:1000}")
    private int commitFrequency;

    @Value("${portfolio.batch.max-errors:100}")
    private int maxErrors;

    @Value("${portfolio.batch.max-restarts:3}")
    private int maxRestarts;

    public CheckpointService(BatchCheckpointRepository checkpointRepository) {
        this.checkpointRepository = checkpointRepository;
    }

    /**
     * Initialize checkpoint for a new batch run.
     * From COBOL PROC-INIT: Sets up initial checkpoint state.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchCheckpoint initializeCheckpoint(String programId) {
        LocalDate today = LocalDate.now();

        Optional<BatchCheckpoint> existing = checkpointRepository
                .findByProgramIdAndRunDate(programId, today);

        if (existing.isPresent()) {
            BatchCheckpoint checkpoint = existing.get();
            if (checkpoint.getStatus() == CheckpointStatus.FAILED ||
                    checkpoint.getStatus() == CheckpointStatus.ACTIVE) {
                // Restart scenario
                int restartCount = checkpoint.getRestartCount() != null ? checkpoint.getRestartCount() : 0;
                if (restartCount >= maxRestarts) {
                    log.error("Maximum restarts ({}) exceeded for program {} on {}",
                            maxRestarts, programId, today);
                    checkpoint.setStatus(CheckpointStatus.FAILED);
                    return checkpointRepository.save(checkpoint);
                }
                checkpoint.setRestartCount(restartCount + 1);
                checkpoint.setStatus(CheckpointStatus.RESTARTED);
                checkpoint.setLastCheckpointTime(LocalDateTime.now());
                log.info("Restarting checkpoint for {} (attempt {})", programId, restartCount + 1);
                return checkpointRepository.save(checkpoint);
            }
            return checkpoint;
        }

        BatchCheckpoint checkpoint = new BatchCheckpoint();
        checkpoint.setProgramId(programId);
        checkpoint.setRunDate(today);
        checkpoint.setRunTime(LocalTime.now());
        checkpoint.setStatus(CheckpointStatus.INITIAL);
        checkpoint.setRecordsRead(0L);
        checkpoint.setRecordsProcessed(0L);
        checkpoint.setRecordsError(0L);
        checkpoint.setRestartCount(0);
        checkpoint.setPhase("00");
        checkpoint.setCommitFrequency(commitFrequency);
        checkpoint.setMaxErrors(maxErrors);
        checkpoint.setMaxRestarts(maxRestarts);
        checkpoint.setRestartMode('N');
        checkpoint.setLastCheckpointTime(LocalDateTime.now());

        log.info("Initialized checkpoint for program {} on {}", programId, today);
        return checkpointRepository.save(checkpoint);
    }

    /**
     * Take a checkpoint (save current processing state).
     * From COBOL PROC-TAKE-CHECKPOINT.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchCheckpoint takeCheckpoint(String programId, String lastKey,
                                          long recordsRead, long recordsProcessed,
                                          long recordsError, String phase) {
        LocalDate today = LocalDate.now();
        BatchCheckpoint checkpoint = checkpointRepository
                .findByProgramIdAndRunDate(programId, today)
                .orElseThrow(() -> new IllegalStateException(
                        "No checkpoint found for " + programId + " on " + today));

        checkpoint.setStatus(CheckpointStatus.ACTIVE);
        checkpoint.setLastKey(lastKey);
        checkpoint.setRecordsRead(recordsRead);
        checkpoint.setRecordsProcessed(recordsProcessed);
        checkpoint.setRecordsError(recordsError);
        checkpoint.setPhase(phase);
        checkpoint.setLastCheckpointTime(LocalDateTime.now());

        log.debug("Checkpoint taken for {}: key={}, read={}, processed={}, errors={}",
                programId, lastKey, recordsRead, recordsProcessed, recordsError);
        return checkpointRepository.save(checkpoint);
    }

    /**
     * Mark checkpoint as complete.
     * From COBOL PROC-COMMIT-CHECKPOINT.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchCheckpoint completeCheckpoint(String programId) {
        LocalDate today = LocalDate.now();
        BatchCheckpoint checkpoint = checkpointRepository
                .findByProgramIdAndRunDate(programId, today)
                .orElseThrow(() -> new IllegalStateException(
                        "No checkpoint found for " + programId + " on " + today));

        checkpoint.setStatus(CheckpointStatus.COMPLETE);
        checkpoint.setPhase("40");
        checkpoint.setLastCheckpointTime(LocalDateTime.now());

        log.info("Checkpoint completed for {}: read={}, processed={}, errors={}",
                programId, checkpoint.getRecordsRead(),
                checkpoint.getRecordsProcessed(), checkpoint.getRecordsError());
        return checkpointRepository.save(checkpoint);
    }

    /**
     * Mark checkpoint as failed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchCheckpoint failCheckpoint(String programId, String errorInfo) {
        LocalDate today = LocalDate.now();
        Optional<BatchCheckpoint> checkpointOpt = checkpointRepository
                .findByProgramIdAndRunDate(programId, today);

        if (checkpointOpt.isPresent()) {
            BatchCheckpoint checkpoint = checkpointOpt.get();
            checkpoint.setStatus(CheckpointStatus.FAILED);
            checkpoint.setLastCheckpointTime(LocalDateTime.now());
            log.error("Checkpoint failed for {}: {}", programId, errorInfo);
            return checkpointRepository.save(checkpoint);
        }

        log.error("No checkpoint to fail for {} on {}", programId, today);
        return null;
    }

    /**
     * Get restart information.
     * From COBOL PROC-RESTART: Retrieves last checkpoint for restart.
     */
    @Transactional(readOnly = true)
    public Optional<BatchCheckpoint> getRestartInfo(String programId) {
        return checkpointRepository.findTopByProgramIdOrderByRunDateDesc(programId)
                .filter(cp -> cp.getStatus() == CheckpointStatus.RESTARTED ||
                        cp.getStatus() == CheckpointStatus.ACTIVE);
    }

    /**
     * Check if processing should continue based on error limits.
     * From COBOL CK-MAX-ERRORS threshold check.
     */
    public boolean shouldContinueProcessing(long errorCount) {
        return errorCount < maxErrors;
    }

    /**
     * Check if a checkpoint should be taken based on commit frequency.
     * From COBOL CK-COMMIT-FREQ threshold.
     */
    public boolean shouldTakeCheckpoint(long recordsProcessed) {
        return recordsProcessed > 0 && recordsProcessed % commitFrequency == 0;
    }
}
