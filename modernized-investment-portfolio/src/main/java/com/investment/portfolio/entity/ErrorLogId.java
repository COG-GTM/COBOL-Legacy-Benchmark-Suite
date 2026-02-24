package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Composite primary key for the ErrorLog entity.
 *
 * DB2 Source: ERRLOG.sql
 *   PRIMARY KEY (ERROR_TIMESTAMP, PROGRAM_ID)
 */
@Embeddable
public class ErrorLogId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "error_timestamp", nullable = false)
    @NotNull
    private LocalDateTime errorTimestamp;

    @Column(name = "program_id", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String programId;

    public ErrorLogId() {
    }

    public ErrorLogId(LocalDateTime errorTimestamp, String programId) {
        this.errorTimestamp = errorTimestamp;
        this.programId = programId;
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
