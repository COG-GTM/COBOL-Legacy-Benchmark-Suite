package com.portfolio.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Composite primary key for ErrorLogRecord entity.
 * Maps DB2 PK: ERROR_TIMESTAMP + PROGRAM_ID.
 */
public class ErrorLogId implements Serializable {

    private LocalDateTime errorTimestamp;
    private String programId;

    public ErrorLogId() {}

    public ErrorLogId(LocalDateTime errorTimestamp, String programId) {
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
        if (o == null || getClass() != o.getClass()) return false;
        ErrorLogId that = (ErrorLogId) o;
        return Objects.equals(errorTimestamp, that.errorTimestamp)
                && Objects.equals(programId, that.programId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorTimestamp, programId);
    }
}
