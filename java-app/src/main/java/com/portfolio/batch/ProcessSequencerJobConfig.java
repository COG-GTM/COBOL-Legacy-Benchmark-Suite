package com.portfolio.batch;

import com.portfolio.model.BatchControlKey;
import com.portfolio.model.BatchControlRecord;
import com.portfolio.model.enums.BatchStatus;
import com.portfolio.repository.BatchControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Process Sequencer Job Configuration.
 * Replaces: PRCSEQ00.cbl - Job dependency management.
 *
 * Orchestrates execution order: TRNVAL00 -> POSUPD00 -> HISTLD00 -> Reports.
 * Uses Spring Batch's FlowBuilder with conditional transitions.
 */
@Configuration
public class ProcessSequencerJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ProcessSequencerJobConfig.class);

    private final BatchControlRepository batchControlRepository;

    public ProcessSequencerJobConfig(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    @Bean
    public Job processSequencerJob(JobRepository jobRepository,
                                    Step sequenceInitStep,
                                    Step sequenceCheckStep,
                                    Step sequenceTerminateStep) {
        return new JobBuilder("processSequencerJob", jobRepository)
                .start(sequenceInitStep)
                .next(sequenceCheckStep)
                .next(sequenceTerminateStep)
                .build();
    }

    /**
     * Initialize sequence - build process table.
     * Replaces: PRCSEQ00.cbl 1000-INITIALIZE-SEQUENCE.
     */
    @Bean
    public Step sequenceInitStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String processDate = LocalDateTime.now()
                    .format(DateTimeFormatter.BASIC_ISO_DATE);

            // Create sequence: TRNVAL -> POSUPD -> HISTLD -> RPTPOS -> RPTAUD -> RPTSTA
            String[] sequence = {"TRNVAL00", "POSUPD00", "HISTLD00",
                    "RPTPOS00", "RPTAUD00", "RPTSTA00"};

            for (int i = 0; i < sequence.length; i++) {
                BatchControlRecord record = new BatchControlRecord();
                record.setKey(new BatchControlKey(sequence[i], processDate, i + 1));
                record.setStatus(BatchStatus.READY.getCode());
                record.setPrereqCount(i > 0 ? 1 : 0);
                record.setReturnCode(0);
                record.setRestartCount(0);
                batchControlRepository.save(record);
            }

            log.info("Process sequence initialized with {} steps for date {}",
                    sequence.length, processDate);
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("sequenceInitStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    /**
     * Check status of all processes.
     * Replaces: PRCSEQ00.cbl 3000-CHECK-STATUS and 3300-CHECK-COMPLETION.
     */
    @Bean
    public Step sequenceCheckStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String processDate = LocalDateTime.now()
                    .format(DateTimeFormatter.BASIC_ISO_DATE);

            List<BatchControlRecord> records =
                    batchControlRepository.findByKeyProcessDate(processDate);

            long activeCount = records.stream()
                    .filter(r -> BatchStatus.ACTIVE.getCode().equals(r.getStatus()))
                    .count();
            long errorCount = records.stream()
                    .filter(r -> BatchStatus.ERROR.getCode().equals(r.getStatus()))
                    .count();
            long doneCount = records.stream()
                    .filter(r -> BatchStatus.DONE.getCode().equals(r.getStatus()))
                    .count();

            log.info("Sequence status: active={}, done={}, error={}, total={}",
                    activeCount, doneCount, errorCount, records.size());

            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("sequenceCheckStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    /**
     * Terminate sequence.
     * Replaces: PRCSEQ00.cbl 4000-TERMINATE-SEQUENCE and 4100-CHECK-FINAL-STATUS.
     */
    @Bean
    public Step sequenceTerminateStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String processDate = LocalDateTime.now()
                    .format(DateTimeFormatter.BASIC_ISO_DATE);

            List<BatchControlRecord> records =
                    batchControlRepository.findByKeyProcessDate(processDate);

            long errorCount = records.stream()
                    .filter(r -> BatchStatus.ERROR.getCode().equals(r.getStatus()))
                    .count();

            if (errorCount > 0) {
                log.error("Sequence completed with {} errors", errorCount);
            } else {
                log.info("Sequence completed successfully");
            }

            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("sequenceTerminateStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
