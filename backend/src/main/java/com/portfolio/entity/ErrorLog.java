package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Error Log entity - migrated from DB2 ERRLOG table.
 * Source: src/database/db2/ERRLOG.sql
 *
 * Error types: 'S'=System, 'A'=Application, 'D'=Data
 * Severity: 1=Info, 2=Warning, 3=Error, 4=Severe
 */
@Entity
@Table(name = "error_log")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_timestamp", nullable = false)
    private LocalDateTime errorTimestamp = LocalDateTime.now();

    @Column(name = "program_id", length = 8, nullable = false)
    @NotBlank
    private String programId;

    @Column(name = "error_type", length = 1, nullable = false)
    @NotBlank
    private String errorType;

    @Column(name = "error_severity", nullable = false)
    @NotNull
    private Integer errorSeverity;

    @Column(name = "error_code", length = 8, nullable = false)
    @NotBlank
    private String errorCode;

    @Column(name = "error_message", length = 200, nullable = false)
    @NotBlank
    private String errorMessage;

    @Column(name = "process_date", nullable = false)
    @NotNull
    private LocalDate processDate;

    @Column(name = "process_time")
    private LocalTime processTime;

    @Column(name = "user_id", length = 8, nullable = false)
    @NotBlank
    private String userId;

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    public ErrorLog() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getErrorTimestamp() { return errorTimestamp; }
    public void setErrorTimestamp(LocalDateTime errorTimestamp) { this.errorTimestamp = errorTimestamp; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public Integer getErrorSeverity() { return errorSeverity; }
    public void setErrorSeverity(Integer errorSeverity) { this.errorSeverity = errorSeverity; }
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
