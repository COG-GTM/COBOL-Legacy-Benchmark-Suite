package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Composite primary key for the ReturnCode entity.
 *
 * DB2 Source: RTNCODES.sql
 *   PRIMARY KEY (TIMESTAMP, PROGRAM_ID)
 */
@Embeddable
public class ReturnCodeId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "log_timestamp", nullable = false)
    @NotNull
    private LocalDateTime logTimestamp;

    @Column(name = "program_id", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String programId;

    public ReturnCodeId() {
    }

    public ReturnCodeId(LocalDateTime logTimestamp, String programId) {
        this.logTimestamp = logTimestamp;
        this.programId = programId;
    }

    public LocalDateTime getLogTimestamp() {
        return logTimestamp;
    }

    public void setLogTimestamp(LocalDateTime logTimestamp) {
        this.logTimestamp = logTimestamp;
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
        ReturnCodeId that = (ReturnCodeId) o;
        return Objects.equals(logTimestamp, that.logTimestamp)
                && Objects.equals(programId, that.programId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logTimestamp, programId);
    }
}
