package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** VSAM TRANHIST as a table (V5); record layout {@code HISTREC.cpy}. */
@Entity
@Table(name = "CHANGE_HISTORY")
public class ChangeHistory {

  @EmbeddedId private ChangeHistoryKey id;

  @Column(name = "RECORD_TYPE", nullable = false, length = 2)
  private String recordType;

  @Column(name = "ACTION_CODE", nullable = false, length = 1)
  private String actionCode;

  @Column(name = "BEFORE_IMAGE", length = 400)
  private String beforeImage;

  @Column(name = "AFTER_IMAGE", length = 400)
  private String afterImage;

  @Column(name = "REASON_CODE", length = 4)
  private String reasonCode;

  @Column(name = "PROCESS_TIMESTAMP")
  private LocalDateTime processTimestamp;

  @Column(name = "PROCESS_USER", length = 8)
  private String processUser;

  protected ChangeHistory() {}

  public ChangeHistory(
      ChangeHistoryKey id,
      String recordType,
      String actionCode,
      String beforeImage,
      String afterImage,
      String reasonCode,
      LocalDateTime processTimestamp,
      String processUser) {
    this.id = id;
    this.recordType = recordType;
    this.actionCode = actionCode;
    this.beforeImage = beforeImage;
    this.afterImage = afterImage;
    this.reasonCode = reasonCode;
    this.processTimestamp = processTimestamp;
    this.processUser = processUser;
  }

  public ChangeHistoryKey getId() {
    return id;
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

  public LocalDateTime getProcessTimestamp() {
    return processTimestamp;
  }

  public void setProcessTimestamp(LocalDateTime processTimestamp) {
    this.processTimestamp = processTimestamp;
  }

  public String getProcessUser() {
    return processUser;
  }

  public void setProcessUser(String processUser) {
    this.processUser = processUser;
  }
}
