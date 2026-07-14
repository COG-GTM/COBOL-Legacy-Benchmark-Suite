package com.coggtm.migration.phase1.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "errlog")
@IdClass(Errlog.ErrlogId.class)
public class Errlog {

    @Id
    @Column(name = "error_timestamp", nullable = false)
    private LocalDateTime errorTimestamp;

    @Id
    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "error_type", length = 1, nullable = false)
    private String errorType;

    @Column(name = "error_severity", nullable = false)
    private Integer errorSeverity;

    @Column(name = "error_code", length = 8, nullable = false)
    private String errorCode;

    @Column(name = "error_message", length = 200, nullable = false)
    private String errorMessage;

    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    @Column(name = "process_time", nullable = false)
    private LocalTime processTime;

    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    public Errlog() {
    }

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

    public static class ErrlogId implements Serializable {

        private static final long serialVersionUID = 1L;
        private LocalDateTime errorTimestamp;
        private String programId;

        public ErrlogId() {
        }

        public ErrlogId(LocalDateTime errorTimestamp, String programId) {
            this.errorTimestamp = errorTimestamp;
            this.programId = programId;
        }

        public LocalDateTime getErrorTimestamp() { return errorTimestamp; }
        public void setErrorTimestamp(LocalDateTime errorTimestamp) { this.errorTimestamp = errorTimestamp; }

        public String getProgramId() { return programId; }
        public void setProgramId(String programId) { this.programId = programId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ErrlogId)) return false;
            ErrlogId that = (ErrlogId) o;
            return Objects.equals(errorTimestamp, that.errorTimestamp)
                    && Objects.equals(programId, that.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorTimestamp, programId);
        }
    }
}
