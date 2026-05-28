package com.clbs.portfolio.batch.config;

import com.clbs.portfolio.batch.processor.HistoryLoadProcessor;
import com.clbs.portfolio.entity.HistoryRecord;
import com.clbs.portfolio.entity.PositionHistory;
import com.clbs.portfolio.repository.HistoryRecordRepository;
import com.clbs.portfolio.repository.PositionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class HistoryLoadJobConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_ERRORS = 100;

    private final HistoryRecordRepository historyRecordRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final HistoryLoadProcessor historyLoadProcessor;

    @Bean
    public RepositoryItemReader<HistoryRecord> historyLoadReader() {
        return new RepositoryItemReaderBuilder<HistoryRecord>()
                .name("historyLoadReader")
                .repository(historyRecordRepository)
                .methodName("findByStatus")
                .arguments(Collections.singletonList("PENDING"))
                .sorts(Map.of("id", Sort.Direction.ASC))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemWriter<PositionHistory> historyLoadWriter() {
        return items -> positionHistoryRepository.saveAll(items);
    }

    @Bean
    public Step historyLoadStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("historyLoadStep", jobRepository)
                .<HistoryRecord, PositionHistory>chunk(CHUNK_SIZE, transactionManager)
                .reader(historyLoadReader())
                .processor(historyLoadProcessor)
                .writer(historyLoadWriter())
                .faultTolerant()
                .skipLimit(MAX_ERRORS)
                .skip(Exception.class)
                .build();
    }
}
