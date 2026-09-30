package com.portfolio.batch;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;

@Service
public class BatchLaunchService {
  private final JobLauncher jobLauncher;
  private final JobRepository jobRepository;
  private final Map<String, Job> jobs;

  public BatchLaunchService(JobLauncher jobLauncher, JobRepository jobRepository, Set<Job> jobSet) {
    this.jobLauncher = jobLauncher;
    this.jobRepository = jobRepository;
    this.jobs = new LinkedHashMap<>();
    for (Job job : jobSet) {
      jobs.put(job.getName(), job);
    }
  }

  public JobRunResult run(String jobName) {
    Job job = jobs.get(jobName) != null ? jobs.get(jobName) : throwUnknownJob(jobName);
    JobParameters parameters =
        new JobParametersBuilder().addLong("run.id", System.currentTimeMillis()).toJobParameters();
    try {
      JobExecution execution = jobLauncher.run(job, parameters);
      return result(execution);
    } catch (Exception exception) {
      return new JobRunResult(
          jobName, null, BatchStatus.FAILED.name(), "FAILED", exception.getMessage(), Map.of());
    }
  }

  private Job throwUnknownJob(String jobName) {
    throw new IllegalArgumentException("Unknown batch job: " + jobName);
  }

  private JobRunResult result(JobExecution execution) {
    Map<String, Object> summaries = new LinkedHashMap<>();
    for (StepExecution stepExecution : execution.getStepExecutions()) {
      Map<String, Object> summary = new LinkedHashMap<>();
      summary.put("read", stepExecution.getReadCount());
      summary.put("write", stepExecution.getWriteCount());
      summary.put("skip", stepExecution.getSkipCount());
      summary.put("returnCode", stepExecution.getExecutionContext().getInt("returnCode", 0));
      summaries.put(stepExecution.getStepName(), summary);
    }
    return new JobRunResult(
        execution.getJobInstance().getJobName(),
        execution.getId(),
        execution.getStatus().name(),
        execution.getExitStatus().getExitCode(),
        execution.getExitStatus().getExitDescription(),
        summaries);
  }

  public record JobRunResult(
      String jobName,
      Long executionId,
      String status,
      String exitCode,
      String exitDescription,
      Map<String, Object> stepSummaries) {}
}
