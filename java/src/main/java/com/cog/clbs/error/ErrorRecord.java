package com.cog.clbs.error;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Error handling record.
 *
 * <p>Java equivalent of the ERROR-HANDLING structure in
 * {@code src/copybook/online/ERRHND.cpy}:
 *
 * <pre>
 *   ERR-PROGRAM    PIC X(8)       -&gt; program
 *   ERR-PARAGRAPH  PIC X(30)      -&gt; paragraph
 *   ERR-SQLCODE    PIC S9(9) COMP -&gt; sqlCode
 *   ERR-SEVERITY   PIC X (F/W/I)  -&gt; severity
 *   ERR-MESSAGE    PIC X(80)      -&gt; message
 *   ERR-ACTION     PIC X (R/C/A)  -&gt; action
 *   ERR-TRACE-ID   PIC X(16)      -&gt; traceId
 *   ERR-TIMESTAMP  PIC X(26)      -&gt; timestamp
 * </pre>
 *
 * <p>The mutable {@code action} field is set by
 * {@link ErrorHandler#determineAction(ErrorRecord)}, mirroring how ERRHNDL
 * updates the COMMAREA before returning to the caller.
 */
public class ErrorRecord {

    private final String program;
    private final String paragraph;
    private final int sqlCode;
    private final ErrorSeverity severity;
    private String message;
    private ErrorAction action;
    private final String traceId;
    private final LocalDateTime timestamp;

    public ErrorRecord(String program, String paragraph, int sqlCode,
                       ErrorSeverity severity, String message) {
        this(program, paragraph, sqlCode, severity, message,
             generateTraceId(), LocalDateTime.now());
    }

    public ErrorRecord(String program, String paragraph, int sqlCode,
                       ErrorSeverity severity, String message,
                       String traceId, LocalDateTime timestamp) {
        this.program = program;
        this.paragraph = paragraph;
        this.sqlCode = sqlCode;
        this.severity = severity;
        this.message = message;
        this.traceId = traceId;
        this.timestamp = timestamp;
    }

    /** P100-INIT-ERROR-HANDLER: assign a trace id when none was supplied. */
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public String getProgram() {
        return program;
    }

    public String getParagraph() {
        return paragraph;
    }

    public int getSqlCode() {
        return sqlCode;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    public ErrorAction getAction() {
        return action;
    }

    void setAction(ErrorAction action) {
        this.action = action;
    }

    public String getTraceId() {
        return traceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
