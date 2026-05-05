package com.portfolio.portmstr.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite key for BatchCheckpoint.
 * Maps COBOL CKPRST.cpy CKR-KEY: CKR-PROGRAM-ID + CKR-RUN-DATE.
 */
public class BatchCheckpointId implements Serializable {

    private String programId;
    private LocalDate runDate;

    public BatchCheckpointId() {
    }

    public BatchCheckpointId(String programId, LocalDate runDate) {
        this.programId = programId;
        this.runDate = runDate;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchCheckpointId that = (BatchCheckpointId) o;
        return Objects.equals(programId, that.programId) &&
                Objects.equals(runDate, that.runDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(programId, runDate);
    }
}
