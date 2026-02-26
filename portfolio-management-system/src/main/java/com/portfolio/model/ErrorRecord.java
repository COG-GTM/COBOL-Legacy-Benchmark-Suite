package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Error Record entity.
 * Migrated from COBOL ERRHAND copybook and ERRLOG.sql table.
 * Error types: S=System, A=Application, D=Data
 * Severity: 1=Info, 2=Warning, 3=Error, 4=Severe
 */
@Entity
@Table(name = "ERRLOG")
public class ErrorRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ERROR_ID")
    private Long errorId;

    @Column(name = "ERROR_TIMESTAMP", nullable = false)
    private LocalDateTime errorTimestamp;

    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    @Column(name = "ERROR_TYPE", length = 1, nullable = false)
    private String errorType;

    @Column(name = "ERROR_SEVERITY", nullable = false)
    private int errorSeverity;

    @Column(name = "ERROR_CODE", length = 8, nullable = false)
    private String errorCode;

    @Column(name = "ERROR_MESSAGE", length = 200, nullable = false)
    private String errorMessage;

    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDate processDate;

    @Column(name = "PROCESS_TIME", nullable = false)
    private LocalTime processTime;

    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    @Column(name = "ADDITIONAL_INFO", length = 500)
    private String additionalInfo;

    // Error type constants
    public static final String TYPE_SYSTEM = "S";
    public static final String TYPE_APPLICATION = "A";
    public static final String TYPE_DATA = "D";

    // Severity constants
    public static final int SEVERITY_INFO = 1;
    public static final int SEVERITY_WARNING = 2;
    public static final int SEVERITY_ERROR = 3;
    public static final int SEVERITY_SEVERE = 4;

    public ErrorRecord() {}

    // Getters and setters
    public Long getErrorId() { return errorId; }
    public void setErrorId(Long errorId) { this.errorId = errorId; }

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

    public LocalTime getProcessTime() { return processTime; }
    public void setProcessTime(LocalTime processTime) { this.processTime = processTime; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
}
