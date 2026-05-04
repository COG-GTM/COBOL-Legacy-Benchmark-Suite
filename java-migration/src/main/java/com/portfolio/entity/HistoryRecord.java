package com.portfolio.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "history_record")
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "history_date", length = 8)
    private String historyDate;

    @Column(name = "history_time", length = 6)
    private String historyTime;

    @Column(name = "seq_no", length = 4)
    private String seqNo;

    @Column(name = "record_type", length = 2)
    private String recordType;

    @Column(name = "action_code", length = 1)
    private String actionCode;

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

    public HistoryRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getHistoryDate() { return historyDate; }
    public void setHistoryDate(String historyDate) { this.historyDate = historyDate; }

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

    public boolean isPortfolioType() { return "PT".equals(recordType); }
    public boolean isPositionType() { return "PS".equals(recordType); }
    public boolean isTransactionType() { return "TR".equals(recordType); }

    public boolean isAdd() { return "A".equals(actionCode); }
    public boolean isChange() { return "C".equals(actionCode); }
    public boolean isDelete() { return "D".equals(actionCode); }
}
