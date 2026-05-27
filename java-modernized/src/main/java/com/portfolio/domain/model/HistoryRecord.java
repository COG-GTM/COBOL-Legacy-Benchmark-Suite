package com.portfolio.domain.model;

import com.portfolio.domain.enums.HistoryActionCode;
import com.portfolio.domain.enums.HistoryRecordType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "history_record")
public class HistoryRecord {

    @EmbeddedId
    private HistoryRecordId id;

    @Column(name = "record_type", length = 2, nullable = false)
    private HistoryRecordType recordType;

    @Column(name = "action_code", length = 1, nullable = false)
    private HistoryActionCode actionCode;

    @Column(name = "before_image", columnDefinition = "TEXT")
    private String beforeImage;

    @Column(name = "after_image", columnDefinition = "TEXT")
    private String afterImage;

    @Column(name = "reason_code", length = 4)
    private String reasonCode;

    @Column(name = "process_date", length = 26)
    private String processDate;

    @Column(name = "process_user", length = 8)
    private String processUser;

    public HistoryRecord() {
    }

    public HistoryRecordId getId() {
        return id;
    }

    public void setId(HistoryRecordId id) {
        this.id = id;
    }

    public HistoryRecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(HistoryRecordType recordType) {
        this.recordType = recordType;
    }

    public HistoryActionCode getActionCode() {
        return actionCode;
    }

    public void setActionCode(HistoryActionCode actionCode) {
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

    public String getProcessDate() {
        return processDate;
    }

    public void setProcessDate(String processDate) {
        this.processDate = processDate;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
