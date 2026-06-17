package com.clbs.position.web;

import com.clbs.position.batch.PositionUpdateJobConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Triggers the position-update batch job &mdash; the modern equivalent of the JCL
 * job step that executed {@code POSUPD00}
 * ({@code documentation/technical/data-dictionary.md} 9.1, window 1815-1900,
 * prerequisite {@code TRNVAL00}). Supplying the same {@code runDate} restarts a
 * previously failed run from its last committed checkpoint.
 */
@RestController
@RequestMapping("/positions/jobs")
@Tag(name = "Batch", description = "Run / restart the position-update batch job")
public class PositionJobController {

    private final JobLauncher jobLauncher;
    private final Job positionUpdateJob;

    public PositionJobController(JobLauncher jobLauncher, Job positionUpdateJob) {
        this.jobLauncher = jobLauncher;
        this.positionUpdateJob = positionUpdateJob;
    }

    @PostMapping("/position-update")
    @Operation(summary = "Run the position-update job (POSUPD00)",
            description = "Launches the chunk-oriented batch job. Re-running with "
                    + "the same runDate restarts from the last committed checkpoint.")
    public ResponseEntity<Map<String, Object>> run(
            @RequestParam(name = "runDate") String runDate) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("runDate", runDate)
                .toJobParameters();
        JobExecution execution = jobLauncher.run(positionUpdateJob, params);
        return ResponseEntity.ok(Map.of(
                "job", PositionUpdateJobConfig.JOB_NAME,
                "status", execution.getStatus().toString(),
                "exitCode", execution.getExitStatus().getExitCode(),
                "readCount", execution.getStepExecutions().stream()
                        .mapToLong(s -> s.getReadCount()).sum(),
                "writeCount", execution.getStepExecutions().stream()
                        .mapToLong(s -> s.getWriteCount()).sum()));
    }
}
