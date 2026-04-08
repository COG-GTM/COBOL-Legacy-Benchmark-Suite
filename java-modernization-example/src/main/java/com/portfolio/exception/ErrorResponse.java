package com.portfolio.exception;

import java.time.Instant;

/**
 * Structured error response DTO that mirrors the ERR-MESSAGE structure
 * from ERRHAND.cpy (src/copybook/common/ERRHAND.cpy, lines 30-39):
 * <pre>
 *  01  ERR-MESSAGE.
 *      05  ERR-TIMESTAMP.
 *          10  ERR-DATE        PIC X(10).
 *          10  ERR-TIME        PIC X(8).
 *      05  ERR-PROGRAM         PIC X(8).
 *      05  ERR-CATEGORY        PIC X(2).     -- VS=VSAM, VL=Validation, PR=Processing, SY=System
 *      05  ERR-CODE            PIC X(4).
 *      05  ERR-SEVERITY        PIC S9(4) COMP.  -- 0=SUCCESS, 4=WARNING, 8=ERROR, 12=SEVERE, 16=TERMINAL
 *      05  ERR-TEXT            PIC X(80).
 *      05  ERR-DETAILS         PIC X(256).
 * </pre>
 */
public class ErrorResponse {

    private Instant timestamp;
    private String program;
    private String category;
    private String code;
    private String severity;
    private String message;
    private String details;

    public ErrorResponse() {
    }

    public ErrorResponse(Instant timestamp, String program, String category,
                         String code, String severity, String message, String details) {
        this.timestamp = timestamp;
        this.program = program;
        this.category = category;
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
