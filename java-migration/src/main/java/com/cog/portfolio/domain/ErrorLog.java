package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/** DBTBLS.cpy ERRLOG-RECORD / ERRLOG.sql. */
@Entity
@Table(name = "ERRLOG")
public class ErrorLog {

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "ERROR_TIMESTAMP", nullable = false)
        private LocalDateTime errorTimestamp;

        @Column(name = "PROGRAM_ID", length = 8, nullable = false)
        private String programId;

        protected Key() {
        }

        public Key(LocalDateTime errorTimestamp, String programId) {
            this.errorTimestamp = errorTimestamp;
            this.programId = programId;
        }

        public LocalDateTime getErrorTimestamp() {
            return errorTimestamp;
        }

        public String getProgramId() {
            return programId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(errorTimestamp, key.errorTimestamp) && Objects.equals(programId, key.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorTimestamp, programId);
        }
    }

    @EmbeddedId
    private Key id;

    /** ERROR_TYPE: S=System, D=Database, A=Application. */
    @Column(name = "ERROR_TYPE", length = 1, nullable = false)
    private String errorType;

    @Column(name = "ERROR_SEVERITY", nullable = false)
    private int severity;

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

    protected ErrorLog() {
    }

    public ErrorLog(Key id, String errorType, int severity, String errorCode, String errorMessage, String userId) {
        this.id = id;
        this.errorType = errorType;
        this.severity = severity;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.userId = userId;
        this.processDate = id.getErrorTimestamp().toLocalDate();
        this.processTime = id.getErrorTimestamp().toLocalTime();
    }

    public Key getId() {
        return id;
    }

    public String getErrorType() {
        return errorType;
    }

    public int getSeverity() {
        return severity;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDate getProcessDate() {
        return processDate;
    }

    public LocalTime getProcessTime() {
        return processTime;
    }

    public String getUserId() {
        return userId;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
