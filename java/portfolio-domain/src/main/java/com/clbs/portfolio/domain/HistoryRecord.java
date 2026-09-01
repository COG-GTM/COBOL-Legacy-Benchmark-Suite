package com.clbs.portfolio.domain;

import com.clbs.portfolio.domain.enums.HistoryActionCode;
import com.clbs.portfolio.domain.enums.HistoryRecordType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "history_record")
public class HistoryRecord {
    @EmbeddedId
    @NotNull
    private HistoryRecordKey key;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "record_type", length = 2, nullable = false, columnDefinition = "CHAR(2)")
    private HistoryRecordType recordType;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "action_code", length = 1, nullable = false, columnDefinition = "CHAR(1)")
    private HistoryActionCode actionCode;

    @Size(max = 400)
    @Column(name = "before_image", length = 400)
    private String beforeImage;

    @Size(max = 400)
    @Column(name = "after_image", length = 400)
    private String afterImage;

    @Size(max = 4)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "reason_code", length = 4, columnDefinition = "CHAR(4)")
    private String reasonCode;

    @NotNull
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @NotNull
    @Size(max = 8)
    @Column(name = "process_user", length = 8, nullable = false)
    private String processUser;

    protected HistoryRecord() {
    }

    public HistoryRecord(HistoryRecordKey key, HistoryRecordType recordType, HistoryActionCode actionCode,
                         String beforeImage, String afterImage, String reasonCode, Instant processedAt,
                         String processUser) {
        this.key = key;
        this.recordType = recordType;
        this.actionCode = actionCode;
        this.beforeImage = beforeImage;
        this.afterImage = afterImage;
        this.reasonCode = reasonCode;
        this.processedAt = processedAt;
        this.processUser = processUser;
    }

    public HistoryRecordKey getKey() {
        return key;
    }

    public void setKey(HistoryRecordKey key) {
        this.key = key;
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

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
