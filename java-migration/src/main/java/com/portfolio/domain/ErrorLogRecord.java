package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Error Log Record entity - migrated from COBOL DBTBLS.cpy (ERRLOG-RECORD).
 *
 * COBOL level-88 mappings:
 * - EL-TYPE: S=System, A=Application, D=Data
 * - EL-SEVERITY: 1=Info, 2=Warning, 3=Error, 4=Severe
 */
@Entity
@Table(name = "errlog")
@IdClass(ErrorLogId.class)
public class ErrorLogRecord {

    @Id
    @Column(name = "error_timestamp", nullable = false)
    private LocalDateTime errorTimestamp;

    @Id
    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "error_type", length = 1, nullable = false)
    private String errorType;

    @Column(name = "error_severity", nullable = false)
    private int errorSeverity;

    @Column(name = "error_code", length = 8, nullable = false)
    private String errorCode;

    @Column(name = "error_message", length = 200, nullable = false)
    private String errorMessage;

    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    @Column(name = "process_time", length = 8, nullable = false)
    private String processTime;

    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    public ErrorLogRecord() {
        this.errorTimestamp = LocalDateTime.now();
        this.processDate = LocalDate.now();
    }

    public LocalDateTime getErrorTimestamp() { return errorTimestamp; }
    public void setErrorTimestamp(LocalDateTime errorTimestamp) { this.errorTimestamp = errorTimestamp; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public int getErrorSeverity() { return errorSeverity; }
    public void setErrorSeverity(int errorSeverity) { this.errorSeverity = errorSeverity; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDate getProcessDate() { return processDate; }
    public void setProcessDate(LocalDate processDate) { this.processDate = processDate; }
    public String getProcessTime() { return processTime; }
    public void setProcessTime(String processTime) { this.processTime = processTime; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
}
