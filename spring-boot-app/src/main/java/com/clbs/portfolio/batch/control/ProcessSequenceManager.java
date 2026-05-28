package com.clbs.portfolio.batch.control;

import com.clbs.portfolio.model.BatchControlRecord;
import com.clbs.portfolio.model.BatchControlRecord.BatchControlStatus;
import com.clbs.portfolio.model.ProcessDependency;
import com.clbs.portfolio.model.ProcessSequenceRecord;
import com.clbs.portfolio.model.ProcessSequenceRecordId;
import com.clbs.portfolio.repository.BatchControlRecordRepository;
import com.clbs.portfolio.repository.ProcessSequenceRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Process sequencing and dependency resolution replacing COBOL program PRCSEQ00.
 * Manages ordered Step definitions within Spring Batch Jobs.
 *
 * Paragraph mapping:
 *   1000-INITIALIZE-SEQUENCE -> initializeSequence()
 *   2000-GET-NEXT-PROCESS -> getNextProcess()
 *   3000-CHECK-STATUS -> checkStatus()
 *   4000-TERMINATE-SEQUENCE -> terminateSequence()
 */
@Component
public class ProcessSequenceManager implements JobExecutionDecider {

    private static final Logger log = LoggerFactory.getLogger(ProcessSequenceManager.class);

    private final ProcessSequenceRecordRepository processSequenceRecordRepository;
    private final BatchControlRecordRepository batchControlRecordRepository;

    private List<ProcessSequenceRecord> sequenceList = new ArrayList<>();
    private int currentIndex = 0;

    public ProcessSequenceManager(ProcessSequenceRecordRepository processSequenceRecordRepository,
                                  BatchControlRecordRepository batchControlRecordRepository) {
        this.processSequenceRecordRepository = processSequenceRecordRepository;
        this.batchControlRecordRepository = batchControlRecordRepository;
    }

    /**
     * 1000-INITIALIZE-SEQUENCE: Build sequence from ProcessSequenceRecord entities.
     */
    public void initializeSequence(ProcessSequenceRecord.SequenceType type) {
        log.info("Initializing process sequence for type={}", type);

        sequenceList = processSequenceRecordRepository.findByTypeOrderByStartTimeAsc(type);
        currentIndex = 0;

        log.info("Loaded {} processes in sequence", sequenceList.size());
    }

    /**
     * 2000-GET-NEXT-PROCESS: Find next ready process, check dependencies.
     * Returns the next ProcessSequenceRecord to execute, or empty if none available.
     */
    public Optional<ProcessSequenceRecord> getNextProcess(String processDate) {
        while (currentIndex < sequenceList.size()) {
            ProcessSequenceRecord process = sequenceList.get(currentIndex);
            currentIndex++;

            if (areDependenciesMet(process, processDate)) {
                log.info("Next process: {} ({})", process.getProcessId(), process.getDescription());
                return Optional.of(process);
            }

            log.warn("Dependencies not met for process: {}", process.getProcessId());
        }

        log.info("No more processes in sequence");
        return Optional.empty();
    }

    /**
     * 3000-CHECK-STATUS: Read control status, update sequence table, check completion.
     */
    public boolean checkStatus(String processId, int version, String processDate) {
        log.info("Checking status for process={}", processId);

        List<BatchControlRecord> controlRecords =
                batchControlRecordRepository.findByJobNameAndProcessDate(processId, processDate);

        boolean allDone = controlRecords.stream()
                .allMatch(r -> r.getStatus() == BatchControlStatus.DONE);

        boolean anyError = controlRecords.stream()
                .anyMatch(r -> r.getStatus() == BatchControlStatus.ERROR);

        if (anyError) {
            log.error("Process {} has errors", processId);
            return false;
        }

        log.info("Process {} status: allDone={}", processId, allDone);
        return allDone;
    }

    /**
     * 4000-TERMINATE-SEQUENCE: Check final status of all processes.
     * Returns true if the entire sequence completed successfully.
     */
    public boolean terminateSequence(String processDate) {
        log.info("Terminating sequence, checking final status");

        boolean allComplete = true;
        for (ProcessSequenceRecord process : sequenceList) {
            if (!checkStatus(process.getProcessId(), process.getVersion(), processDate)) {
                allComplete = false;
                log.warn("Process {} did not complete successfully", process.getProcessId());
            }
        }

        log.info("Sequence termination complete: allSuccessful={}", allComplete);
        return allComplete;
    }

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        String processDate = jobExecution.getJobParameters().getString("processDate");
        if (processDate == null) {
            return new FlowExecutionStatus("FAILED");
        }

        Optional<ProcessSequenceRecord> next = getNextProcess(processDate);
        if (next.isPresent()) {
            return new FlowExecutionStatus("CONTINUE");
        }

        return new FlowExecutionStatus("COMPLETED");
    }

    private boolean areDependenciesMet(ProcessSequenceRecord process, String processDate) {
        List<ProcessDependency> deps = process.getDependencies();
        if (deps == null || deps.isEmpty()) {
            return true;
        }

        for (ProcessDependency dep : deps) {
            List<BatchControlRecord> depRecords =
                    batchControlRecordRepository.findByJobNameAndProcessDate(dep.getDepId(), processDate);

            boolean satisfied;
            if ("H".equals(dep.getDepType())) {
                satisfied = depRecords.stream()
                        .anyMatch(r -> r.getStatus() == BatchControlStatus.DONE
                                && (dep.getDepRc() == null || r.getReturnCode() <= dep.getDepRc()));
            } else {
                satisfied = depRecords.isEmpty() || depRecords.stream()
                        .anyMatch(r -> r.getStatus() == BatchControlStatus.DONE);
            }

            if (!satisfied) {
                return false;
            }
        }

        return true;
    }
}
