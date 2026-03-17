package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for BatchControlRecord entity.
 * Migrated from: BCHCTL.cpy BCT-KEY (lines 10-13).
 * Key: job_name (8) + process_date (8) + sequence_no (4)
 */
@Embeddable
public class BatchControlKey implements Serializable {

    @Column(name = "job_name", length = 8, nullable = false)
    private String jobName;

    @Column(name = "process_date", length = 8, nullable = false)
    private String processDate;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    public BatchControlKey() {
    }

    public BatchControlKey(String jobName, String processDate, int sequenceNo) {
        this.jobName = jobName;
        this.processDate = processDate;
        this.sequenceNo = sequenceNo;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getProcessDate() {
        return processDate;
    }

    public void setProcessDate(String processDate) {
        this.processDate = processDate;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(int sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchControlKey that = (BatchControlKey) o;
        return sequenceNo == that.sequenceNo
                && Objects.equals(jobName, that.jobName)
                && Objects.equals(processDate, that.processDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobName, processDate, sequenceNo);
    }
}
