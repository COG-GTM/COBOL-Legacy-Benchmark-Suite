package com.cobolbenchmark.batch;

import com.cobolbenchmark.common.BatchConstants;
import com.cobolbenchmark.common.ReturnCodeManager;
import com.cobolbenchmark.db.BatchControlRepository;
import com.cobolbenchmark.db.ProcessSequenceRepository;
import com.cobolbenchmark.model.BatchControlRecord;
import com.cobolbenchmark.model.BatchStatus;
import com.cobolbenchmark.model.Dependency;
import com.cobolbenchmark.model.DependencyType;
import com.cobolbenchmark.model.ProcessSequenceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Process Sequence Job - migrated from PRCSEQ00.cbl.
 * Operations: INIT, NEXT, STAT, TERM.
 * Process table max 100 entries, dependency checking with return codes.
 * Replaces VSAM START KEY >= / READ NEXT with database queries.
 */
@Service
public class ProcessSequenceJob {

    private static final Logger logger = LoggerFactory.getLogger(ProcessSequenceJob.class);

    private final ProcessSequenceRepository processSequenceRepository;
    private final BatchControlRepository batchControlRepository;
    private final ReturnCodeManager returnCodeManager = new ReturnCodeManager();

    // Process table - max 100 entries from PRCSEQ00.cbl
    private final List<ProcessSequenceRecord> processTable = new ArrayList<>();

    public ProcessSequenceJob(ProcessSequenceRepository processSequenceRepository,
                              BatchControlRepository batchControlRepository) {
        this.processSequenceRepository = processSequenceRepository;
        this.batchControlRepository = batchControlRepository;
    }

    /**
     * INIT function - initialize process sequence.
     * From PRCSEQ00.cbl: P200-INIT-SEQUENCE paragraph.
     * Replaces VSAM START/READ NEXT with database query.
     */
    public void initialize(String startKey) {
        logger.info("Initializing process sequence from key: {}", startKey);
        returnCodeManager.reset();
        processTable.clear();

        List<ProcessSequenceRecord> records = processSequenceRepository.findByProcessIdGreaterThanEqual(startKey);

        int count = 0;
        for (ProcessSequenceRecord record : records) {
            if (count >= BatchConstants.MAX_PROCESS_TABLE_ENTRIES) {
                logger.warn("Process table limit reached ({})", BatchConstants.MAX_PROCESS_TABLE_ENTRIES);
                break;
            }
            processTable.add(record);
            count++;
        }

        logger.info("Loaded {} processes into process table", processTable.size());
        returnCodeManager.setReturnCode(BatchConstants.RC_SUCCESS, "INIT");
    }

    /**
     * NEXT function - get next process to execute.
     * From PRCSEQ00.cbl: P300-NEXT-PROCESS paragraph.
     * Checks scheduling and dependencies.
     */
    public ProcessSequenceRecord getNextProcess(String processDate) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        for (ProcessSequenceRecord process : processTable) {
            // Check if process is scheduled for today
            Set<DayOfWeek> activeDays = process.getActiveDaysOfWeek();
            if (!activeDays.contains(today)) {
                continue;
            }

            // Check dependencies - 2210-CHECK-DEP-STATUS
            if (checkDependencies(process, processDate)) {
                return process;
            }
        }

        return null; // No eligible process found
    }

    /**
     * Check dependency status - from PRCSEQ00.cbl: 2210-CHECK-DEP-STATUS paragraph.
     * Hard dependencies must be DONE with acceptable return code.
     * Soft dependencies are checked but don't block.
     */
    public boolean checkDependencies(ProcessSequenceRecord process, String processDate) {
        List<Dependency> dependencies = process.getDependencies();

        for (Dependency dep : dependencies) {
            DependencyType type = dep.getDependencyType();

            // Look up the dependency's batch control record
            List<BatchControlRecord> depRecords = batchControlRepository
                    .findByJobNameAndProcessDate(dep.getDepId(), processDate);

            boolean depSatisfied = false;
            for (BatchControlRecord depRecord : depRecords) {
                if (BatchStatus.DONE.getCode().equals(depRecord.getStatus())
                        && depRecord.getReturnCode() <= dep.getDepRc()) {
                    depSatisfied = true;
                    break;
                }
            }

            if (!depSatisfied) {
                if (type == DependencyType.HARD) {
                    logger.debug("Hard dependency {} not satisfied for process {}",
                            dep.getDepId(), process.getProcessId());
                    return false;
                } else {
                    logger.debug("Soft dependency {} not satisfied for process {} (continuing)",
                            dep.getDepId(), process.getProcessId());
                }
            }
        }

        return true;
    }

    /**
     * STAT function - get process sequence status.
     * From PRCSEQ00.cbl: P400-GET-STATUS paragraph.
     */
    public int getStatus() {
        return returnCodeManager.getHighestReturnCode();
    }

    /**
     * TERM function - terminate process sequence.
     * From PRCSEQ00.cbl: P500-TERMINATE paragraph.
     */
    public int terminate() {
        logger.info("Terminating process sequence. Highest RC: {}", returnCodeManager.getHighestReturnCode());
        int rc = returnCodeManager.getHighestReturnCode();
        processTable.clear();
        return rc;
    }

    public List<ProcessSequenceRecord> getProcessTable() {
        return new ArrayList<>(processTable);
    }
}
