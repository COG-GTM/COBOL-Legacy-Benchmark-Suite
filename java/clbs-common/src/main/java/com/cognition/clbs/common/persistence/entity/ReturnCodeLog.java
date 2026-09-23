package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** DB2 {@code RTNCODES} (V4): return codes logged by the RTNCODE program. */
@Entity
@Table(name = "RTNCODES")
public class ReturnCodeLog {

  @EmbeddedId private ReturnCodeLogKey id;

  @Column(name = "RETURN_CODE", nullable = false)
  private int returnCode;

  @Column(name = "HIGHEST_CODE", nullable = false)
  private int highestCode;

  @Column(name = "STATUS_CODE", nullable = false, length = 1)
  private String statusCode;

  @Column(name = "MESSAGE_TEXT", length = 80)
  private String messageText;

  protected ReturnCodeLog() {}

  public ReturnCodeLog(
      ReturnCodeLogKey id, int returnCode, int highestCode, String statusCode, String messageText) {
    this.id = id;
    this.returnCode = returnCode;
    this.highestCode = highestCode;
    this.statusCode = statusCode;
    this.messageText = messageText;
  }

  public ReturnCodeLogKey getId() {
    return id;
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
