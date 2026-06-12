package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * History record, migrated from HISTREC.cpy (HISTORY-RECORD).
 * VSAM KSDS with RECORD KEY HIST-KEY = HIST-PORTFOLIO-ID + HIST-DATE + HIST-TIME + HIST-SEQ-NO,
 * mapped to table HISTORY_RECORD with composite PK (PORTFOLIO_ID, HIST_DATE, HIST_TIME, SEQ_NO).
 * HIST-FILLER PIC X(50) is reserved space and is not migrated.
 */
@Entity
@Table(name = "HISTORY_RECORD")
public class HistoryRecord {

    /** HIST-KEY = HIST-PORTFOLIO-ID + HIST-DATE + HIST-TIME + HIST-SEQ-NO. */
    @EmbeddedId
    private HistoryRecordId id;

    /** HIST-RECORD-TYPE PIC X(02); 88-levels: 'PT' Portfolio, 'PS' Position, 'TR' Transaction. */
    @Column(name = "RECORD_TYPE", columnDefinition = "CHAR(2)", length = 2, nullable = false)
    private String recordType;

    /** HIST-ACTION-CODE PIC X(01); 88-levels: 'A' Add, 'C' Change, 'D' Delete. */
    @Column(name = "ACTION_CODE", columnDefinition = "CHAR(1)", length = 1, nullable = false)
    private String actionCode;

    /** HIST-BEFORE-IMAGE PIC X(400) (raw record image before change). */
    @Column(name = "BEFORE_IMAGE", length = 400)
    private String beforeImage;

    /** HIST-AFTER-IMAGE PIC X(400) (raw record image after change). */
    @Column(name = "AFTER_IMAGE", length = 400)
    private String afterImage;

    /** HIST-REASON-CODE PIC X(04). */
    @Column(name = "REASON_CODE", columnDefinition = "CHAR(4)", length = 4)
    private String reasonCode;

    /** HIST-PROCESS-DATE PIC X(26) (DB2 timestamp format). */
    @Column(name = "PROCESS_DATE")
    private LocalDateTime processDate;

    /** HIST-PROCESS-USER PIC X(08). */
    @Column(name = "PROCESS_USER", length = 8)
    private String processUser;

    public HistoryRecordId getId() {
        return id;
    }

    public void setId(HistoryRecordId id) {
        this.id = id;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getBeforeImage() {
        return beforeImage;
    }

    public void setBeforeImage(String beforeImage) {
        this.beforeImage = beforeImage;
    }

    public String getAfterImage() {
        return afterImage;
    }

    public void setAfterImage(String afterImage) {
        this.afterImage = afterImage;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public LocalDateTime getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDateTime processDate) {
        this.processDate = processDate;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
