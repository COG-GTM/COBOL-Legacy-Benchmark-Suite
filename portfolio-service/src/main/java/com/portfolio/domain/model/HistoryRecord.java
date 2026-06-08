package com.portfolio.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    public Long getId() { return id; }
    public String getPortfolioId() { return portfolioId; }
    public HistoryRecordType getRecordType() { return recordType; }
    public HistoryActionCode getActionCode() { return actionCode; }
    public String getBeforeImage() { return beforeImage; }
    public String getAfterImage() { return afterImage; }
    public String getReasonCode() { return reasonCode; }
    public LocalDateTime getProcessDate() { return processDate; }
    public String getProcessUser() { return processUser; }
}
