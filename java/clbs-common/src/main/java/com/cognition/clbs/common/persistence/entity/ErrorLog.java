package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

/** DB2 {@code ERRLOG} (V3); host variables in {@code DBTBLS.cpy ERRLOG-RECORD}. */
@Entity
@Table(name = "ERRLOG")
public class ErrorLog {

  @EmbeddedId private ErrorLogKey id;

  @Column(name = "ERROR_TYPE", nullable = false, length = 1)
  private String errorType;

  @Column(name = "ERROR_SEVERITY", nullable = false)
  private int severity;

  @Column(name = "ERROR_CODE", nullable = false, length = 8)
  private String errorCode;

  @Column(name = "ERROR_MESSAGE", nullable = false, length = 200)
  private String errorMessage;

  @Column(name = "PROCESS_DATE", nullable = false)
  private LocalDate processDate;

  @Column(name = "PROCESS_TIME", nullable = false)
  private LocalTime processTime;

  @Column(name = "USER_ID", nullable = false, length = 8)
  private String userId;

  @Column(name = "ADDITIONAL_INFO", length = 500)
  private String additionalInfo;

  protected ErrorLog() {}

  public ErrorLog(
      ErrorLogKey id,
      String errorType,
      int severity,
      String errorCode,
      String errorMessage,
      LocalDate processDate,
      LocalTime processTime,
      String userId,
      String additionalInfo) {
    this.id = id;
    this.errorType = errorType;
    this.severity = severity;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.processDate = processDate;
    this.processTime = processTime;
    this.userId = userId;
    this.additionalInfo = additionalInfo;
  }

  public ErrorLogKey getId() {
    return id;
  }

  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  public int getSeverity() {
    return severity;
  }

  public void setSeverity(int severity) {
    this.severity = severity;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public LocalDate getProcessDate() {
    return processDate;
  }

  public void setProcessDate(LocalDate processDate) {
    this.processDate = processDate;
  }

  public LocalTime getProcessTime() {
    return processTime;
  }

  public void setProcessTime(LocalTime processTime) {
    this.processTime = processTime;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getAdditionalInfo() {
    return additionalInfo;
  }

  public void setAdditionalInfo(String additionalInfo) {
    this.additionalInfo = additionalInfo;
  }
}
