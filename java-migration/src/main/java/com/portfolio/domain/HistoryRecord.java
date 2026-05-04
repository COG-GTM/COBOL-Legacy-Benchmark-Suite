package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * History Record entity - migrated from COBOL HISTREC.cpy.
 *
 * COBOL level-88 mappings:
 * - HIST-TYPE: PT=Portfolio, PS=Position, TR=Transaction
 * - HIST-ACTION: A=Add, C=Change, D=Delete
 */
@Entity
@Table(name = "history_record")
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "history_date", nullable = false)
    private LocalDate historyDate;

    @Column(name = "history_time", length = 6, nullable = false)
    private String historyTime;

    @Column(name = "seq_no", length = 4, nullable = false)
    private String seqNo;

    @Column(name = "record_type", length = 2, nullable = false)
    private String recordType;

    @Column(name = "action_code", length = 1, nullable = false)
    private String actionCode;

    @Column(name = "before_image", length = 400)
    private String beforeImage;

    @Column(name = "after_image", length = 400)
    private String afterImage;

    @Column(name = "reason_code", length = 4)
    private String reasonCode;

    @Column(name = "process_date", nullable = false)
    private LocalDateTime processDate;

    @Column(name = "process_user", length = 8, nullable = false)
    private String processUser;

    @Column(name = "filler", length = 50)
    private String filler;

    public HistoryRecord() {
        this.processDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public LocalDate getHistoryDate() { return historyDate; }
    public void setHistoryDate(LocalDate historyDate) { this.historyDate = historyDate; }
    public String getHistoryTime() { return historyTime; }
    public void setHistoryTime(String historyTime) { this.historyTime = historyTime; }
    public String getSeqNo() { return seqNo; }
    public void setSeqNo(String seqNo) { this.seqNo = seqNo; }
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
    public LocalDateTime getProcessDate() { return processDate; }
    public void setProcessDate(LocalDateTime processDate) { this.processDate = processDate; }
    public String getProcessUser() { return processUser; }
    public void setProcessUser(String processUser) { this.processUser = processUser; }
    public String getFiller() { return filler; }
    public void setFiller(String filler) { this.filler = filler; }

    public boolean isPortfolioRecord() { return "PT".equals(recordType); }
    public boolean isPositionRecord() { return "PS".equals(recordType); }
    public boolean isTransactionRecord() { return "TR".equals(recordType); }
    public boolean isAdd() { return "A".equals(actionCode); }
    public boolean isChange() { return "C".equals(actionCode); }
    public boolean isDelete() { return "D".equals(actionCode); }
}
