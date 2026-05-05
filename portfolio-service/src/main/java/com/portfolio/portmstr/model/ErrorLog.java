package com.portfolio.portmstr.model;

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
 * Error Log entity.
 * Mapped from COBOL copybook DBTBLS.cpy (ERRLOG-RECORD) and
 * DB2 table ERRLOG (ERRLOG.sql).
 */
@Entity
@Table(name = "ERROR_LOG")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ERROR_TIMESTAMP", nullable = false)
    private LocalDateTime errorTimestamp;

    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    @Column(name = "ERROR_TYPE", length = 1, nullable = false)
    private Character errorType;

    @Column(name = "ERROR_SEVERITY", nullable = false)
    private Integer errorSeverity;

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

    public ErrorLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getErrorTimestamp() {
        return errorTimestamp;
    }

    public void setErrorTimestamp(LocalDateTime errorTimestamp) {
        this.errorTimestamp = errorTimestamp;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public Character getErrorType() {
        return errorType;
    }

    public void setErrorType(Character errorType) {
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
