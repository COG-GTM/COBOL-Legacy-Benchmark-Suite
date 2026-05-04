package com.portfolio.entity;

import java.io.Serializable;
import java.util.Objects;

public class BatchControlId implements Serializable {

    private String jobName;
    private String processDate;
    private int sequenceNo;

    public BatchControlId() {
    }

    public BatchControlId(String jobName, String processDate, int sequenceNo) {
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
        BatchControlId that = (BatchControlId) o;
        return sequenceNo == that.sequenceNo
                && Objects.equals(jobName, that.jobName)
                && Objects.equals(processDate, that.processDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobName, processDate, sequenceNo);
    }
}
