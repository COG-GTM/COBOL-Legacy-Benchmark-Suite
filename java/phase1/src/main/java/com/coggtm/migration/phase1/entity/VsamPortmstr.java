package com.coggtm.migration.phase1.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "vsam_portmstr")
@IdClass(VsamPortmstr.VsamPortmstrId.class)
public class VsamPortmstr {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "account_no", length = 10, nullable = false)
    private String accountNo;

    @Column(name = "client_name", length = 30, nullable = false)
    private String clientName;

    @Column(name = "client_type", length = 1, nullable = false)
    private String clientType;

    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;

    @Column(name = "last_maint_date", nullable = false)
    private LocalDate lastMaintDate;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "total_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal cashBalance;

    @Column(name = "last_user", length = 8, nullable = false)
    private String lastUser;

    @Column(name = "last_trans_date", nullable = false)
    private LocalDate lastTransDate;

    @Column(name = "filler", length = 50)
    private String filler;

    public VsamPortmstr() {
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }

    public LocalDate getCreateDate() { return createDate; }
    public void setCreateDate(LocalDate createDate) { this.createDate = createDate; }

    public LocalDate getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDate lastMaintDate) { this.lastMaintDate = lastMaintDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

    public String getLastUser() { return lastUser; }
    public void setLastUser(String lastUser) { this.lastUser = lastUser; }

    public LocalDate getLastTransDate() { return lastTransDate; }
    public void setLastTransDate(LocalDate lastTransDate) { this.lastTransDate = lastTransDate; }

    public String getFiller() { return filler; }
    public void setFiller(String filler) { this.filler = filler; }

    public static class VsamPortmstrId implements Serializable {
        private String portfolioId;
        private String accountNo;

        public VsamPortmstrId() {
        }

        public VsamPortmstrId(String portfolioId, String accountNo) {
            this.portfolioId = portfolioId;
            this.accountNo = accountNo;
        }

        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VsamPortmstrId)) return false;
            VsamPortmstrId that = (VsamPortmstrId) o;
            return Objects.equals(portfolioId, that.portfolioId)
                    && Objects.equals(accountNo, that.accountNo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, accountNo);
        }
    }
}
