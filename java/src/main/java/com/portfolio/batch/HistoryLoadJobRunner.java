package com.portfolio.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * JCL → Spring Boot mapping: the HISTLD00 job step from
 * {@code src/jcl/HISTLOAD.jcl} becomes this runner, activated with the
 * {@code histld00} profile. The COBOL RETURN-CODE (error count) becomes the
 * JVM exit code via {@link ExitCodeGenerator}.
 */
@Component
@Profile("histld00")
public class HistoryLoadJobRunner implements ApplicationRunner, ExitCodeGenerator {

    private final JobLauncher jobLauncher;
    private final Job histld00Job;
    private final HistoryLoadStats stats;

    public HistoryLoadJobRunner(JobLauncher jobLauncher, Job histld00Job, HistoryLoadStats stats) {
        this.jobLauncher = jobLauncher;
        this.histld00Job = histld00Job;
        this.stats = stats;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String processDate = args.getNonOptionArgs().isEmpty()
                ? LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                : args.getNonOptionArgs().get(0);
        JobParameters params = new JobParametersBuilder()
                .addString("processDate", processDate)
                .addLong("startedAt", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(histld00Job, params);

        // HISTLD00 3400-DISPLAY-STATS
        System.out.println("HISTLD00 Processing Statistics:");
        System.out.println("  Records Read:    " + stats.getRecordsRead());
        System.out.println("  Records Written: " + stats.getRecordsWritten());
        System.out.println("  Errors:          " + stats.getErrorCount());
        System.out.println("  Job Status:      " + execution.getStatus());
    }

    /** MOVE WS-ERROR-COUNT TO RETURN-CODE. */
    @Override
    public int getExitCode() {
        return (int) Math.min(stats.getErrorCount(), 255);
    }
}
