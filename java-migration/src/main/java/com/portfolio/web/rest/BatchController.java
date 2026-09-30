package com.portfolio.web.rest;

import com.portfolio.batch.BatchLaunchService;
import com.portfolio.batch.BatchLaunchService.JobRunResult;
import com.portfolio.security.AuthorizationService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
public class BatchController {
  private final BatchLaunchService batchLaunchService;
  private final AuthorizationService authorizationService;

  public BatchController(
      BatchLaunchService batchLaunchService, AuthorizationService authorizationService) {
    this.batchLaunchService = batchLaunchService;
    this.authorizationService = authorizationService;
  }

  @PostMapping("/daily")
  public ResponseEntity<JobRunResult> daily(Principal principal) {
    return launch(principal, "dailyJob");
  }

  @PostMapping("/hist-load")
  public ResponseEntity<JobRunResult> historyLoad(Principal principal) {
    return launch(principal, "histLoadJob");
  }

  @PostMapping("/report/position")
  public ResponseEntity<JobRunResult> positionReport(Principal principal) {
    return launch(principal, "positionReportJob");
  }

  @PostMapping("/report/audit")
  public ResponseEntity<JobRunResult> auditReport(Principal principal) {
    return launch(principal, "auditReportJob");
  }

  @PostMapping("/report/stats")
  public ResponseEntity<JobRunResult> statsReport(Principal principal) {
    return launch(principal, "statsReportJob");
  }

  @PostMapping("/analysis/rtn")
  public ResponseEntity<JobRunResult> returnCodeAnalysis(Principal principal) {
    return launch(principal, "rtnAnalysisJob");
  }

  private ResponseEntity<JobRunResult> launch(Principal principal, String jobName) {
    authorizationService.requireAccess(principal.getName(), "BATCH", "EXECUTE");
    JobRunResult result = batchLaunchService.run(jobName);
    HttpStatus status =
        switch (result.status()) {
          case "COMPLETED" -> HttpStatus.OK;
          case "STOPPED" -> HttpStatus.CONFLICT;
          default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    return ResponseEntity.status(status).body(result);
  }
}
