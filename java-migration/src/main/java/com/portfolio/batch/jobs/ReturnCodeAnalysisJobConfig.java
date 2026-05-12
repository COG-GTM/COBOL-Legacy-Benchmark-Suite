package com.portfolio.batch.jobs;

import com.portfolio.model.entity.ReturnCodeEntry;
import com.portfolio.repository.ReturnCodeRepository;
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

import java.text.DecimalFormat;
import java.util.Map;

@Configuration
public class ReturnCodeAnalysisJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ReturnCodeAnalysisJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ReturnCodeRepository returnCodeRepository;

    public ReturnCodeAnalysisJobConfig(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       ReturnCodeRepository returnCodeRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.returnCodeRepository = returnCodeRepository;
    }

    @Bean
    public Job returnCodeAnalysisJob() {
        return new JobBuilder("returnCodeAnalysisJob", jobRepository)
                .start(returnCodeAnalysisStep())
                .build();
    }

    @Bean
    public Step returnCodeAnalysisStep() {
        return new StepBuilder("returnCodeAnalysisStep", jobRepository)
                .<ReturnCodeEntry, String>chunk(100, transactionManager)
                .reader(returnCodeReader())
                .processor(returnCodeProcessor())
                .writer(returnCodeWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<ReturnCodeEntry> returnCodeReader() {
        return new RepositoryItemReaderBuilder<ReturnCodeEntry>()
                .name("returnCodeReader")
                .repository(returnCodeRepository)
                .methodName("findAll")
                .sorts(Map.of("programId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<ReturnCodeEntry, String> returnCodeProcessor() {
        DecimalFormat countFormat = new DecimalFormat("#,##0");
        return entry -> String.format("%-8s %10s RC=%-4d Highest=%-4d Status=%c %s",
                entry.getProgramId(),
                countFormat.format(1),
                entry.getReturnCode(),
                entry.getHighestCode(),
                entry.getStatusCode(),
                entry.getMessageText() != null ? entry.getMessageText() : "");
    }

    @Bean
    public ItemWriter<String> returnCodeWriter() {
        return items -> items.forEach(line -> log.info("RC_ANALYSIS: {}", line));
    }
}
