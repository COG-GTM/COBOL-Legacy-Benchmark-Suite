package com.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PortfolioCreateRequest {

    @NotBlank(message = "Portfolio ID is required")
    @Pattern(regexp = "PORT\\d{4}", message = "Portfolio ID must start with 'PORT' followed by 4 digits")
    private String portfolioId;

    @Size(max = 10)
    private String accountNo;

    @Size(max = 30)
    private String clientName;

    private String clientType;

    @NotBlank(message = "Portfolio name is required")
    @Size(max = 50)
    private String portfolioName;

    @NotNull(message = "Status is required")
    @Pattern(regexp = "[ACS]", message = "Status must be A (Active), C (Closed), or S (Suspended)")
    private String status;

    @Size(max = 2)
    private String accountType;

    @Size(max = 2)
    private String branchId;

    @Size(max = 10)
    private String clientId;

    @Size(max = 3)
    private String currencyCode;

    @Size(max = 1)
    private String riskLevel;

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
