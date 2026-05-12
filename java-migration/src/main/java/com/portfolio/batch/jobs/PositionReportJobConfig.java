package com.portfolio.batch.jobs;

import com.portfolio.model.entity.Position;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import com.portfolio.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;

@Configuration
public class PositionReportJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PositionReportJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PositionRepository positionRepository;

    public PositionReportJobConfig(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   PositionRepository positionRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.positionRepository = positionRepository;
    }

    @Bean
    public Job positionReportJob() {
        return new JobBuilder("positionReportJob", jobRepository)
                .start(positionReportStep())
                .build();
    }

    @Bean
    public Step positionReportStep() {
        return new StepBuilder("positionReportStep", jobRepository)
                .<Position, String>chunk(100, transactionManager)
                .reader(positionReportReader())
                .processor(positionReportProcessor())
                .writer(positionReportWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<Position> positionReportReader() {
        return new RepositoryItemReaderBuilder<Position>()
                .name("positionReportReader")
                .repository(positionRepository)
                .methodName("findAll")
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Position, String> positionReportProcessor() {
        DecimalFormat quantityFormat = new DecimalFormat("#,###,##0.00");
        DecimalFormat amountFormat = new DecimalFormat("$#,###,##0.00");
        DecimalFormat changeFormat = new DecimalFormat("+##0.00;-##0.00");

        return position -> {
            BigDecimal changePercent = BigDecimal.ZERO;
            if (position.getCostBasis() != null
                    && position.getCostBasis().compareTo(BigDecimal.ZERO) != 0
                    && position.getMarketValue() != null) {
                changePercent = position.getMarketValue()
                        .subtract(position.getCostBasis())
                        .divide(position.getCostBasis(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            return String.format("%-8s %-10s %15s %15s %15s %8s",
                    position.getId().getPortfolioId(),
                    position.getId().getInvestmentId(),
                    quantityFormat.format(position.getQuantity()),
                    amountFormat.format(position.getCostBasis()),
                    amountFormat.format(position.getMarketValue()),
                    changeFormat.format(changePercent));
        };
    }

    @Bean
    public ItemWriter<String> positionReportWriter() {
        return items -> items.forEach(line -> log.info("REPORT: {}", line));
    }
}
