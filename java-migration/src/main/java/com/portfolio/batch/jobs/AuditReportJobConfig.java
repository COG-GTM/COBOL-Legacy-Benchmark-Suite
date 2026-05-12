package com.portfolio.batch.jobs;

import com.portfolio.model.entity.AuditRecord;
import com.portfolio.repository.AuditRecordRepository;
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
public class AuditReportJobConfig {

    private static final Logger log = LoggerFactory.getLogger(AuditReportJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AuditRecordRepository auditRecordRepository;

    public AuditReportJobConfig(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                AuditRecordRepository auditRecordRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.auditRecordRepository = auditRecordRepository;
    }

    @Bean
    public Job auditReportJob() {
        return new JobBuilder("auditReportJob", jobRepository)
                .start(auditReportStep())
                .build();
    }

    @Bean
    public Step auditReportStep() {
        return new StepBuilder("auditReportStep", jobRepository)
                .<AuditRecord, String>chunk(100, transactionManager)
                .reader(auditReportReader())
                .processor(auditReportProcessor())
                .writer(auditReportWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<AuditRecord> auditReportReader() {
        return new RepositoryItemReaderBuilder<AuditRecord>()
                .name("auditReportReader")
                .repository(auditRecordRepository)
                .methodName("findAll")
                .sorts(Map.of("auditTimestamp", Sort.Direction.DESC))
                .build();
    }

    @Bean
    public ItemProcessor<AuditRecord, String> auditReportProcessor() {
        return record -> String.format("%-26s %-8s %-8s %-4s %-8s %-4s %s",
                record.getAuditTimestamp(),
                record.getUserId(),
                record.getProgram(),
                record.getAuditType(),
                record.getAuditAction(),
                record.getAuditStatus(),
                record.getMessage() != null ? record.getMessage() : "");
    }

    @Bean
    public ItemWriter<String> auditReportWriter() {
        return items -> items.forEach(line -> log.info("AUDIT_REPORT: {}", line));
    }
}
