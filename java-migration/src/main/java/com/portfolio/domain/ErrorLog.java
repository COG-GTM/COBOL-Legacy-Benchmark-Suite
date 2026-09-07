package com.portfolio.domain;

import com.portfolio.domain.converter.*;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "ERRLOG")
public class ErrorLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long errlogId;

  private LocalDateTime errorTimestamp;

  @Column(name = "PROGRAM_ID", columnDefinition = "char(8)")
  private String programId;

  @Convert(converter = ErrorTypeConverter.class)
  @Column(name = "ERROR_TYPE", columnDefinition = "char(1)")
  private ErrorType errorType;

  @Convert(converter = ErrorSeverityConverter.class)
  @Column(name = "ERROR_SEVERITY")
  private ErrorSeverity errorSeverity;

  @Column(name = "ERROR_CODE", columnDefinition = "char(8)")
  private String errorCode;

  @Column(name = "ERROR_MESSAGE", length = 200)
  private String errorMessage;

  private LocalDate processDate;
  private LocalTime processTime;

  @Column(name = "USER_ID", columnDefinition = "char(8)")
  private String userId;

  @Column(name = "ADDITIONAL_INFO", length = 500)
  private String additionalInfo;

  public ErrorLog() {}

  public Long getErrlogId() {
    return errlogId;
  }

  public void setErrlogId(Long errlogId) {
    this.errlogId = errlogId;
  }

  public LocalDateTime getErrorTimestamp() {
    return errorTimestamp;
  }

  public void setErrorTimestamp(LocalDateTime errorTimestamp) {
    this.errorTimestamp = errorTimestamp;
  }

  public String getProgramId() {
    return programId;
  }

  public void setProgramId(String programId) {
    this.programId = programId;
  }

  public ErrorType getErrorType() {
    return errorType;
  }

  public void setErrorType(ErrorType errorType) {
    this.errorType = errorType;
  }

  public ErrorSeverity getErrorSeverity() {
    return errorSeverity;
  }

  public void setErrorSeverity(ErrorSeverity errorSeverity) {
    this.errorSeverity = errorSeverity;
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
