package com.clbs.portfolio.service;

import com.clbs.portfolio.model.CheckpointControl;
import com.clbs.portfolio.model.CheckpointControl.CheckpointPhase;
import com.clbs.portfolio.model.CheckpointControl.CheckpointStatus;
import com.clbs.portfolio.repository.CheckpointControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Checkpoint/restart service wrapping Spring Batch's restart mechanism.
 * Maps to COBOL CKPRST.cpy concepts and checkpoint processing routines
 * (CKPINIT, CKPTAKE, CKPCMIT, CKPRSTR).
 */
@Service
public class CheckpointService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointService.class);

    private final CheckpointControlRepository checkpointControlRepository;

    public CheckpointService(CheckpointControlRepository checkpointControlRepository) {
        this.checkpointControlRepository = checkpointControlRepository;
    }

    /**
     * Initialize a checkpoint record (maps to CKPINIT).
     */
    @Transactional
    public CheckpointControl initializeCheckpoint(String programId, String runDate) {
        log.info("Initializing checkpoint for program={}, runDate={}", programId, runDate);

        Optional<CheckpointControl> existing = checkpointControlRepository
                .findByProgramIdAndRunDate(programId, runDate);

        if (existing.isPresent()) {
            CheckpointControl ck = existing.get();
            if (ck.getStatus() == CheckpointStatus.FAILED
                    && ck.getRestartCount() < ck.getMaxRestarts()) {
                ck.setStatus(CheckpointStatus.RESTARTED);
                ck.setRestartCount(ck.getRestartCount() + 1);
                ck.setRestartMode("R");
                ck.setLastTime(LocalDateTime.now());
                return checkpointControlRepository.save(ck);
            }
            return ck;
        }

        CheckpointControl checkpoint = CheckpointControl.builder()
                .programId(programId)
                .runDate(runDate)
                .runTime(LocalDateTime.now().toLocalTime().toString().replace(":", "").substring(0, 6))
                .status(CheckpointStatus.INITIAL)
                .phase(CheckpointPhase.INIT)
                .recordsRead(0L)
                .recordsProcessed(0L)
                .recordsError(0L)
                .restartCount(0)
                .restartMode("N")
                .lastTime(LocalDateTime.now())
                .build();

        return checkpointControlRepository.save(checkpoint);
    }

    /**
     * Take a checkpoint — record current processing position (maps to CKPTAKE).
     */
    @Transactional
    public void takeCheckpoint(String programId, String runDate, String lastKey,
                               CheckpointPhase phase, long recordsRead,
                               long recordsProcessed, long recordsError) {
        CheckpointControl checkpoint = checkpointControlRepository
                .findByProgramIdAndRunDate(programId, runDate)
                .orElseThrow(() -> new IllegalStateException(
                        "No checkpoint found for program=" + programId + ", runDate=" + runDate));

        checkpoint.setLastKey(lastKey);
        checkpoint.setPhase(phase);
        checkpoint.setRecordsRead(recordsRead);
        checkpoint.setRecordsProcessed(recordsProcessed);
        checkpoint.setRecordsError(recordsError);
        checkpoint.setLastTime(LocalDateTime.now());
        checkpoint.setStatus(CheckpointStatus.ACTIVE);

        checkpointControlRepository.save(checkpoint);
        log.debug("Checkpoint taken: program={}, key={}, phase={}", programId, lastKey, phase);
    }

    /**
     * Complete a checkpoint — mark processing as done (maps to CKPCMIT).
     */
    @Transactional
    public void completeCheckpoint(String programId, String runDate) {
        CheckpointControl checkpoint = checkpointControlRepository
                .findByProgramIdAndRunDate(programId, runDate)
                .orElseThrow(() -> new IllegalStateException(
                        "No checkpoint found for program=" + programId + ", runDate=" + runDate));

        checkpoint.setStatus(CheckpointStatus.COMPLETE);
        checkpoint.setPhase(CheckpointPhase.TERMINATE);
        checkpoint.setLastTime(LocalDateTime.now());

        checkpointControlRepository.save(checkpoint);
        log.info("Checkpoint completed: program={}", programId);
    }

    /**
     * Mark a checkpoint as failed (maps to error path in CKPRSTR).
     */
    @Transactional
    public void failCheckpoint(String programId, String runDate) {
        checkpointControlRepository.findByProgramIdAndRunDate(programId, runDate)
                .ifPresent(checkpoint -> {
                    checkpoint.setStatus(CheckpointStatus.FAILED);
                    checkpoint.setLastTime(LocalDateTime.now());
                    checkpointControlRepository.save(checkpoint);
                    log.error("Checkpoint failed: program={}", programId);
                });
    }

    /**
     * Check if a restart is possible (maps to CKPRSTR).
     */
    public boolean canRestart(String programId, String runDate) {
        return checkpointControlRepository.findByProgramIdAndRunDate(programId, runDate)
                .map(ck -> ck.getStatus() == CheckpointStatus.FAILED
                        && ck.getRestartCount() < ck.getMaxRestarts())
                .orElse(false);
    }

    /**
     * Get the last processed key for restart positioning.
     */
    public Optional<String> getRestartKey(String programId, String runDate) {
        return checkpointControlRepository.findByProgramIdAndRunDate(programId, runDate)
                .map(CheckpointControl::getLastKey);
    }
}
