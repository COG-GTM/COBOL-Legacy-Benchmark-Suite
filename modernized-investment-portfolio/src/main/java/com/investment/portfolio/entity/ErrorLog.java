package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * JPA Entity mapping for the Error Log table (ERRLOG).
 *
 * COBOL Source: DBTBLS.cpy (ERRLOG-RECORD)
 *   EL-ERROR-TIMESTAMP, EL-PROGRAM-ID, EL-ERROR-TYPE, EL-ERROR-SEVERITY,
 *   EL-ERROR-CODE, EL-ERROR-MESSAGE, EL-PROCESS-DATE, EL-PROCESS-TIME,
 *   EL-USER-ID, EL-ADDITIONAL-INFO
 *
 * DB2 Source: ERRLOG.sql
 *   PK: (ERROR_TIMESTAMP, PROGRAM_ID)
 *   Error types: S=System, A=Application, D=Data
 *   Severity levels: 1=Info, 2=Warning, 3=Error, 4=Severe
 */
@Entity
@Table(name = "error_log")
public class ErrorLog {

    @EmbeddedId
    private ErrorLogId id;

    /**
     * Error Type.
     * COBOL: EL-ERROR-TYPE PIC X(1) — S=System, A=Application, D=Data
     * DB2: ERROR_TYPE CHAR(1) NOT NULL
     */
    @Column(name = "error_type", length = 1, nullable = false)
    @NotNull
    @Size(max = 1)
    private String errorType;

    /**
     * Error Severity.
     * COBOL: EL-ERROR-SEVERITY PIC S9(4) COMP — 1=Info, 2=Warning, 3=Error, 4=Severe
     * DB2: ERROR_SEVERITY INTEGER NOT NULL
     */
    @Column(name = "error_severity", nullable = false)
    @NotNull
    private Integer errorSeverity;

    /**
     * Error Code.
     * COBOL: EL-ERROR-CODE PIC X(8)
     * DB2: ERROR_CODE CHAR(8) NOT NULL
     */
    @Column(name = "error_code", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String errorCode;

    /**
     * Error Message.
     * COBOL: EL-ERROR-MESSAGE PIC X(200)
     * DB2: ERROR_MESSAGE VARCHAR(200) NOT NULL
     */
    @Column(name = "error_message", length = 200, nullable = false)
    @NotNull
    @Size(max = 200)
    private String errorMessage;

    /**
     * Processing Date.
     * COBOL: EL-PROCESS-DATE PIC X(10)
     * DB2: PROCESS_DATE DATE NOT NULL
     */
    @Column(name = "process_date", nullable = false)
    @NotNull
    private LocalDate processDate;

    /**
     * Processing Time.
     * COBOL: EL-PROCESS-TIME PIC X(8)
     * DB2: PROCESS_TIME TIME NOT NULL
     */
    @Column(name = "process_time", nullable = false)
    @NotNull
    private LocalTime processTime;

    /**
     * User Identifier.
     * COBOL: EL-USER-ID PIC X(8)
     * DB2: USER_ID CHAR(8) NOT NULL
     */
    @Column(name = "user_id", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String userId;

    /**
     * Additional Information (optional).
     * COBOL: EL-ADDITIONAL-INFO PIC X(500)
     * DB2: ADDITIONAL_INFO VARCHAR(500)
     */
    @Column(name = "additional_info", length = 500)
    @Size(max = 500)
    private String additionalInfo;

    public ErrorLog() {
    }

    // --- Getters and Setters ---

    public ErrorLogId getId() {
        return id;
    }

    public void setId(ErrorLogId id) {
        this.id = id;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public Integer getErrorSeverity() {
        return errorSeverity;
    }

    public void setErrorSeverity(Integer errorSeverity) {
        this.errorSeverity = errorSeverity;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDate getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDate processDate) {
        this.processDate = processDate;
    }

    public LocalTime getProcessTime() {
        return processTime;
    }

    public void setProcessTime(LocalTime processTime) {
        this.processTime = processTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
