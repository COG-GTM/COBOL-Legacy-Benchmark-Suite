package com.portfolio.controller;

import com.portfolio.dto.BatchJobResponse;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Batch controller - allows triggering batch jobs via REST API.
 * Replaces BCHCTL00 batch control program.
 * Source: src/programs/batch/BCHCTL00.cbl
 */
@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final JobLauncher asyncJobLauncher;
    private final Job endOfDayJob;

    public BatchController(@Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher,
                           Job endOfDayJob) {
        this.asyncJobLauncher = asyncJobLauncher;
        this.endOfDayJob = endOfDayJob;
    }

    @PostMapping("/end-of-day")
    public ResponseEntity<BatchJobResponse> runEndOfDay() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = asyncJobLauncher.run(endOfDayJob, params);

        BatchJobResponse response = new BatchJobResponse();
        response.setJobId(execution.getJobId());
        response.setJobName("endOfDayJob");
        response.setStatus(execution.getStatus().name());
        response.setStartTime(execution.getStartTime());
        response.setMessage("Job submitted successfully");

        return ResponseEntity.ok(response);
    }
}
