package com.portfolio.batch;

import com.portfolio.entity.PositionHistory;
import com.portfolio.service.ReturnCodeService;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class HistoryLoadJobConfig {

    private static final Logger log = LoggerFactory.getLogger(HistoryLoadJobConfig.class);
    private static final int CHUNK_SIZE = 1000;
    private static final int SKIP_LIMIT = 100;

    private final ReturnCodeService returnCodeService;

    public HistoryLoadJobConfig(ReturnCodeService returnCodeService) {
        this.returnCodeService = returnCodeService;
    }

    @Bean
    public Job historyLoadJob(JobRepository jobRepository, Step historyLoadStep) {
        return new JobBuilder("historyLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(historyLoadStep)
                .build();
    }

    @Bean
    public Step historyLoadStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                EntityManagerFactory entityManagerFactory,
                                @Value("${batch.history.input-file:classpath:data/position-history.json}")
                                Resource inputFile) {
        return new StepBuilder("historyLoadStep", jobRepository)
                .<PositionHistory, PositionHistory>chunk(CHUNK_SIZE, transactionManager)
                .reader(historyItemReader(inputFile))
                .writer(historyItemWriter(entityManagerFactory))
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .listener(new HistoryLoadStepListener(returnCodeService))
                .build();
    }

    @Bean
    public JsonItemReader<PositionHistory> historyItemReader(
            @Value("${batch.history.input-file:classpath:data/position-history.json}")
            Resource inputFile) {
        return new JsonItemReaderBuilder<PositionHistory>()
                .jsonObjectReader(new JacksonJsonObjectReader<>(PositionHistory.class))
                .resource(inputFile)
                .name("historyItemReader")
                .build();
    }

    @Bean
    public JpaItemWriter<PositionHistory> historyItemWriter(
            EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<PositionHistory>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
