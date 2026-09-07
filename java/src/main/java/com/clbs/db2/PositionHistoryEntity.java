package com.clbs.db2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** DBTBLS.cpy POSHIST-RECORD / database/tables POSHIST table loaded by HISTLD00. */
@Entity
@Table(name = "poshist")
public class PositionHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", length = 10)
    private String accountNo;

    @Column(name = "portfolio_id", length = 10)
    private String portfolioId;

    @Column(name = "trans_date", length = 10)
    private String transDate;

    @Column(name = "trans_type", length = 4)
    private String transType;

    @Column(name = "investment_id", length = 12)
    private String investmentId;

    @Column(name = "trans_units", precision = 15, scale = 4)
    private BigDecimal transUnits;

    @Column(name = "trans_price", precision = 15, scale = 4)
    private BigDecimal transPrice;

    @Column(name = "trans_amount", precision = 15, scale = 2)
    private BigDecimal transAmount;

    @Column(name = "trans_fees", precision = 15, scale = 2)
    private BigDecimal transFees;

    @Column(name = "trans_time", length = 8)
    private String transTime;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "cost_basis", precision = 15, scale = 2)
    private BigDecimal costBasis;

    @Column(name = "gain_loss", precision = 15, scale = 2)
    private BigDecimal gainLoss;

    @Column(name = "process_date", length = 8)
    private String processDate;

    @Column(name = "process_user", length = 8)
    private String processUser;

    public Long getId() {
        return id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getTransDate() {
        return transDate;
    }

    public void setTransDate(String transDate) {
        this.transDate = transDate;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public BigDecimal getTransUnits() {
        return transUnits;
    }

    public void setTransUnits(BigDecimal transUnits) {
        this.transUnits = transUnits;
    }

    public BigDecimal getTransPrice() {
        return transPrice;
    }

    public void setTransPrice(BigDecimal transPrice) {
        this.transPrice = transPrice;
    }

    public BigDecimal getTransAmount() {
        return transAmount;
    }

    public void setTransAmount(BigDecimal transAmount) {
        this.transAmount = transAmount;
    }

    public BigDecimal getTransFees() {
        return transFees;
    }

    public void setTransFees(BigDecimal transFees) {
        this.transFees = transFees;
    }

    public String getTransTime() {
        return transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public BigDecimal getGainLoss() {
        return gainLoss;
    }

    public void setGainLoss(BigDecimal gainLoss) {
        this.gainLoss = gainLoss;
    }

    public String getProcessDate() {
        return processDate;
    }

    public void setProcessDate(String processDate) {
        this.processDate = processDate;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
