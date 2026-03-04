package com.investment.portfolio.batch;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;
import com.investment.portfolio.model.BatchControlRecord;
import com.investment.portfolio.model.BatchControlRecord.BatchStatus;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Batch Controller (BCHCTL00) - Java equivalent of BCHCTL00.cbl
 *
 * Original COBOL: src/programs/batch/BCHCTL00.cbl
 *
 * Responsibilities:
 * - Manages batch job control lifecycle
 * - Checks prerequisites before job execution
 * - Updates job status during processing
 * - Handles job initialization and termination
 *
 * This is a callable subprogram. The COBOL version is called via
 * CALL 'BCHCTL00' USING BCT-FUNCTION BCT-RECORD.
 * In Java this maps to a service class with methods for each function code.
 *
 * Function codes (from LINKAGE SECTION):
 * - INIT: Initialize batch control for a new job
 * - CHEK: Check prerequisites for the job
 * - UPDT: Update job status during processing
 * - TERM: Terminate and finalize the job
 */
public class BatchController {

    private static final Logger LOGGER = Logger.getLogger(BatchController.class.getName());
    private static final String PROGRAM_ID = "BCHCTL00";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Function codes matching COBOL BCT-FUNCTION field */
    public enum Function {
        INIT, CHEK, UPDT, TERM
    }

    private final Path controlFilePath;
    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;

    /** In-memory control record cache - maps to VSAM KSDS indexed file */
    private final Map<String, BatchControlRecord> controlRecords;

    public BatchController(Path controlFilePath) {
        this.controlFilePath = controlFilePath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
        this.controlRecords = new HashMap<>();
        loadControlRecords();
    }

    /**
     * Main entry point - maps to COBOL PROCEDURE DIVISION USING.
     *
     * EVALUATE BCT-FUNCTION
     *   WHEN 'INIT' PERFORM 1000-PROCESS-INITIALIZE
     *   WHEN 'CHEK' PERFORM 2000-CHECK-PREREQUISITES
     *   WHEN 'UPDT' PERFORM 3000-UPDATE-STATUS
     *   WHEN 'TERM' PERFORM 4000-PROCESS-TERMINATE
     *   WHEN OTHER  MOVE 16 TO BCT-RETURN-CODE
     * END-EVALUATE
     */
    public int execute(Function function, BatchControlRecord record) {
        LOGGER.info(PROGRAM_ID + " - Function: " + function + " Job: " + record.getJobName());

        try {
            switch (function) {
                case INIT:
                    processInitialize(record);
                    break;
                case CHEK:
                    checkPrerequisites(record);
                    break;
                case UPDT:
                    updateStatus(record);
                    break;
                case TERM:
                    processTerminate(record);
                    break;
                default:
                    returnCode.setCode(ReturnCode.CRITICAL);
                    record.setReturnCode(ReturnCode.CRITICAL);
                    record.setErrorDescription("Invalid function code");
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error", e);
            returnCode.setCode(ReturnCode.SEVERE);
            record.setReturnCode(ReturnCode.SEVERE);
            record.setErrorDescription(e.getMessage());
        }

        return record.getReturnCode();
    }

    /**
     * 1000-PROCESS-INITIALIZE: Initialize a new batch job control entry.
     *
     * Maps to:
     *   MOVE 'A' TO BCT-STATUS
     *   MOVE CURRENT-DATE TO BCT-START-TIME
     *   WRITE BCT-RECORD
     */
    private void processInitialize(BatchControlRecord record) {
        LOGGER.info("Initializing job: " + record.getJobName());

        record.setStatus(BatchStatus.ACTIVE);
        record.setStartTime(LocalDateTime.now().format(TIME_FMT));
        record.setProcessDate(LocalDate.now().format(DATE_FMT));
        record.setReturnCode(ReturnCode.SUCCESS);
        record.setRestartCount(0);
        record.setAttemptTimestamp(LocalDateTime.now());

        // Write control record - maps to WRITE BCT-RECORD
        controlRecords.put(record.getCompositeKey(), record);
        saveControlRecords();

        LOGGER.info("Job initialized: " + record.getJobName());
    }

    /**
     * 2000-CHECK-PREREQUISITES: Verify all prerequisite jobs completed.
     *
     * Maps to:
     *   PERFORM VARYING WS-PREREQ-IDX FROM 1 BY 1
     *     UNTIL WS-PREREQ-IDX > BCT-PREREQ-COUNT
     *     READ BATCH-CONTROL-FILE INTO WS-PREREQ-RECORD
     *       KEY IS BCT-PREREQ-NAME(WS-PREREQ-IDX)
     *     IF BCT-STATUS NOT = 'D'
     *       OR BCT-RETURN-CODE > BCT-PREREQ-RC(WS-PREREQ-IDX)
     *       MOVE 'W' TO BCT-STATUS
     *       MOVE 8 TO BCT-RETURN-CODE
     *     END-IF
     *   END-PERFORM
     */
    private void checkPrerequisites(BatchControlRecord record) {
        LOGGER.info("Checking prerequisites for job: " + record.getJobName());

        if (record.getPrerequisiteCount() == 0) {
            record.setReturnCode(ReturnCode.SUCCESS);
            LOGGER.info("No prerequisites for job: " + record.getJobName());
            return;
        }

        boolean allMet = true;

        for (int i = 0; i < record.getPrerequisiteCount()
                && i < record.getPrerequisites().size(); i++) {
            BatchControlRecord.PrerequisiteJob prereq = record.getPrerequisites().get(i);

            // Look up the prerequisite job in control records
            BatchControlRecord prereqRecord = findLatestJob(prereq.getJobName());

            if (prereqRecord == null) {
                LOGGER.warning("Prerequisite job not found: " + prereq.getJobName());
                allMet = false;
                continue;
            }

            // Check that prerequisite completed successfully
            if (prereqRecord.getStatus() != BatchStatus.DONE) {
                LOGGER.warning("Prerequisite not complete: " + prereq.getJobName()
                        + " Status: " + prereqRecord.getStatus());
                allMet = false;
                continue;
            }

            // Check return code is within acceptable range
            if (prereqRecord.getReturnCode() > prereq.getRequiredReturnCode()) {
                LOGGER.warning("Prerequisite return code too high: " + prereq.getJobName()
                        + " RC: " + prereqRecord.getReturnCode()
                        + " Max: " + prereq.getRequiredReturnCode());
                allMet = false;
            }
        }

        if (allMet) {
            record.setReturnCode(ReturnCode.SUCCESS);
            LOGGER.info("All prerequisites met for job: " + record.getJobName());
        } else {
            record.setStatus(BatchStatus.WAITING);
            record.setReturnCode(ReturnCode.ERROR);
            record.setErrorDescription("Prerequisites not met");
            LOGGER.warning("Prerequisites NOT met for job: " + record.getJobName());
        }
    }

    /**
     * 3000-UPDATE-STATUS: Update batch control status during processing.
     *
     * Maps to:
     *   REWRITE BCT-RECORD
     */
    private void updateStatus(BatchControlRecord record) {
        LOGGER.info("Updating status for job: " + record.getJobName()
                + " to " + record.getStatus());

        // Set return code BEFORE persisting, so the file reflects the final state
        // (matches COBOL REWRITE which writes the record after all fields are set)
        record.setReturnCode(ReturnCode.SUCCESS);
        controlRecords.put(record.getCompositeKey(), record);
        saveControlRecords();
    }

    /**
     * 4000-PROCESS-TERMINATE: Finalize job, record completion.
     *
     * Maps to:
     *   MOVE 'D' TO BCT-STATUS
     *   MOVE CURRENT-DATE TO BCT-END-TIME
     *   MOVE CURRENT TIMESTAMP TO BCT-COMPLETE-TS
     *   REWRITE BCT-RECORD
     */
    private void processTerminate(BatchControlRecord record) {
        LOGGER.info("Terminating job: " + record.getJobName());

        record.setStatus(BatchStatus.DONE);
        record.setEndTime(LocalDateTime.now().format(TIME_FMT));
        record.setCompleteTimestamp(LocalDateTime.now());

        controlRecords.put(record.getCompositeKey(), record);
        saveControlRecords();

        LOGGER.info("Job terminated: " + record.getJobName()
                + " RC: " + record.getReturnCode());
    }

    /**
     * Finds the most recent control record for a given job name.
     */
    private BatchControlRecord findLatestJob(String jobName) {
        return controlRecords.values().stream()
                .filter(r -> jobName.equals(r.getJobName()))
                .reduce((a, b) -> {
                    int cmp = a.getProcessDate().compareTo(b.getProcessDate());
                    return cmp >= 0 ? a : b;
                })
                .orElse(null);
    }

    /**
     * Loads control records from the control file.
     * Maps to OPEN I-O BATCH-CONTROL-FILE.
     */
    private void loadControlRecords() {
        try (FileHandler file = new FileHandler(controlFilePath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(file.openInput())) {
                return; // File doesn't exist yet
            }

            String line;
            while ((line = file.readLine()) != null) {
                BatchControlRecord rec = parseControlRecord(line);
                if (rec != null) {
                    controlRecords.put(rec.getCompositeKey(), rec);
                }
            }
        } catch (Exception e) {
            LOGGER.info("No existing control file; starting fresh");
        }
    }

    /**
     * Saves all control records back to the file.
     * Maps to WRITE/REWRITE BCT-RECORD.
     */
    private void saveControlRecords() {
        try (FileHandler file = new FileHandler(controlFilePath)) {
            file.openOutput();
            for (BatchControlRecord rec : controlRecords.values()) {
                file.writeLine(formatControlRecord(rec));
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E100", "Error saving control records", e);
        }
    }

    private BatchControlRecord parseControlRecord(String line) {
        try {
            if (line == null || line.length() < 20) return null;
            BatchControlRecord rec = new BatchControlRecord();
            rec.setJobName(line.substring(0, 8).trim());
            rec.setProcessDate(line.substring(8, 16).trim());
            rec.setSequenceNumber(Integer.parseInt(line.substring(16, 20).trim()));
            if (line.length() > 20) rec.setStatus(BatchStatus.fromCode(line.charAt(20)));
            if (line.length() > 28) rec.setStepName(line.substring(21, 29).trim());
            if (line.length() > 36) rec.setProgramName(line.substring(29, 37).trim());
            if (line.length() > 41) rec.setReturnCode(Integer.parseInt(line.substring(37, 41).trim()));
            return rec;
        } catch (Exception e) {
            return null;
        }
    }

    private String formatControlRecord(BatchControlRecord rec) {
        return String.format("%-8s%-8s%04d%c%-8s%-8s%4d",
                rec.getJobName(),
                rec.getProcessDate(),
                rec.getSequenceNumber(),
                rec.getStatus().getCode(),
                rec.getStepName() != null ? rec.getStepName() : "",
                rec.getProgramName() != null ? rec.getProgramName() : "",
                rec.getReturnCode());
    }
}
