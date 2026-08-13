package com.ipms.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/** ERRLOG table from {@code src/database/db2/ERRLOG.sql}. */
@Entity
@Table(name = "ERRLOG")
@IdClass(ErrorLog.Key.class)
public class ErrorLog {

    @Id
    @Column(name = "ERROR_TIMESTAMP", nullable = false)
    private LocalDateTime errorTimestamp;

    @Id
    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    /** Error Type (S=System, A=Application, D=Data). */
    @Column(name = "ERROR_TYPE", length = 1, nullable = false)
    private String errorType;

    /** Error Severity (1=Info, 2=Warning, 3=Error, 4=Severe). */
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

    public static class Key implements Serializable {
        private LocalDateTime errorTimestamp;
        private String programId;

        public Key() {
        }

        public Key(LocalDateTime errorTimestamp, String programId) {
            this.errorTimestamp = errorTimestamp;
            this.programId = programId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(errorTimestamp, key.errorTimestamp)
                    && Objects.equals(programId, key.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorTimestamp, programId);
        }
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

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public int getErrorSeverity() {
        return errorSeverity;
    }

    public void setErrorSeverity(int errorSeverity) {
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
