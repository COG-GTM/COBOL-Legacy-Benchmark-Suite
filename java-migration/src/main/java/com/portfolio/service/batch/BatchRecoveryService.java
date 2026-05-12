package com.portfolio.service.batch;

import com.portfolio.model.entity.CheckpointControl;
import com.portfolio.repository.CheckpointControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BatchRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(BatchRecoveryService.class);

    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final CheckpointControlRepository checkpointControlRepository;

    public BatchRecoveryService(JobExplorer jobExplorer, JobOperator jobOperator,
                                CheckpointControlRepository checkpointControlRepository) {
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
        this.checkpointControlRepository = checkpointControlRepository;
    }

    public Optional<String> getLastCheckpointKey(String programId) {
        return checkpointControlRepository
                .findTopByProgramIdOrderByLastTimeDesc(programId)
                .map(CheckpointControl::getLastKey);
    }

    public void restartJob(String jobName) throws Exception {
        var executions = jobExplorer.findRunningJobExecutions(jobName);
        if (executions.isEmpty()) {
            log.info("No running executions found for job: {}", jobName);
            return;
        }

        for (JobExecution execution : executions) {
            if (execution.isRunning()) {
                log.info("Restarting job execution: {}", execution.getId());
                jobOperator.restart(execution.getId());
            }
        }
    }
}
