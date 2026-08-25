package com.portfolio.model.copybook;

/**
 * Migrated from copybook {@code src/copybook/common/ERRHAND.cpy} (01 ERR-MESSAGE)
 * plus its categories, return codes, and VSAM status constants. The online
 * variant {@code src/copybook/online/ERRHND.cpy} shares the same intent and is
 * covered by this class for batch purposes.
 */
public class ErrorMessage {

    /** ERR-DATE PIC X(10). */
    private String date;

    /** ERR-TIME PIC X(8). */
    private String time;

    /** ERR-PROGRAM PIC X(8). */
    private String program;

    /** ERR-CATEGORY PIC X(2) — VS=VSAM, VL=Validation, PR=Processing, SY=System. */
    private String category;

    /** ERR-CODE PIC X(4). */
    private String code;

    /** ERR-SEVERITY PIC S9(4) COMP — 0/4/8/12/16. */
    private int severity;

    /** ERR-TEXT PIC X(80). */
    private String text;

    /** ERR-DETAILS PIC X(256). */
    private String details;

    // Error categories (ERR-CATEGORIES, PIC X(2))
    public static final String CAT_VSAM = "VS";
    public static final String CAT_VALIDATION = "VL";
    public static final String CAT_PROCESSING = "PR";
    public static final String CAT_SYSTEM = "SY";

    // Standard return codes (ERR-RETURN-CODES, PIC S9(4) COMP)
    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_TERMINAL = 16;

    // VSAM file statuses (ERR-VSAM-STATUSES, PIC X(2)) — replaced by exceptions in Java
    public static final String VSAM_SUCCESS = "00";
    public static final String VSAM_EOF = "10";
    public static final String VSAM_DUPKEY = "22";
    public static final String VSAM_NOTFND = "23";

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
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
