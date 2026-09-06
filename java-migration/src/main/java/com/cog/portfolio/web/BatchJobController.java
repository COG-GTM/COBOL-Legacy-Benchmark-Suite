package com.cog.portfolio.web;

import com.cog.portfolio.batch.ReportService;
import java.time.LocalDateTime;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces the JCL triggers under src/jcl. Each POST launches the Spring Batch
 * job that stands in for the JCL step. Access: operator / admin (see SecurityConfiguration).
 */
@RestController
@RequestMapping("/batch")
public class BatchJobController {

    public record LaunchResponse(String jobName, Long executionId, String status, String exitCode,
                                 String exitDescription) {
        static LaunchResponse of(JobExecution execution) {
            return new LaunchResponse(execution.getJobInstance().getJobName(), execution.getId(),
                    execution.getStatus().name(), execution.getExitStatus().getExitCode(),
                    execution.getExitStatus().getExitDescription());
        }
    }

    private final JobLauncher jobLauncher;
    private final Job dailyProcessingJob;
    private final Job positionReportJob;
    private final Job auditReportJob;
    private final Job statisticsReportJob;
    private final Job returnCodeAnalysisJob;
    private final Job historyLoadJob;
    private final ReportService reportService;

    public BatchJobController(JobLauncher jobLauncher, Job dailyProcessingJob, Job positionReportJob,
                              Job auditReportJob, Job statisticsReportJob, Job returnCodeAnalysisJob,
                              Job historyLoadJob, ReportService reportService) {
        this.jobLauncher = jobLauncher;
        this.dailyProcessingJob = dailyProcessingJob;
        this.positionReportJob = positionReportJob;
        this.auditReportJob = auditReportJob;
        this.statisticsReportJob = statisticsReportJob;
        this.returnCodeAnalysisJob = returnCodeAnalysisJob;
        this.historyLoadJob = historyLoadJob;
        this.reportService = reportService;
    }

    /** PRCSEQ daily sequence TRNVAL00 -> POSUPD00 -> HISTLD00 -> RPTPOS00. */
    @PostMapping("/daily")
    public ResponseEntity<LaunchResponse> daily() {
        return launch(dailyProcessingJob);
    }

    /** HISTLD.jcl. */
    @PostMapping("/history/load")
    public ResponseEntity<LaunchResponse> historyLoad() {
        return launch(historyLoadJob);
    }

    /** RPTPOS.jcl. */
    @PostMapping("/report/positions")
    public ResponseEntity<LaunchResponse> positions() {
        return launch(positionReportJob);
    }

    /** RPTAUD.jcl. */
    @PostMapping("/report/audit")
    public ResponseEntity<LaunchResponse> audit() {
        return launch(auditReportJob);
    }

    /** RPTSTA.jcl. */
    @PostMapping("/report/statistics")
    public ResponseEntity<LaunchResponse> statistics() {
        return launch(statisticsReportJob);
    }

    /** RTNANA.jcl. */
    @PostMapping("/returncodes/analyze")
    public ResponseEntity<LaunchResponse> returnCodes() {
        return launch(returnCodeAnalysisJob);
    }

    /** Report content (the SYSOUT the JCL would have printed). */
    @PostMapping("/report/positions/data")
    public ReportService.PositionReport positionReportData() {
        return reportService.positionReport();
    }

    @PostMapping("/returncodes/analyze/data")
    public ReportService.ReturnCodeReport returnCodeReportData() {
        return reportService.returnCodeReport();
    }

    private ResponseEntity<LaunchResponse> launch(Job job) {
        try {
            JobExecution execution = jobLauncher.run(job, new JobParametersBuilder()
                    .addLocalDateTime("requestedAt", LocalDateTime.now())
                    .toJobParameters());
            HttpStatus status = execution.getStatus().isUnsuccessful() ? HttpStatus.INTERNAL_SERVER_ERROR
                    : HttpStatus.ACCEPTED;
            return ResponseEntity.status(status).body(LaunchResponse.of(execution));
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                 | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new LaunchResponse(job.getName(), null, "REJECTED", "FAILED", e.getMessage()));
        }
    }
}
