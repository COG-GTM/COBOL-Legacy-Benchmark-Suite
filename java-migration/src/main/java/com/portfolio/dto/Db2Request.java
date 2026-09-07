package com.portfolio.dto;

public class Db2Request {
  private String requestType;
  private int responseCode;
  private String connectionToken;
  private int sqlCode;
  private String errorMessage;

  public String getRequestType() {
    return requestType;
  }

  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  public String getConnectionToken() {
    return connectionToken;
  }

  public void setConnectionToken(String connectionToken) {
    this.connectionToken = connectionToken;
  }

  public int getSqlCode() {
    return sqlCode;
  }

  public void setSqlCode(int sqlCode) {
    this.sqlCode = sqlCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
