package com.portfolio.portmstr.controller;

import com.portfolio.portmstr.dto.BatchJobResponse;
import com.portfolio.portmstr.dto.PortfolioRequest;
import com.portfolio.portmstr.dto.PortfolioResponse;
import com.portfolio.portmstr.dto.TransactionRequest;
import com.portfolio.portmstr.model.TransactionHistory;
import com.portfolio.portmstr.service.PortfolioMasterService;
import com.portfolio.portmstr.service.TransactionProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portfolio Master REST controller.
 * Exposes PORTMSTR CRUD operations and batch processing as REST endpoints.
 *
 * Maps COBOL PORTMSTR.cbl EVALUATE TRUE command routing:
 *   CREATE-PORT  -> POST /api/portfolios
 *   READ-PORT    -> GET  /api/portfolios/{id}
 *   UPDATE-PORT  -> PUT  /api/portfolios/{id}
 *   DELETE-PORT  -> DELETE /api/portfolios/{id}
 */
@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolio Master", description = "PORTMSTR - Portfolio Master CRUD operations")
public class PortfolioController {

    private final PortfolioMasterService portfolioService;
    private final TransactionProcessingService transactionService;
    private final JobLauncher jobLauncher;
    private final Job portfolioProcessingJob;

    public PortfolioController(PortfolioMasterService portfolioService,
                               TransactionProcessingService transactionService,
                               JobLauncher jobLauncher,
                               Job portfolioProcessingJob) {
        this.portfolioService = portfolioService;
        this.transactionService = transactionService;
        this.jobLauncher = jobLauncher;
        this.portfolioProcessingJob = portfolioProcessingJob;
    }

    @PostMapping
    @Operation(summary = "Create Portfolio", description = "COBOL 2000-CREATE-PORTFOLIO equivalent")
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @Valid @RequestBody PortfolioRequest request) {
        PortfolioResponse response = portfolioService.createPortfolio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{portfolioId}")
    @Operation(summary = "Read Portfolio", description = "COBOL 3000-READ-PORTFOLIO equivalent")
    public ResponseEntity<PortfolioResponse> readPortfolio(
            @Parameter(description = "Portfolio ID (e.g., PORT0001)")
            @PathVariable String portfolioId) {
        PortfolioResponse response = portfolioService.readPortfolio(portfolioId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{portfolioId}")
    @Operation(summary = "Update Portfolio", description = "COBOL 4000-UPDATE-PORTFOLIO equivalent")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @Parameter(description = "Portfolio ID")
            @PathVariable String portfolioId,
            @Valid @RequestBody PortfolioRequest request) {
        PortfolioResponse response = portfolioService.updatePortfolio(portfolioId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{portfolioId}")
    @Operation(summary = "Delete Portfolio", description = "COBOL 5000-DELETE-PORTFOLIO equivalent")
    public ResponseEntity<PortfolioResponse> deletePortfolio(
            @Parameter(description = "Portfolio ID")
            @PathVariable String portfolioId) {
        PortfolioResponse response = portfolioService.deletePortfolio(portfolioId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List All Portfolios", description = "COBOL PORTREAD sequential read equivalent")
    public ResponseEntity<List<PortfolioResponse>> listPortfolios() {
        return ResponseEntity.ok(portfolioService.listPortfolios());
    }

    @GetMapping("/active")
    @Operation(summary = "List Active Portfolios", description = "DB2 ACTIVE_PORTFOLIOS view equivalent")
    public ResponseEntity<List<PortfolioResponse>> listActivePortfolios() {
        return ResponseEntity.ok(portfolioService.listActivePortfolios());
    }

    @PostMapping("/{portfolioId}/transactions")
    @Operation(summary = "Process Transaction",
            description = "COBOL PORTTRAN transaction processing equivalent")
    public ResponseEntity<TransactionHistory> processTransaction(
            @Parameter(description = "Portfolio ID")
            @PathVariable String portfolioId,
            @Valid @RequestBody TransactionRequest request) {
        if (!portfolioId.equals(request.portfolioId())) {
            throw new com.portfolio.portmstr.exception.PortfolioValidationException(
                    "Path portfolioId '" + portfolioId +
                    "' does not match request body portfolioId '" + request.portfolioId() + "'", 1);
        }
        TransactionHistory result = transactionService.processTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/batch/run")
    @Operation(summary = "Run Batch Job",
            description = "Equivalent to JCL batch job execution (PORTADD/PORTUPDT/PORTDEL)")
    public ResponseEntity<BatchJobResponse> runBatchJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(portfolioProcessingJob, params);

            BatchJobResponse response = new BatchJobResponse(
                    execution.getId(),
                    execution.getJobInstance().getJobName(),
                    execution.getStatus().name(),
                    0, 0, 0,
                    0,
                    "Batch job " + execution.getStatus().name()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            BatchJobResponse response = new BatchJobResponse(
                    0, "portfolioProcessingJob", "FAILED",
                    0, 0, 0, 12,
                    "Batch job failed: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
