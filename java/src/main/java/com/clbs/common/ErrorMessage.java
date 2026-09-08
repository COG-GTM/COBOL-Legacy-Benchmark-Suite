package com.clbs.common;

/** ERRHAND.cpy ERR-MESSAGE. */
public class ErrorMessage {

    private String date = "";
    private String time = "";
    private String programId = "";
    private String category = "";
    private String code = "";
    private int severity;
    private String text = "";
    private String details = "";

    public ErrorMessage() {
    }

    public ErrorMessage(String programId, String category, String code, int severity, String text,
            String details) {
        this.programId = programId;
        this.category = category;
        this.code = code;
        this.severity = severity;
        this.text = text;
        this.details = details;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
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

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
