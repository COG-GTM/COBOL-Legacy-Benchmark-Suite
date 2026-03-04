package com.cobolbenchmark.online;

/**
 * Inquiry Request DTO - replaces INQMAP BMS map input.
 * Maps to EXEC CICS RECEIVE MAP('INQMAP') input fields.
 */
public class InquiryRequest {

    private String portfolioId;
    private String userId;
    private String password;
    private String inquiryType; // POSITION, HISTORY, SUMMARY

    public InquiryRequest() {
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getInquiryType() { return inquiryType; }
    public void setInquiryType(String inquiryType) { this.inquiryType = inquiryType; }
}
