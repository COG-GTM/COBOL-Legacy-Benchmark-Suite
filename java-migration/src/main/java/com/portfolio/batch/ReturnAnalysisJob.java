package com.portfolio.batch;

import com.portfolio.domain.BatchControlRecord;
import com.portfolio.repository.BatchControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Return Analysis Job - migrated from COBOL RTNANA00.cbl.
 * Analyzes return codes from batch processing.
 */
@Configuration
public class ReturnAnalysisJob {

    private static final Logger log = LoggerFactory.getLogger(ReturnAnalysisJob.class);

    private final BatchControlRepository batchControlRepository;

    public ReturnAnalysisJob(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    @Bean
    public Job returnAnalysisBatchJob(JobRepository jobRepository, Step returnAnalysisStep) {
        return new JobBuilder("returnAnalysisJob", jobRepository)
                .start(returnAnalysisStep)
                .build();
    }

    @Bean
    public Step returnAnalysisStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager) {
        return new StepBuilder("returnAnalysisStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    List<BatchControlRecord> allJobs = batchControlRepository.findAll();
                    int totalJobs = allJobs.size();
                    long successJobs = allJobs.stream()
                            .filter(j -> j.getReturnCode() == 0).count();
                    long warningJobs = allJobs.stream()
                            .filter(j -> j.getReturnCode() == 4).count();
                    long errorJobs = allJobs.stream()
                            .filter(j -> j.getReturnCode() >= 8).count();

                    log.info("=== RETURN CODE ANALYSIS ===");
                    log.info("Total Jobs: {}", totalJobs);
                    log.info("Success (RC=0): {}", successJobs);
                    log.info("Warning (RC=4): {}", warningJobs);
                    log.info("Error (RC>=8): {}", errorJobs);
                    log.info("============================");

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
