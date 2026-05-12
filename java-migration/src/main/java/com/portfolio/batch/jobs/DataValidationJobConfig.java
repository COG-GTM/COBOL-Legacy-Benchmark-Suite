package com.portfolio.batch.jobs;

import com.portfolio.model.entity.Portfolio;
import com.portfolio.repository.PortfolioRepository;
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
public class DataValidationJobConfig {

    private static final Logger log = LoggerFactory.getLogger(DataValidationJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PortfolioRepository portfolioRepository;

    public DataValidationJobConfig(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   PortfolioRepository portfolioRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.portfolioRepository = portfolioRepository;
    }

    @Bean
    public Job dataValidationJob() {
        return new JobBuilder("dataValidationJob", jobRepository)
                .start(dataValidationStep())
                .build();
    }

    @Bean
    public Step dataValidationStep() {
        return new StepBuilder("dataValidationStep", jobRepository)
                .<Portfolio, String>chunk(100, transactionManager)
                .reader(validationReader())
                .processor(validationProcessor())
                .writer(validationWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<Portfolio> validationReader() {
        return new RepositoryItemReaderBuilder<Portfolio>()
                .name("validationReader")
                .repository(portfolioRepository)
                .methodName("findAll")
                .sorts(Map.of("portfolioId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Portfolio, String> validationProcessor() {
        return portfolio -> {
            StringBuilder issues = new StringBuilder();

            if (portfolio.getPortfolioId() == null || !portfolio.getPortfolioId().startsWith("PORT")) {
                issues.append("Invalid ID format; ");
            }
            if (portfolio.getPortfolioName() == null || portfolio.getPortfolioName().isBlank()) {
                issues.append("Missing name; ");
            }
            if (portfolio.getStatus() == null) {
                issues.append("Missing status; ");
            }

            if (issues.isEmpty()) {
                return null;
            }

            return String.format("VALIDATION ERROR: Portfolio %s - %s",
                    portfolio.getPortfolioId(), issues);
        };
    }

    @Bean
    public ItemWriter<String> validationWriter() {
        return items -> items.forEach(line -> log.warn(line));
    }
}
