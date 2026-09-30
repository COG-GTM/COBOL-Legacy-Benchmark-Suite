package com.portfolio.batch;

import com.portfolio.domain.ReturnCodeRecord;
import com.portfolio.repository.ReturnCodeRepository;
import java.time.LocalDateTime;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class ReturnCodeRecordingListener implements StepExecutionListener {
  private final ReturnCodeRepository returnCodeRepository;

  public ReturnCodeRecordingListener(ReturnCodeRepository returnCodeRepository) {
    this.returnCodeRepository = returnCodeRepository;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    int returnCode = stepExecution.getExecutionContext().getInt("returnCode", 0);
    int highestCode = stepExecution.getExecutionContext().getInt("highestCode", returnCode);
    String programId = programId(stepExecution.getStepName());
    ReturnCodeRecord record = new ReturnCodeRecord();
    record.setRcTimestamp(LocalDateTime.now());
    record.setProgramId(programId);
    record.setReturnCode(returnCode);
    record.setHighestCode(highestCode);
    record.setStatusCode(returnCode > 0 ? "E" : "D");
    record.setMessageText(stepExecution.getExitStatus().getExitDescription());
    returnCodeRepository.save(record);
    return null;
  }

  private String programId(String stepName) {
    if (stepName.contains("trnVal")) return "TRNVAL00";
    if (stepName.contains("posUpd")) return "POSUPD00";
    if (stepName.contains("histLoad")) return "HISTLD00";
    if (stepName.contains("positionReport")) return "RPTPOS00";
    if (stepName.contains("auditReport")) return "RPTAUD00";
    if (stepName.contains("statsReport")) return "RPTSTA00";
    return "RTNANA00";
  }
}
