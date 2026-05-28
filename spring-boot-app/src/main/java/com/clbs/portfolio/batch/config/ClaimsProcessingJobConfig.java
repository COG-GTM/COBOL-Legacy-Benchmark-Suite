package com.clbs.portfolio.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ClaimsProcessingJobConfig {

    @Bean
    public Job claimsProcessingJob(JobRepository jobRepository,
                                    Step transactionValidationStep,
                                    Step adjudicationStep,
                                    Step positionUpdateStep,
                                    Step historyLoadStep) {
        Flow validationFlow = new FlowBuilder<SimpleFlow>("validationFlow")
                .start(transactionValidationStep)
                .build();

        Flow adjudicationFlow = new FlowBuilder<SimpleFlow>("adjudicationFlow")
                .start(adjudicationStep)
                .build();

        Flow positionUpdateFlow = new FlowBuilder<SimpleFlow>("positionUpdateFlow")
                .start(positionUpdateStep)
                .build();

        Flow historyLoadFlow = new FlowBuilder<SimpleFlow>("historyLoadFlow")
                .start(historyLoadStep)
                .build();

        Flow mainFlow = new FlowBuilder<SimpleFlow>("mainProcessingFlow")
                .start(validationFlow)
                .next(adjudicationFlow)
                .next(positionUpdateFlow)
                .next(historyLoadFlow)
                .build();

        return new JobBuilder("claimsProcessingJob", jobRepository)
                .start(mainFlow)
                .end()
                .build();
    }
}
