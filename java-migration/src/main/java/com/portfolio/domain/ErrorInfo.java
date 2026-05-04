package com.portfolio.domain;

import java.time.LocalDateTime;

/**
 * Error information structure - migrated from COBOL ERRHAND.cpy.
 */
public class ErrorInfo {

    private LocalDateTime timestamp;
    private String program;
    private String category;
    private String code;
    private int severity;
    private String text;
    private String details;

    public ErrorInfo() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorInfo(String program, String category, String code, int severity,
                     String text, String details) {
        this();
        this.program = program;
        this.category = category;
        this.code = code;
        this.severity = severity;
        this.text = text;
        this.details = details;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
