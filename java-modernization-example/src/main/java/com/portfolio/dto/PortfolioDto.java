package com.portfolio.dto;

import com.portfolio.model.Portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for portfolio inquiry.
 * Contains the subset of Portfolio entity fields relevant to the inquiry screen.
 *
 * In the COBOL system, the BMS map (INQSET) defined which fields were displayed
 * on the 3270 terminal screen. This DTO serves the same purpose: it controls
 * which fields are included in the JSON response sent to the client.
 */
public class PortfolioDto {

    private String portfolioId;
    private String accountNo;
    private String clientName;
    private String clientType;
    private LocalDate createDate;
    private String status;
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private String lastUser;
    private LocalDate lastTransDate;

    public PortfolioDto() {
    }

    /**
     * Factory method to convert a Portfolio entity to a DTO.
     */
    public static PortfolioDto fromEntity(Portfolio entity) {
        PortfolioDto dto = new PortfolioDto();
        dto.setPortfolioId(entity.getKey().getPortfolioId());
        dto.setAccountNo(entity.getKey().getAccountNo());
        dto.setClientName(entity.getClientName());
        dto.setClientType(entity.getClientType() != null ? entity.getClientType().name() : null);
        dto.setCreateDate(entity.getCreateDate());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setTotalValue(entity.getTotalValue());
        dto.setCashBalance(entity.getCashBalance());
        dto.setLastUser(entity.getLastUser());
        dto.setLastTransDate(entity.getLastTransDate());
        return dto;
    }

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

    public LocalDate getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDate createDate) {
        this.createDate = createDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public String getLastUser() {
        return lastUser;
    }

    public void setLastUser(String lastUser) {
        this.lastUser = lastUser;
    }

    public LocalDate getLastTransDate() {
        return lastTransDate;
    }

    public void setLastTransDate(LocalDate lastTransDate) {
        this.lastTransDate = lastTransDate;
    }
}
