package com.portfolio.model.dto;

import java.time.LocalDateTime;

public class OnlineErrorInfo {

    private String program;
    private String paragraph;
    private int sqlCode;
    private int cicsResp;
    private int cicsResp2;
    private char severity;
    private String message;
    private char action;
    private String traceId;
    private LocalDateTime timestamp;

    public OnlineErrorInfo() {
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getParagraph() {
        return paragraph;
    }

    public void setParagraph(String paragraph) {
        this.paragraph = paragraph;
    }

    public int getSqlCode() {
        return sqlCode;
    }

    public void setSqlCode(int sqlCode) {
        this.sqlCode = sqlCode;
    }

    public int getCicsResp() {
        return cicsResp;
    }

    public void setCicsResp(int cicsResp) {
        this.cicsResp = cicsResp;
    }

    public int getCicsResp2() {
        return cicsResp2;
    }

    public void setCicsResp2(int cicsResp2) {
        this.cicsResp2 = cicsResp2;
    }

    public char getSeverity() {
        return severity;
    }

    public void setSeverity(char severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public char getAction() {
        return action;
    }

    public void setAction(char action) {
        this.action = action;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
