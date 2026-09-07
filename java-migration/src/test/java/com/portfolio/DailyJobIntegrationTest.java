package com.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("demo")
class DailyJobIntegrationTest {
  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  @Qualifier("dailyJob")
  private Job dailyJob;

  @Autowired
  @Qualifier("histLoadJob")
  private Job histLoadJob;

  @Test
  void dailyJobCompletesAndHistoryLoadIsIdempotent() throws Exception {
    jobLauncherTestUtils.setJob(dailyJob);
    JobExecution dailyExecution =
        jobLauncherTestUtils.launchJob(
            new org.springframework.batch.core.JobParametersBuilder()
                .addLong("daily", System.nanoTime())
                .toJobParameters());
    assertEquals(BatchStatus.COMPLETED, dailyExecution.getStatus());

    jobLauncherTestUtils.setJob(histLoadJob);
    JobExecution firstHistory =
        jobLauncherTestUtils.launchJob(
            new org.springframework.batch.core.JobParametersBuilder()
                .addLong("history-one", System.nanoTime())
                .toJobParameters());
    JobExecution secondHistory =
        jobLauncherTestUtils.launchJob(
            new org.springframework.batch.core.JobParametersBuilder()
                .addLong("history-two", System.nanoTime())
                .toJobParameters());
    assertEquals(BatchStatus.COMPLETED, firstHistory.getStatus());
    assertEquals(BatchStatus.COMPLETED, secondHistory.getStatus());
    assertEquals(0, secondHistory.getStepExecutions().iterator().next().getWriteCount());
  }
}
