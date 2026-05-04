package com.portfolio.batch;

import com.portfolio.domain.Position;
import com.portfolio.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;

/**
 * Position Report Job - migrated from COBOL RPTPOS00.cbl.
 * Reads positions, generates report.
 */
@Configuration
public class PositionReportJob {

    private static final Logger log = LoggerFactory.getLogger(PositionReportJob.class);

    private final PositionRepository positionRepository;

    public PositionReportJob(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @Bean
    public Job positionReportBatchJob(JobRepository jobRepository, Step positionReportStep) {
        return new JobBuilder("positionReportJob", jobRepository)
                .start(positionReportStep)
                .build();
    }

    @Bean
    public Step positionReportStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager) {
        return new StepBuilder("positionReportStep", jobRepository)
                .<Position, String>chunk(100, transactionManager)
                .reader(positionReportReader())
                .writer(positionReportWriter())
                .build();
    }

    @Bean
    public ItemReader<Position> positionReportReader() {
        return new ListItemReader<>(positionRepository.findAll());
    }

    @Bean
    public ItemWriter<String> positionReportWriter() {
        return items -> {
            BigDecimal totalValue = BigDecimal.ZERO;
            int count = 0;
            for (String line : items) {
                log.info("REPORT: {}", line);
                count++;
            }
            log.info("Position Report: {} positions written", count);
        };
    }
}
