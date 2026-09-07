package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "RTNCODES")
public class ReturnCodeRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long rtnId;

  @Column(name = "RC_TIMESTAMP")
  private LocalDateTime rcTimestamp;

  @Column(name = "PROGRAM_ID", columnDefinition = "char(8)")
  private String programId;

  private int returnCode;
  private int highestCode;

  @Column(name = "STATUS_CODE", columnDefinition = "char(1)")
  private String statusCode;

  @Column(name = "MESSAGE_TEXT", length = 80)
  private String messageText;

  public ReturnCodeRecord() {}

  public Long getRtnId() {
    return rtnId;
  }

  public void setRtnId(Long rtnId) {
    this.rtnId = rtnId;
  }

  public LocalDateTime getRcTimestamp() {
    return rcTimestamp;
  }

  public void setRcTimestamp(LocalDateTime rcTimestamp) {
    this.rcTimestamp = rcTimestamp;
  }

  public String getProgramId() {
    return programId;
  }

  public void setProgramId(String programId) {
    this.programId = programId;
  }

  public int getReturnCode() {
    return returnCode;
  }

  public void setReturnCode(int returnCode) {
    this.returnCode = returnCode;
  }

  public int getHighestCode() {
    return highestCode;
  }

  public void setHighestCode(int highestCode) {
    this.highestCode = highestCode;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }

  public String getMessageText() {
    return messageText;
  }

  public void setMessageText(String messageText) {
    this.messageText = messageText;
  }
}
