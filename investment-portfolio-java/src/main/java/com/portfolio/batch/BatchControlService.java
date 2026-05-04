package com.portfolio.batch;

import com.portfolio.entity.BatchControl;
import com.portfolio.entity.BatchControlId;
import com.portfolio.entity.BatchStatus;
import com.portfolio.repository.BatchControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BatchControlService {

    private static final Logger log = LoggerFactory.getLogger(BatchControlService.class);

    private final BatchControlRepository batchControlRepository;

    public BatchControlService(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    @Transactional
    public BatchControl createJobEntry(String jobName, String processDate, int sequenceNo,
                                       String stepName, String programName) {
        BatchControl control = new BatchControl();
        control.setJobName(jobName);
        control.setProcessDate(processDate);
        control.setSequenceNo(sequenceNo);
        control.setStatus(BatchStatus.READY);
        control.setStepName(stepName);
        control.setProgramName(programName);
        control.setReturnCode(0);
        control.setRestartCount(0);

        return batchControlRepository.save(control);
    }

    @Transactional
    public void startJob(String jobName, String processDate, int sequenceNo) {
        BatchControlId id = new BatchControlId(jobName, processDate, sequenceNo);
        BatchControl control = batchControlRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch control not found: " + jobName));

        control.setStatus(BatchStatus.ACTIVE);
        control.setStartTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        control.setAttemptTimestamp(LocalDateTime.now());

        batchControlRepository.save(control);
        log.info("Job started: {} seq={}", jobName, sequenceNo);
    }

    @Transactional
    public void completeJob(String jobName, String processDate, int sequenceNo, int returnCode) {
        BatchControlId id = new BatchControlId(jobName, processDate, sequenceNo);
        BatchControl control = batchControlRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch control not found: " + jobName));

        control.setStatus(returnCode == 0 ? BatchStatus.DONE : BatchStatus.ERROR);
        control.setEndTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        control.setReturnCode(returnCode);
        control.setCompleteTimestamp(LocalDateTime.now());

        batchControlRepository.save(control);
        log.info("Job completed: {} seq={} rc={}", jobName, sequenceNo, returnCode);
    }

    @Transactional
    public void failJob(String jobName, String processDate, int sequenceNo,
                        int returnCode, String errorDesc) {
        BatchControlId id = new BatchControlId(jobName, processDate, sequenceNo);
        BatchControl control = batchControlRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch control not found: " + jobName));

        control.setStatus(BatchStatus.ERROR);
        control.setEndTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        control.setReturnCode(returnCode);
        control.setErrorDesc(errorDesc);
        control.setRestartCount(control.getRestartCount() + 1);

        batchControlRepository.save(control);
        log.error("Job failed: {} seq={} rc={} error={}", jobName, sequenceNo, returnCode, errorDesc);
    }

    @Transactional(readOnly = true)
    public boolean checkPrerequisites(String jobName, String processDate, int sequenceNo) {
        BatchControlId id = new BatchControlId(jobName, processDate, sequenceNo);
        BatchControl control = batchControlRepository.findById(id).orElse(null);
        if (control == null || control.getPrereqCount() == 0) {
            return true;
        }
        List<BatchControl> prereqs = batchControlRepository.findByJobNameAndProcessDate(
                jobName, processDate);
        long completedPrereqs = prereqs.stream()
                .filter(p -> p.getStatus() == BatchStatus.DONE)
                .count();
        return completedPrereqs >= control.getPrereqCount();
    }
}
