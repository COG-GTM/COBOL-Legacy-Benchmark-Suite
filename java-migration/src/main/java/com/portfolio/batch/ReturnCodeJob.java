package com.portfolio.batch;

import com.portfolio.domain.enums.ReturnCode;
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

/**
 * Return Code Job - migrated from COBOL RTNCDE00.cbl.
 */
@Configuration
public class ReturnCodeJob {

    private static final Logger log = LoggerFactory.getLogger(ReturnCodeJob.class);

    @Bean
    public Job returnCodeBatchJob(JobRepository jobRepository, Step returnCodeStep) {
        return new JobBuilder("returnCodeJob", jobRepository)
                .start(returnCodeStep)
                .build();
    }

    @Bean
    public Step returnCodeStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
        return new StepBuilder("returnCodeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("=== RETURN CODE DEFINITIONS ===");
                    for (ReturnCode rc : ReturnCode.values()) {
                        log.info("Code {} ({}): {}",
                                rc.getCode(), rc.name(),
                                rc.isError() ? "ERROR" : "OK");
                    }
                    log.info("================================");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
