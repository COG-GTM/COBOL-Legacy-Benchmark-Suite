package com.investment.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Portfolio Master Record - Java equivalent of PORTFLIO.cpy
 * Maps the COBOL PORT-RECORD copybook structure.
 */
public class PortfolioRecord {

    /** Portfolio key fields */
    private String portfolioId;        // PORT-ID: PIC X(8)
    private String accountNumber;      // PORT-ACCOUNT-NO: PIC X(10)

    /** Client information */
    private String clientName;         // PORT-CLIENT-NAME: PIC X(30)
    private ClientType clientType;     // PORT-CLIENT-TYPE: PIC X(1)

    /** Portfolio information */
    private LocalDate createDate;      // PORT-CREATE-DATE: PIC 9(8)
    private LocalDate lastMaintDate;   // PORT-LAST-MAINT: PIC 9(8)
    private PortfolioStatus status;    // PORT-STATUS: PIC X(1)

    /** Financial information */
    private BigDecimal totalValue;     // PORT-TOTAL-VALUE: PIC S9(13)V99 COMP-3
    private BigDecimal cashBalance;    // PORT-CASH-BALANCE: PIC S9(13)V99 COMP-3

    /** Audit information */
    private String lastUser;           // PORT-LAST-USER: PIC X(8)
    private LocalDate lastTransDate;   // PORT-LAST-TRANS: PIC 9(8)

    public enum ClientType {
        INDIVIDUAL('I'),
        CORPORATE('C'),
        TRUST('T');

        private final char code;

        ClientType(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static ClientType fromCode(char code) {
            for (ClientType type : values()) {
                if (type.code == code) return type;
            }
            throw new IllegalArgumentException("Invalid client type code: " + code);
        }
    }

    public enum PortfolioStatus {
        ACTIVE('A'),
        CLOSED('C'),
        SUSPENDED('S');

        private final char code;

        PortfolioStatus(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static PortfolioStatus fromCode(char code) {
            for (PortfolioStatus s : values()) {
                if (s.code == code) return s;
            }
            throw new IllegalArgumentException("Invalid portfolio status code: " + code);
        }
    }

    // --- Getters and Setters ---

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public ClientType getClientType() { return clientType; }
    public void setClientType(ClientType clientType) { this.clientType = clientType; }

    public LocalDate getCreateDate() { return createDate; }
    public void setCreateDate(LocalDate createDate) { this.createDate = createDate; }

    public LocalDate getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDate lastMaintDate) { this.lastMaintDate = lastMaintDate; }

    public PortfolioStatus getStatus() { return status; }
    public void setStatus(PortfolioStatus status) { this.status = status; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

    public String getLastUser() { return lastUser; }
    public void setLastUser(String lastUser) { this.lastUser = lastUser; }

    public LocalDate getLastTransDate() { return lastTransDate; }
    public void setLastTransDate(LocalDate lastTransDate) { this.lastTransDate = lastTransDate; }

    @Override
    public String toString() {
        return "PortfolioRecord{" +
                "portfolioId='" + portfolioId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", clientName='" + clientName + '\'' +
                ", status=" + status +
                ", totalValue=" + totalValue +
                '}';
    }
}
