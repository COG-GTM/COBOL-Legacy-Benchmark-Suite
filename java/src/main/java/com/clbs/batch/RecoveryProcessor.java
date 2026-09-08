package com.clbs.batch;

import com.clbs.common.ErrorMessage;
import com.clbs.common.ErrorProcessor;
import com.clbs.common.ProgramResult;
import com.clbs.domain.ErrorCodes;
import com.clbs.store.DatasetCatalog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * RCVPRC00 — process recovery handler. LS-RECOVERY-TYPE 'P', 'S' and 'A' select single-process,
 * whole-date and whole-file recovery; the action per process comes from 2110-DETERMINE-ACTION.
 */
@Service
public class RecoveryProcessor {

    public static final String PROGRAM = "RCVPRC00";

    public static final String ERR_DATE_REQUIRED = "Process date required";
    public static final String ERR_INVALID_TYPE = "Invalid recovery type";
    public static final String ERR_ID_REQUIRED = "Process ID required for process recovery";
    public static final String ERR_NOT_FOUND = "Process record not found";
    public static final String ERR_NO_DEFINITION = "Process definition not found";
    public static final String ERR_UPDATE = "Error updating control record";
    public static final String MSG_BYPASSED = "Process bypassed by recovery";
    public static final String MSG_TERMINATED = "Process terminated by recovery";
    public static final String MSG_OK = "Recovery completed successfully";
    public static final String MSG_ERRORS = "Recovery completed with errors";

    /** WS-RECOVERY-ACTION. */
    public enum Action { RESTART, BYPASS, TERMINATE }

    private final DatasetCatalog datasets;
    private final ErrorProcessor errorProcessor;

    public RecoveryProcessor(DatasetCatalog datasets, ErrorProcessor errorProcessor) {
        this.datasets = datasets;
        this.errorProcessor = errorProcessor;
    }

    /** LS-RECOVERY-REQUEST. */
    public record RecoveryRequest(String processDate, String processId, char recoveryType) {
    }

    /** 1200-VALIDATE-REQUEST + 1300-SET-RECOVERY-MODE. */
    public int validate(RecoveryRequest request) {
        if (request.processDate() == null || request.processDate().isBlank()) {
            return error(ERR_DATE_REQUIRED);
        }
        if ("PSA".indexOf(request.recoveryType()) < 0) {
            return error(ERR_INVALID_TYPE);
        }
        if (request.recoveryType() == 'P'
                && (request.processId() == null || request.processId().isBlank())) {
            return error(ERR_ID_REQUIRED);
        }
        return BatchConstants.RC_SUCCESS;
    }

    /** 2000-PROCESS-RECOVERY. */
    public ProgramResult recover(RecoveryRequest request) {
        ProgramResult result = new ProgramResult(PROGRAM);
        int returnCode = validate(request);
        if (returnCode != BatchConstants.RC_SUCCESS) {
            result.setReturnCode(returnCode);
            return result;
        }

        List<BatchControlRecord> targets = switch (request.recoveryType()) {
            case 'P' -> datasets.batchControlFile().all().stream()
                    .filter(record -> record.getJobName().equals(request.processId())
                            && record.getProcessDate().equals(request.processDate()))
                    .toList();
            case 'S' -> datasets.batchControlFile().all().stream()
                    .filter(record -> record.getProcessDate().equals(request.processDate()))
                    .toList();
            default -> List.copyOf(datasets.batchControlFile().all());
        };

        if (targets.isEmpty()) {
            result.setReturnCode(error(request.recoveryType() == 'P'
                    ? ERR_NOT_FOUND : "No processes found for date"));
            return result;
        }

        long restarted = 0;
        long bypassed = 0;
        long terminated = 0;
        for (BatchControlRecord control : targets) {
            Action action = determineAction(control);
            if (action == null) {
                returnCode = error(ERR_NO_DEFINITION);
                continue;
            }
            switch (action) {
                case RESTART -> {
                    restarted++;
                    restart(control);
                }
                case BYPASS -> {
                    bypassed++;
                    bypass(control);
                }
                case TERMINATE -> {
                    terminated++;
                    terminate(control);
                }
            }
            if (!com.clbs.store.FileStatus.SUCCESS
                    .equals(datasets.batchControlFile().rewrite(control))) {
                returnCode = error(ERR_UPDATE);
            }
            result.display(control.getJobName() + " -> " + action);
        }

        // 3100-UPDATE-FINAL-STATUS
        errorProcessor.process(new ErrorMessage(PROGRAM, ErrorCodes.CAT_PROCESSING,
                ErrorCodes.PROCESSING, returnCode,
                returnCode == BatchConstants.RC_SUCCESS ? MSG_OK : MSG_ERRORS, ""));

        result.count("restarted", restarted).count("bypassed", bypassed)
                .count("terminated", terminated);
        result.setReturnCode(returnCode);
        return result;
    }

    /** 2110-DETERMINE-ACTION. */
    public Action determineAction(BatchControlRecord control) {
        ProcessSequenceRecord definition = datasets.processSequenceFile().all().stream()
                .filter(record -> record.getProcessId().equals(control.getJobName()))
                .findFirst()
                .orElse(null);
        if (definition == null) {
            return null;
        }
        if (definition.isRestartable()) {
            return Action.RESTART;
        }
        return control.getRestartCount() > control.getMaxRestarts()
                ? Action.TERMINATE : Action.BYPASS;
    }

    /** 2121-RESTART-PROCESS. */
    private void restart(BatchControlRecord control) {
        control.setStatus(BatchControlRecord.READY);
        control.setRestartCount(control.getRestartCount() + 1);
        control.setAttemptTimestamp(LocalDateTime.now().toString());
    }

    /** 2122-BYPASS-PROCESS. */
    private void bypass(BatchControlRecord control) {
        control.setStatus(BatchControlRecord.DONE);
        control.setReturnCode(BatchConstants.RC_WARNING);
        control.setErrorDesc(MSG_BYPASSED);
    }

    /** 2123-TERMINATE-PROCESS. */
    private void terminate(BatchControlRecord control) {
        control.setStatus(BatchControlRecord.ERROR);
        control.setReturnCode(BatchConstants.RC_ERROR);
        control.setErrorDesc(MSG_TERMINATED);
    }

    /** 9000-ERROR-ROUTINE. */
    private int error(String text) {
        errorProcessor.process(new ErrorMessage(PROGRAM, ErrorCodes.CAT_PROCESSING,
                ErrorCodes.PROCESSING, BatchConstants.RC_ERROR, text, ""));
        return BatchConstants.RC_ERROR;
    }
}
