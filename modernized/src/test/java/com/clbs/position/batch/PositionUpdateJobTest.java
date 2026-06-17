package com.clbs.position.batch;

import com.clbs.position.entity.Position;
import com.clbs.position.repository.PositionRepository;
import com.clbs.position.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of the Spring Batch position-update job (the modern
 * checkpoint/restart driver). Runs the chunk-oriented job over the seeded
 * pending transactions and verifies the resulting holdings.
 */
@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:batchdb;DB_CLOSE_DELAY=-1")
class PositionUpdateJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Job processes all pending transactions and completes")
    void jobCompletes() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("runDate", "20240617")
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // No transactions remain pending.
        assertThat(transactionRepository
                .findByStatusOrderByTrnDateAscTrnTimeAscPortfolioIdAscSequenceNoAsc("P"))
                .isEmpty();

        // PORT0001 / SEC0000001: 1000 + 100 (buy) = 1100; cost 50000 + 5500 - 25 (fee) = 55475
        Optional<Position> pos = positionRepository
                .findFirstByPortfolioIdAndInvestmentIdOrderByPositionDateDesc("PORT0001", "SEC0000001");
        assertThat(pos).isPresent();
        assertThat(pos.get().getQuantity()).isEqualByComparingTo("1100.0000");
        assertThat(pos.get().getCostBasis()).isEqualByComparingTo("55475.00");
    }
}
