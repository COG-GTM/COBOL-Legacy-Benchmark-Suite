package com.portfolio.batch;

import com.portfolio.model.BatchControlKey;
import com.portfolio.model.BatchControlRecord;
import com.portfolio.model.enums.BatchStatus;
import com.portfolio.repository.BatchControlRepository;
import com.portfolio.service.AuditService;
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
import java.util.Optional;

/**
 * Batch Control Job Configuration.
 * Replaces: BCHCTL00.cbl - The master batch controller.
 *
 * Implements as a Spring Batch Job with 4 steps:
 * 1. INIT: Initialize batch control record (set status to Active, record start time)
 * 2. CHEK: Check prerequisites by querying BatchControlRepository
 * 3. UPDT: Update batch control status during processing
 * 4. TERM: Finalize (set status to Done, record end time, set return code)
 *
 * Maps the linkage section functions (INIT/CHEK/UPDT/TERM from BCHCTL00.cbl lines 49-53).
 */
@Configuration
public class BatchControlJobConfig {

    private static final Logger log = LoggerFactory.getLogger(BatchControlJobConfig.class);

    private final BatchControlRepository batchControlRepository;
    private final AuditService auditService;

    public BatchControlJobConfig(BatchControlRepository batchControlRepository,
                                  AuditService auditService) {
        this.batchControlRepository = batchControlRepository;
        this.auditService = auditService;
    }

    @Bean
    public Job batchControlJob(JobRepository jobRepository,
                               Step initStep, Step checkPrereqStep,
                               Step updateStatusStep, Step terminateStep) {
        return new JobBuilder("batchControlJob", jobRepository)
                .start(initStep)
                .next(checkPrereqStep)
                .next(updateStatusStep)
                .next(terminateStep)
                .build();
    }

    /**
     * INIT step: Initialize batch control record.
     * Replaces: BCHCTL00.cbl 1000-PROCESS-INITIALIZE.
     */
    @Bean
    public Step initStep(JobRepository jobRepository,
                         PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            Object jobNameParam = chunkContext.getStepContext().getJobParameters().get("jobName");
            String jobName = jobNameParam != null ? jobNameParam.toString() : "DEFAULT";
            String processDate = LocalDateTime.now()
                    .format(DateTimeFormatter.BASIC_ISO_DATE);

            BatchControlRecord record = new BatchControlRecord();
            record.setKey(new BatchControlKey(jobName, processDate, 1));
            record.setStatus(BatchStatus.ACTIVE.getCode());
            record.setStartTime(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            record.setPrereqCount(0);
            record.setReturnCode(0);
            record.setRestartCount(0);
            record.setAttemptTs(LocalDateTime.now());

            batchControlRepository.save(record);
            auditService.logSystemEvent("STARTUP", "SUCC",
                    "Batch control initialized: " + jobName);

            log.info("Batch control initialized for job: {}", jobName);
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("initStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    /**
     * CHEK step: Check prerequisites.
     * Replaces: BCHCTL00.cbl 2000-CHECK-PREREQUISITES.
     * Replicates the BCT-PREREQ-JOBS OCCURS 10 TIMES logic.
     */
    @Bean
    public Step checkPrereqStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            Object jobNameParam2 = chunkContext.getStepContext().getJobParameters().get("jobName");
            String jobName = jobNameParam2 != null ? jobNameParam2.toString() : "DEFAULT";
            String processDate = LocalDateTime.now()
                    .format(DateTimeFormatter.BASIC_ISO_DATE);

            // Check if all prerequisite jobs are done
            List<BatchControlRecord> prereqs = batchControlRepository
                    .findByKeyProcessDate(processDate);

            boolean allPrereqsMet = prereqs.stream()
                    .filter(r -> !r.getKey().getJobName().equals(jobName))
                    .allMatch(r -> BatchStatus.DONE.getCode().equals(r.getStatus()));

            if (!allPrereqsMet) {
                log.warn("Prerequisites not met for job: {}", jobName);
            } else {
                log.info("All prerequisites met for job: {}", jobName);
            }

            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("checkPrereqStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    /**
     * UPDT step: Update batch control status.
     * Replaces: BCHCTL00.cbl 3000-UPDATE-STATUS.
     */
    @Bean
    public Step updateStatusStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            Object jobNameParam3 = chunkContext.getStepContext().getJobParameters().get("jobName");
            String jobName = jobNameParam3 != null ? jobNameParam3.toString() : "DEFAULT";
            String processDate = LocalDateTime.now()
                    .format(DateTimeFormatter.BASIC_ISO_DATE);

            Optional<BatchControlRecord> recordOpt = batchControlRepository
                    .findById(new BatchControlKey(jobName, processDate, 1));

            recordOpt.ifPresent(record -> {
                record.setStepName("PROCESS");
                record.setProgramName(jobName);
                batchControlRepository.save(record);
            });

            log.info("Batch control status updated for job: {}", jobName);
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("updateStatusStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    /**
     * TERM step: Finalize batch.
     * Replaces: BCHCTL00.cbl 4000-PROCESS-TERMINATE.
     */
    @Bean
    public Step terminateStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            Object jobNameParam4 = chunkContext.getStepContext().getJobParameters().get("jobName");
            String jobName = jobNameParam4 != null ? jobNameParam4.toString() : "DEFAULT";
            String processDate = LocalDateTime.now()
                    .format(DateTimeFormatter.BASIC_ISO_DATE);

            Optional<BatchControlRecord> recordOpt = batchControlRepository
                    .findById(new BatchControlKey(jobName, processDate, 1));

            recordOpt.ifPresent(record -> {
                record.setStatus(BatchStatus.DONE.getCode());
                record.setEndTime(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                record.setCompleteTs(LocalDateTime.now());
                record.setReturnCode(0);
                batchControlRepository.save(record);
            });

            auditService.logSystemEvent("SHUTDOWN", "SUCC",
                    "Batch control finalized: " + jobName);
            log.info("Batch control finalized for job: {}", jobName);
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("terminateStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
