package com.portfolio.batch.jobs;

import com.portfolio.model.entity.ErrorLogEntry;
import com.portfolio.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class StatisticsReportJobConfig {

    private static final Logger log = LoggerFactory.getLogger(StatisticsReportJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ErrorLogRepository errorLogRepository;

    public StatisticsReportJobConfig(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     ErrorLogRepository errorLogRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.errorLogRepository = errorLogRepository;
    }

    @Bean
    public Job statisticsReportJob() {
        return new JobBuilder("statisticsReportJob", jobRepository)
                .start(statisticsReportStep())
                .build();
    }

    @Bean
    public Step statisticsReportStep() {
        return new StepBuilder("statisticsReportStep", jobRepository)
                .<ErrorLogEntry, String>chunk(100, transactionManager)
                .reader(statisticsReader())
                .processor(statisticsProcessor())
                .writer(statisticsWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<ErrorLogEntry> statisticsReader() {
        return new RepositoryItemReaderBuilder<ErrorLogEntry>()
                .name("statisticsReader")
                .repository(errorLogRepository)
                .methodName("findAll")
                .sorts(Map.of("processDate", Sort.Direction.DESC))
                .build();
    }

    @Bean
    public ItemProcessor<ErrorLogEntry, String> statisticsProcessor() {
        return entry -> String.format("%-8s %-26s %-8s SEV=%d %s",
                entry.getProgramId(),
                entry.getErrorTimestamp(),
                entry.getErrorCode(),
                entry.getErrorSeverity(),
                entry.getErrorMessage());
    }

    @Bean
    public ItemWriter<String> statisticsWriter() {
        return items -> items.forEach(line -> log.info("STATS_REPORT: {}", line));
    }
}
