package com.portfolio.domain;

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

/**
 * JPA entity for the DB2 ERRLOG table ({@code src/database/db2/ERRLOG.sql});
 * the corresponding COBOL host structure is 01 ERRLOG-RECORD in
 * {@code src/copybook/db2/DBTBLS.cpy}.
 *
 * <p>Primary key: (ERROR_TIMESTAMP, PROGRAM_ID).
 */
@Entity
@Table(name = "ERRLOG")
public class ErrorLog {

    @EmbeddedId
    private Key key;

    /** EL-ERROR-TYPE PIC X(1) / ERROR_TYPE CHAR(1) — S=System, A=Application, D=Data. */
    @Column(name = "ERROR_TYPE", length = 1, nullable = false)
    private String errorType;

    /** EL-ERROR-SEVERITY PIC S9(4) COMP / ERROR_SEVERITY INTEGER — 1=Info, 2=Warn, 3=Error, 4=Severe. */
    @Column(name = "ERROR_SEVERITY", nullable = false)
    private int errorSeverity;

    /** EL-ERROR-CODE PIC X(8) / ERROR_CODE CHAR(8). */
    @Column(name = "ERROR_CODE", length = 8, nullable = false)
    private String errorCode;

    /** EL-ERROR-MESSAGE PIC X(200) / ERROR_MESSAGE VARCHAR(200). */
    @Column(name = "ERROR_MESSAGE", length = 200, nullable = false)
    private String errorMessage;

    /** EL-PROCESS-DATE PIC X(10) / PROCESS_DATE DATE. */
    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDate processDate;

    /** EL-PROCESS-TIME PIC X(8) / PROCESS_TIME TIME. */
    @Column(name = "PROCESS_TIME", nullable = false)
    private LocalTime processTime;

    /** EL-USER-ID PIC X(8) / USER_ID CHAR(8). */
    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    /** EL-ADDITIONAL-INFO PIC X(500) / ADDITIONAL_INFO VARCHAR(500). */
    @Column(name = "ADDITIONAL_INFO", length = 500)
    private String additionalInfo;

    /** Composite primary key (ERROR_TIMESTAMP, PROGRAM_ID). */
    @Embeddable
    public static class Key implements Serializable {

        /** EL-ERROR-TIMESTAMP PIC X(26) / ERROR_TIMESTAMP TIMESTAMP. */
        @Column(name = "ERROR_TIMESTAMP", nullable = false)
        private LocalDateTime errorTimestamp;

        /** EL-PROGRAM-ID PIC X(8) / PROGRAM_ID CHAR(8). */
        @Column(name = "PROGRAM_ID", length = 8, nullable = false)
        private String programId;

        public Key() {}

        public Key(LocalDateTime errorTimestamp, String programId) {
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
            if (!(o instanceof Key key)) return false;
            return Objects.equals(errorTimestamp, key.errorTimestamp)
                    && Objects.equals(programId, key.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorTimestamp, programId);
        }
    }

    public Key getKey() { return key; }
    public void setKey(Key key) { this.key = key; }
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
