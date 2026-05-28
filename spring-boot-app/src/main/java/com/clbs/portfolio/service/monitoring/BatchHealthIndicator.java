package com.clbs.portfolio.service.monitoring;

import com.clbs.portfolio.enums.BatchStatus;
import com.clbs.portfolio.repository.BatchControlRecordRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class BatchHealthIndicator implements HealthIndicator {

    private final BatchControlRecordRepository batchControlRecordRepository;

    public BatchHealthIndicator(BatchControlRecordRepository batchControlRecordRepository) {
        this.batchControlRecordRepository = batchControlRecordRepository;
    }

    @Override
    public Health health() {
        long activeJobs = batchControlRecordRepository.countByStatus(BatchStatus.ACTIVE);
        long errorJobs = batchControlRecordRepository.countByStatus(BatchStatus.ERROR);

        Health.Builder builder;
        if (errorJobs > 0) {
            builder = Health.down()
                    .withDetail("reason", "Failed batch jobs detected");
        } else if (activeJobs > 10) {
            builder = Health.down()
                    .withDetail("reason", "Too many active batch jobs (possible stuck jobs)");
        } else {
            builder = Health.up();
        }

        return builder
                .withDetail("activeJobs", activeJobs)
                .withDetail("errorJobs", errorJobs)
                .build();
    }
}
