package com.portfolio.dto;

import jakarta.validation.constraints.NotBlank;

public class InquiryRequest {
  public enum InquiryFunction {
    MENU,
    INQP,
    INQH,
    EXIT
  }

  private InquiryFunction function;
  @NotBlank private String accountNo;

  public InquiryFunction getFunction() {
    return function;
  }

  public void setFunction(InquiryFunction function) {
    this.function = function;
  }

  public String getAccountNo() {
    return accountNo;
  }

  public void setAccountNo(String accountNo) {
    this.accountNo = accountNo;
  }
}
