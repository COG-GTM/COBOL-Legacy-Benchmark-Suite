package com.ipms.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Relational model of the VSAM KSDS PORTMSTR file (Portfolio Master File) from
 * {@code src/database/vsam/vsam-definitions.txt}: 400-byte fixed records keyed on
 * Portfolio ID (8) + Account Type (2) + Branch ID (2). Non-key attributes follow the
 * PORTFLIO.cpy record layout.
 */
@Entity
@Table(name = "PORTMSTR")
@IdClass(PortfolioMasterFile.Key.class)
public class PortfolioMasterFile {

    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "ACCOUNT_TYPE", length = 2, nullable = false)
    private String accountType;

    @Id
    @Column(name = "BRANCH_ID", length = 2, nullable = false)
    private String branchId;

    @Column(name = "ACCOUNT_NO", length = 10)
    private String accountNo;

    @Column(name = "CLIENT_NAME", length = 30)
    private String clientName;

    @Column(name = "CLIENT_TYPE", length = 1)
    private String clientType;

    @Column(name = "CREATE_DATE", length = 8)
    private String createDate;

    @Column(name = "LAST_MAINT", length = 8)
    private String lastMaint;

    @Column(name = "STATUS", length = 1)
    private String status;

    @Column(name = "TOTAL_VALUE", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "CASH_BALANCE", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "LAST_USER", length = 8)
    private String lastUser;

    @Column(name = "LAST_TRANS", length = 8)
    private String lastTrans;

    public static class Key implements Serializable {
        private String portfolioId;
        private String accountType;
        private String branchId;

        public Key() {
        }

        public Key(String portfolioId, String accountType, String branchId) {
            this.portfolioId = portfolioId;
            this.accountType = accountType;
            this.branchId = branchId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(portfolioId, key.portfolioId)
                    && Objects.equals(accountType, key.accountType)
                    && Objects.equals(branchId, key.branchId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, accountType, branchId);
        }
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
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

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getLastMaint() {
        return lastMaint;
    }

    public void setLastMaint(String lastMaint) {
        this.lastMaint = lastMaint;
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

    public String getLastTrans() {
        return lastTrans;
    }

    public void setLastTrans(String lastTrans) {
        this.lastTrans = lastTrans;
    }
}
