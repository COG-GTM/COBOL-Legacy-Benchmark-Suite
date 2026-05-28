package com.clbs.portfolio.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Bean
    public Job positionReportJob(JobRepository jobRepository,
                                  @Qualifier("positionReportStep") Step positionReportStep) {
        return new JobBuilder("positionReportJob", jobRepository)
                .start(positionReportStep)
                .build();
    }

    @Bean
    public Step positionReportStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    @Qualifier("positionReportTasklet") Tasklet tasklet) {
        return new StepBuilder("positionReportStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job auditReportJob(JobRepository jobRepository,
                               @Qualifier("auditReportStep") Step auditReportStep) {
        return new JobBuilder("auditReportJob", jobRepository)
                .start(auditReportStep)
                .build();
    }

    @Bean
    public Step auditReportStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 @Qualifier("auditReportTasklet") Tasklet tasklet) {
        return new StepBuilder("auditReportStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job systemStatsReportJob(JobRepository jobRepository,
                                     @Qualifier("systemStatsReportStep") Step step) {
        return new JobBuilder("systemStatsReportJob", jobRepository)
                .start(step)
                .build();
    }

    @Bean
    public Step systemStatsReportStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       @Qualifier("systemStatsReportTasklet") Tasklet tasklet) {
        return new StepBuilder("systemStatsReportStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job maintenanceJob(JobRepository jobRepository,
                               @Qualifier("maintenanceStep") Step step) {
        return new JobBuilder("maintenanceJob", jobRepository)
                .start(step)
                .build();
    }

    @Bean
    public Step maintenanceStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 @Qualifier("maintenanceTasklet") Tasklet tasklet) {
        return new StepBuilder("maintenanceStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
