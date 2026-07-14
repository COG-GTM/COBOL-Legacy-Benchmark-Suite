package com.coggtm.migration.phase1.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "history_record")
@IdClass(HistoryRecord.HistoryRecordId.class)
public class HistoryRecord {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "history_date", nullable = false)
    private LocalDate historyDate;

    @Id
    @Column(name = "history_time", nullable = false)
    private LocalTime historyTime;

    @Id
    @Column(name = "sequence_no", length = 4, nullable = false)
    private String sequenceNo;

    @Column(name = "record_type", length = 2, nullable = false)
    private String recordType;

    @Column(name = "action_code", length = 1, nullable = false)
    private String actionCode;

    @Column(name = "before_image", length = 400, nullable = false)
    private String beforeImage;

    @Column(name = "after_image", length = 400, nullable = false)
    private String afterImage;

    @Column(name = "reason_code", length = 4, nullable = false)
    private String reasonCode;

    @Column(name = "process_timestamp", nullable = false)
    private LocalDateTime processTimestamp;

    @Column(name = "process_user", length = 8, nullable = false)
    private String processUser;

    @Column(name = "filler", length = 50)
    private String filler;

    public HistoryRecord() {
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public LocalDate getHistoryDate() { return historyDate; }
    public void setHistoryDate(LocalDate historyDate) { this.historyDate = historyDate; }

    public LocalTime getHistoryTime() { return historyTime; }
    public void setHistoryTime(LocalTime historyTime) { this.historyTime = historyTime; }

    public String getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(String sequenceNo) { this.sequenceNo = sequenceNo; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getActionCode() { return actionCode; }
    public void setActionCode(String actionCode) { this.actionCode = actionCode; }

    public String getBeforeImage() { return beforeImage; }
    public void setBeforeImage(String beforeImage) { this.beforeImage = beforeImage; }

    public String getAfterImage() { return afterImage; }
    public void setAfterImage(String afterImage) { this.afterImage = afterImage; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public LocalDateTime getProcessTimestamp() { return processTimestamp; }
    public void setProcessTimestamp(LocalDateTime processTimestamp) { this.processTimestamp = processTimestamp; }

    public String getProcessUser() { return processUser; }
    public void setProcessUser(String processUser) { this.processUser = processUser; }

    public String getFiller() { return filler; }
    public void setFiller(String filler) { this.filler = filler; }

    public static class HistoryRecordId implements Serializable {

        private static final long serialVersionUID = 1L;
        private String portfolioId;
        private LocalDate historyDate;
        private LocalTime historyTime;
        private String sequenceNo;

        public HistoryRecordId() {
        }

        public HistoryRecordId(String portfolioId, LocalDate historyDate, LocalTime historyTime, String sequenceNo) {
            this.portfolioId = portfolioId;
            this.historyDate = historyDate;
            this.historyTime = historyTime;
            this.sequenceNo = sequenceNo;
        }

        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

        public LocalDate getHistoryDate() { return historyDate; }
        public void setHistoryDate(LocalDate historyDate) { this.historyDate = historyDate; }

        public LocalTime getHistoryTime() { return historyTime; }
        public void setHistoryTime(LocalTime historyTime) { this.historyTime = historyTime; }

        public String getSequenceNo() { return sequenceNo; }
        public void setSequenceNo(String sequenceNo) { this.sequenceNo = sequenceNo; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HistoryRecordId)) return false;
            HistoryRecordId that = (HistoryRecordId) o;
            return Objects.equals(portfolioId, that.portfolioId)
                    && Objects.equals(historyDate, that.historyDate)
                    && Objects.equals(historyTime, that.historyTime)
                    && Objects.equals(sequenceNo, that.sequenceNo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, historyDate, historyTime, sequenceNo);
        }
    }
}
