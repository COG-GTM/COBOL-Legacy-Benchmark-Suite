package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** HISTREC.cpy HISTORY-RECORD (VSAM TRANHIST). Before/after images are PIC X(400). */
@Entity
@Table(name = "HISTORY_RECORD")
public class History {

    @EmbeddedId
    private HistoryId id;

    @Convert(converter = HistoryRecordType.Converter.class)
    @Column(name = "RECORD_TYPE", length = 2, nullable = false)
    private HistoryRecordType recordType;

    @Convert(converter = HistoryAction.Converter.class)
    @Column(name = "ACTION_CODE", length = 1, nullable = false)
    private HistoryAction action;

    @Lob
    @Column(name = "BEFORE_IMAGE")
    private String beforeImage;

    @Lob
    @Column(name = "AFTER_IMAGE")
    private String afterImage;

    @Column(name = "REASON_CODE", length = 4)
    private String reasonCode;

    @Column(name = "PROCESS_DATE")
    private LocalDateTime processDate;

    @Column(name = "PROCESS_USER", length = 8)
    private String processUser;

    protected History() {
    }

    public History(HistoryId id, HistoryRecordType recordType, HistoryAction action) {
        this.id = id;
        this.recordType = recordType;
        this.action = action;
    }

    public HistoryId getId() {
        return id;
    }

    public HistoryRecordType getRecordType() {
        return recordType;
    }

    public HistoryAction getAction() {
        return action;
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
