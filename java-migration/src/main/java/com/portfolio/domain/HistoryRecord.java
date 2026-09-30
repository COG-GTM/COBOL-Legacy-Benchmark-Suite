package com.portfolio.domain;

import com.portfolio.domain.converter.HistoryActionConverter;
import com.portfolio.domain.converter.HistoryRecordTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORY_RECORD")
public class HistoryRecord {
  @EmbeddedId private HistoryKey id;

  @Convert(converter = HistoryRecordTypeConverter.class)
  @Column(name = "RECORD_TYPE", columnDefinition = "char(2)")
  private HistoryRecordType recordType;

  @Convert(converter = HistoryActionConverter.class)
  @Column(name = "ACTION_CODE", columnDefinition = "char(1)")
  private HistoryAction actionCode;

  @Lob
  @Column(name = "BEFORE_IMAGE")
  private String beforeImage;

  @Lob
  @Column(name = "AFTER_IMAGE")
  private String afterImage;

  @Column(name = "REASON_CODE", columnDefinition = "char(4)")
  private String reasonCode;

  private LocalDateTime processDate;

  @Column(length = 8)
  private String processUser;

  public HistoryRecord() {}

  public HistoryKey getId() {
    return id;
  }

  public void setId(HistoryKey id) {
    this.id = id;
  }

  public HistoryRecordType getRecordType() {
    return recordType;
  }

  public void setRecordType(HistoryRecordType recordType) {
    this.recordType = recordType;
  }

  public HistoryAction getActionCode() {
    return actionCode;
  }

  public void setActionCode(HistoryAction actionCode) {
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
