package com.portfolio.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Maps COBOL HISTREC.cpy HISTORY-RECORD.
 */
@Entity
@Table(name = "history_record")
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "hist_date", length = 8)
    private String histDate;

    @Column(name = "hist_time", length = 6)
    private String histTime;

    @Column(name = "seq_no", length = 4)
    private String sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", length = 12)
    private HistoryRecordType recordType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_code", length = 8)
    private HistoryActionCode actionCode;

    @Column(name = "before_image", length = 400)
    private String beforeImage;

    @Column(name = "after_image", length = 400)
    private String afterImage;

    @Column(name = "reason_code", length = 4)
    private String reasonCode;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Column(name = "process_user", length = 8)
    private String processUser;

    protected HistoryRecord() { /* JPA */ }

    private HistoryRecord(Builder builder) {
        this.portfolioId = builder.portfolioId;
        this.histDate = builder.histDate;
        this.histTime = builder.histTime;
        this.sequenceNumber = builder.sequenceNumber;
        this.recordType = builder.recordType;
        this.actionCode = builder.actionCode;
        this.beforeImage = builder.beforeImage;
        this.afterImage = builder.afterImage;
        this.reasonCode = builder.reasonCode;
        this.processDate = builder.processDate;
        this.processUser = builder.processUser;
    }

    public Long getId() { return id; }
    public String getPortfolioId() { return portfolioId; }
    public String getHistDate() { return histDate; }
    public String getHistTime() { return histTime; }
    public String getSequenceNumber() { return sequenceNumber; }
    public HistoryRecordType getRecordType() { return recordType; }
    public HistoryActionCode getActionCode() { return actionCode; }
    public String getBeforeImage() { return beforeImage; }
    public String getAfterImage() { return afterImage; }
    public String getReasonCode() { return reasonCode; }
    public LocalDateTime getProcessDate() { return processDate; }
    public String getProcessUser() { return processUser; }

    public static Builder builder(String portfolioId, HistoryRecordType recordType,
                                  HistoryActionCode actionCode) {
        return new Builder(portfolioId, recordType, actionCode);
    }

    public static class Builder {
        private final String portfolioId;
        private final HistoryRecordType recordType;
        private final HistoryActionCode actionCode;
        private String histDate;
        private String histTime;
        private String sequenceNumber;
        private String beforeImage;
        private String afterImage;
        private String reasonCode;
        private LocalDateTime processDate;
        private String processUser;

        private Builder(String portfolioId, HistoryRecordType recordType,
                        HistoryActionCode actionCode) {
            this.portfolioId = portfolioId;
            this.recordType = recordType;
            this.actionCode = actionCode;
            LocalDateTime now = LocalDateTime.now();
            this.histDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            this.histTime = now.format(DateTimeFormatter.ofPattern("HHmmss"));
            this.processDate = now;
        }

        public Builder histDate(String histDate) { this.histDate = histDate; return this; }
        public Builder histTime(String histTime) { this.histTime = histTime; return this; }
        public Builder sequenceNumber(String sequenceNumber) { this.sequenceNumber = sequenceNumber; return this; }
        public Builder beforeImage(String beforeImage) { this.beforeImage = beforeImage; return this; }
        public Builder afterImage(String afterImage) { this.afterImage = afterImage; return this; }
        public Builder reasonCode(String reasonCode) { this.reasonCode = reasonCode; return this; }
        public Builder processDate(LocalDateTime processDate) { this.processDate = processDate; return this; }
        public Builder processUser(String processUser) { this.processUser = processUser; return this; }

        public HistoryRecord build() {
            return new HistoryRecord(this);
        }
    }
}
