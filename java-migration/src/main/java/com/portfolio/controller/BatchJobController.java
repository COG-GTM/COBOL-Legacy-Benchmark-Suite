package com.portfolio.controller;

import com.portfolio.repository.BatchControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/batch")
public class BatchJobController {

    private static final Logger log = LoggerFactory.getLogger(BatchJobController.class);
    private final JobLauncher jobLauncher;
    private final Job dailyBatchJob;
    private final BatchControlRepository batchControlRepository;

    public BatchJobController(JobLauncher jobLauncher,
                              Job dailyBatchJob,
                              BatchControlRepository batchControlRepository) {
        this.jobLauncher = jobLauncher;
        this.dailyBatchJob = dailyBatchJob;
        this.batchControlRepository = batchControlRepository;
    }

    @GetMapping
    public String batchDashboard(Model model) {
        model.addAttribute("recentJobs", batchControlRepository.findTop20ByOrderByStartTimeDesc());
        return "batch-dashboard";
    }

    @PostMapping("/run-daily")
    public String runDailyBatch(RedirectAttributes redirectAttributes) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runDate", LocalDateTime.now().toString())
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(dailyBatchJob, params);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Daily batch job launched. Status: " + execution.getStatus());
            log.info("Daily batch job launched: {}", execution.getJobId());
        } catch (Exception e) {
            log.error("Failed to launch batch job", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to launch batch job: " + e.getMessage());
        }
        return "redirect:/batch";
    }

    @PostMapping("/api/run-daily")
    @ResponseBody
    public Map<String, Object> runDailyBatchApi() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runDate", LocalDateTime.now().toString())
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(dailyBatchJob, params);
            return Map.of(
                    "status", "launched",
                    "jobId", execution.getJobId(),
                    "batchStatus", execution.getStatus().toString()
            );
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
