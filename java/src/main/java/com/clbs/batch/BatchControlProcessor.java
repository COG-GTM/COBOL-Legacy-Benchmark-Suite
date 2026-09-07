package com.clbs.batch;

import com.clbs.common.ErrorMessage;
import com.clbs.common.ErrorProcessor;
import com.clbs.domain.ErrorCodes;
import com.clbs.store.DatasetCatalog;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * BCHCTL00 — batch control processor. LS-FUNCTION 'INIT', 'CHEK', 'UPDT' and 'TERM' become the
 * four public methods, each returning LS-RETURN-CODE.
 *
 * <p>The COBOL source lists 1100/1200/1300/1400, 2200, 3200/3300, 4100 and 4200 as "to be
 * implemented". They are implemented here against the BCHCTL dataset following the semantics
 * BCHCTL.cpy and the sibling programs (PRCSEQ00, RCVPRC00) already establish for the same record.
 */
@Service
public class BatchControlProcessor {

    public static final String PROGRAM = "BCHCTL00";
    public static final String ERR_INVALID_FUNCTION = "Invalid function code";
    public static final String ERR_NOT_FOUND = "Control record not found";
    public static final String ERR_WRITE = "Error updating control record";

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DatasetCatalog datasets;
    private final ErrorProcessor errorProcessor;

    public BatchControlProcessor(DatasetCatalog datasets, ErrorProcessor errorProcessor) {
        this.datasets = datasets;
        this.errorProcessor = errorProcessor;
    }

    /** LS-CONTROL-REQUEST. */
    public record ControlRequest(String function, String jobName, String processDate,
            String sequenceNo) {
    }

    /** 0000-MAIN. */
    public int execute(ControlRequest request) {
        return switch (request.function()) {
            case "INIT" -> initialize(request);
            case "CHEK" -> checkPrerequisites(request);
            case "UPDT" -> updateStatus(request, BatchControlRecord.ACTIVE, BatchConstants.RC_SUCCESS);
            case "TERM" -> terminate(request, BatchConstants.RC_SUCCESS);
            default -> error(ERR_INVALID_FUNCTION);
        };
    }

    /** 1000-PROCESS-INITIALIZE: read the control record and flag it ACTIVE. */
    public int initialize(ControlRequest request) {
        BatchControlRecord record = read(request);
        if (record == null) {
            return error(ERR_NOT_FOUND);
        }
        // 1300-VALIDATE-PROCESS
        if (record.getStatus() == BatchControlRecord.ACTIVE) {
            return error("Process already active");
        }
        // 1400-UPDATE-START-STATUS
        record.setStatus(BatchControlRecord.ACTIVE);
        record.setStartTime(LocalDateTime.now().format(TIME));
        record.setAttemptTimestamp(LocalDateTime.now().toString());
        return rewrite(record);
    }

    /** 2000-CHECK-PREREQUISITES / 2200-CHECK-DEPENDENCIES. */
    public int checkPrerequisites(ControlRequest request) {
        BatchControlRecord record = read(request);
        if (record == null) {
            return error(ERR_NOT_FOUND);
        }
        for (BatchControlRecord.Prerequisite prereq : record.getPrerequisites()) {
            BatchControlRecord dependency = datasets.batchControlFile()
                    .read(keyOf(prereq.jobName(), record.getProcessDate(), prereq.sequenceNo()));
            if (dependency == null || !dependency.isDone()) {
                return BatchConstants.RC_WARNING;
            }
            if (dependency.getReturnCode() > prereq.maxReturnCode()) {
                return BatchConstants.RC_WARNING;
            }
        }
        return BatchConstants.RC_SUCCESS;
    }

    /** 3000-UPDATE-STATUS / 3200 / 3300. */
    public int updateStatus(ControlRequest request, char status, int returnCode) {
        BatchControlRecord record = read(request);
        if (record == null) {
            return error(ERR_NOT_FOUND);
        }
        record.setStatus(status);
        record.setReturnCode(returnCode);
        return rewrite(record);
    }

    /** 4000-PROCESS-TERMINATE / 4100-UPDATE-COMPLETION. */
    public int terminate(ControlRequest request, int returnCode) {
        BatchControlRecord record = read(request);
        if (record == null) {
            return error(ERR_NOT_FOUND);
        }
        record.setStatus(returnCode > BatchConstants.RC_WARNING
                ? BatchControlRecord.ERROR : BatchControlRecord.DONE);
        record.setReturnCode(returnCode);
        record.setEndTime(LocalDateTime.now().format(TIME));
        record.setCompletionTimestamp(LocalDateTime.now().toString());
        return rewrite(record);
    }

    private BatchControlRecord read(ControlRequest request) {
        return datasets.batchControlFile()
                .read(keyOf(request.jobName(), request.processDate(), request.sequenceNo()));
    }

    private int rewrite(BatchControlRecord record) {
        String status = datasets.batchControlFile().rewrite(record);
        return com.clbs.store.FileStatus.SUCCESS.equals(status)
                ? BatchConstants.RC_SUCCESS : error(ERR_WRITE);
    }

    /** 9000-ERROR-ROUTINE. */
    private int error(String text) {
        errorProcessor.process(new ErrorMessage(PROGRAM, ErrorCodes.CAT_PROCESSING,
                ErrorCodes.PROCESSING, BatchConstants.RC_ERROR, text, ""));
        return BatchConstants.RC_ERROR;
    }

    static String keyOf(String jobName, String processDate, String sequenceNo) {
        BatchControlRecord probe = new BatchControlRecord();
        probe.setJobName(jobName);
        probe.setProcessDate(processDate);
        probe.setSequenceNo(sequenceNo);
        return probe.key();
    }
}
