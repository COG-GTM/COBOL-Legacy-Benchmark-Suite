package com.portfolio.batch.listeners;

import com.portfolio.model.entity.CheckpointControl;
import com.portfolio.model.enums.CheckpointPhase;
import com.portfolio.model.enums.CheckpointStatus;
import com.portfolio.repository.CheckpointControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CheckpointRestartListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(CheckpointRestartListener.class);

    private final CheckpointControlRepository checkpointControlRepository;

    public CheckpointRestartListener(CheckpointControlRepository checkpointControlRepository) {
        this.checkpointControlRepository = checkpointControlRepository;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String programId = stepExecution.getJobExecution().getJobInstance().getJobName();
        String runDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        checkpointControlRepository.findByProgramIdAndRunDate(programId, runDate)
                .ifPresent(checkpoint -> {
                    if (checkpoint.getStatus() == CheckpointStatus.FAILED.getCode()) {
                        log.info("Restarting from checkpoint: lastKey={}, recordsProcessed={}",
                                checkpoint.getLastKey(), checkpoint.getRecordsProcessed());
                        checkpoint.setStatus(CheckpointStatus.RESTARTED.getCode());
                        checkpoint.setRestartCount(
                                checkpoint.getRestartCount() != null ? checkpoint.getRestartCount() + 1 : 1);
                        checkpointControlRepository.save(checkpoint);
                    }
                });
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String programId = stepExecution.getJobExecution().getJobInstance().getJobName();
        String runDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        CheckpointControl checkpoint = checkpointControlRepository
                .findByProgramIdAndRunDate(programId, runDate)
                .orElseGet(() -> {
                    CheckpointControl newCp = new CheckpointControl();
                    newCp.setProgramId(programId);
                    newCp.setRunDate(runDate);
                    return newCp;
                });

        checkpoint.setRecordsRead(stepExecution.getReadCount());
        checkpoint.setRecordsProcessed(stepExecution.getWriteCount());
        checkpoint.setRecordsError(stepExecution.getSkipCount());
        checkpoint.setLastTime(LocalDateTime.now());
        checkpoint.setPhase(CheckpointPhase.TERMINATE.getCode());

        if (stepExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            checkpoint.setStatus(CheckpointStatus.COMPLETE.getCode());
        } else {
            checkpoint.setStatus(CheckpointStatus.FAILED.getCode());
        }

        checkpointControlRepository.save(checkpoint);

        log.info("Checkpoint saved: read={}, processed={}, errors={}",
                checkpoint.getRecordsRead(), checkpoint.getRecordsProcessed(),
                checkpoint.getRecordsError());

        return stepExecution.getExitStatus();
    }
}
