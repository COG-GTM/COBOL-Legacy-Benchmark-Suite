package com.portfolio.service.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class ProcessSequenceService {

    private static final Logger log = LoggerFactory.getLogger(ProcessSequenceService.class);

    private final JobLauncher jobLauncher;
    private final Map<String, Job> availableJobs;

    public ProcessSequenceService(JobLauncher jobLauncher, Map<String, Job> availableJobs) {
        this.jobLauncher = jobLauncher;
        this.availableJobs = availableJobs;
    }

    public void executeMainSequence() throws Exception {
        String[] sequence = {"transactionValidationJob", "positionUpdateJob",
                "historyLoadJob", "positionReportJob"};

        for (String jobName : sequence) {
            Job job = availableJobs.get(jobName);
            if (job == null) {
                log.warn("Job not found: {}", jobName);
                continue;
            }

            JobParameters params = new JobParametersBuilder()
                    .addDate("runDate", new Date())
                    .addString("jobName", jobName)
                    .toJobParameters();

            log.info("Starting job: {}", jobName);
            jobLauncher.run(job, params);
            log.info("Completed job: {}", jobName);
        }
    }
}
