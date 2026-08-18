package com.cog.clbs.error;

import com.cog.clbs.program.ReturnCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Centralized error handler.
 *
 * <p>Java equivalent of {@code src/templates/error/error-handling.cbl}
 * (batch error handling / RC 0-16 management) and the online handler
 * {@code src/programs/online/ERRHNDL.cbl} (logging, message formatting,
 * action determination):
 *
 * <pre>
 *   8010/8020/8030-HANDLE-*  -&gt; handle(record): counts by severity
 *   8100-LOG-ERROR / P200    -&gt; error log (pluggable sink; default in-memory)
 *   P300-FORMAT-MESSAGE      -&gt; formatMessage(record)
 *   P400-DETERMINE-ACTION    -&gt; determineAction(record)
 *   8000-CHECK-FINAL-STATUS  -&gt; finalReturnCode()
 * </pre>
 */
public class ErrorHandler {

    private final List<ErrorRecord> errorLog = new ArrayList<>();
    private final Consumer<ErrorRecord> logSink;

    private int warningCount;
    private int errorCount;
    private int severeCount;
    private boolean abendRequested;

    public ErrorHandler() {
        this(null);
    }

    /** @param logSink optional external log destination (the ERRLOG INSERT equivalent) */
    public ErrorHandler(Consumer<ErrorRecord> logSink) {
        this.logSink = logSink;
    }

    /**
     * Processes one error: counts it by severity, formats the message,
     * logs it, and determines the recovery action.
     */
    public ErrorAction handle(ErrorRecord record) {
        switch (record.getSeverity()) {
            case WARNING -> warningCount++;
            case INFO -> { /* informational: no count impact */ }
            case FATAL -> severeCount++;
        }
        formatMessage(record);
        logError(record);
        return determineAction(record);
    }

    /** Records a non-fatal application error (8020-HANDLE-ERROR pattern). */
    public void recordError() {
        errorCount++;
    }

    /**
     * P300-FORMAT-MESSAGE: 'Error in {program} - {message} ({traceId})'.
     */
    public String formatMessage(ErrorRecord record) {
        String formatted = "Error in " + record.getProgram().trim()
                + " - " + record.getMessage()
                + " (" + record.getTraceId() + ")";
        record.setMessage(formatted);
        return formatted;
    }

    /** 8100-LOG-ERROR / P200-LOG-ERROR: append to the error log. */
    private void logError(ErrorRecord record) {
        errorLog.add(record);
        if (logSink != null) {
            logSink.accept(record);
        }
    }

    /**
     * P400-DETERMINE-ACTION: FATAL -&gt; ABEND, WARNING/INFO -&gt; CONTINUE,
     * otherwise RETURN.
     */
    public ErrorAction determineAction(ErrorRecord record) {
        ErrorAction action = switch (record.getSeverity()) {
            case FATAL -> ErrorAction.ABEND;
            case WARNING, INFO -> ErrorAction.CONTINUE;
        };
        if (action == ErrorAction.ABEND) {
            abendRequested = true;
        }
        record.setAction(action);
        return action;
    }

    /**
     * 8000-CHECK-FINAL-STATUS: derive the final RC 0-16 return code from
     * the accumulated severity counts (severe &gt; error &gt; warning &gt; success).
     */
    public ReturnCode finalReturnCode() {
        if (severeCount > 0) {
            return ReturnCode.SEVERE;
        }
        if (errorCount > 0) {
            return ReturnCode.ERROR;
        }
        if (warningCount > 0) {
            return ReturnCode.WARNING;
        }
        return ReturnCode.SUCCESS;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getSevereCount() {
        return severeCount;
    }

    /** WS-ABEND-FLAG: set when a fatal error requested an abend (8040 pattern). */
    public boolean isAbendRequested() {
        return abendRequested;
    }

    public List<ErrorRecord> getErrorLog() {
        return Collections.unmodifiableList(errorLog);
    }
}
