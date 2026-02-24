package com.investment.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Position Record - Java equivalent of POSREC.cpy
 * Maps the COBOL POSITION-RECORD copybook structure.
 */
public class PositionRecord {

    /** Key fields */
    private String portfolioId;        // POS-PORTFOLIO-ID: PIC X(08)
    private String positionDate;       // POS-DATE: PIC X(08)  YYYYMMDD
    private String investmentId;       // POS-INVESTMENT-ID: PIC X(10)

    /** Position data */
    private BigDecimal quantity;        // POS-QUANTITY: PIC S9(11)V9(4) COMP-3
    private BigDecimal costBasis;       // POS-COST-BASIS: PIC S9(13)V9(2) COMP-3
    private BigDecimal marketValue;     // POS-MARKET-VALUE: PIC S9(13)V9(2) COMP-3
    private String currency;            // POS-CURRENCY: PIC X(03)
    private PositionStatus status;      // POS-STATUS: PIC X(01)

    /** Audit fields */
    private LocalDateTime lastMaintDate; // POS-LAST-MAINT-DATE: PIC X(26)
    private String lastMaintUser;        // POS-LAST-MAINT-USER: PIC X(08)

    public enum PositionStatus {
        ACTIVE('A'),
        CLOSED('C'),
        PENDING('P');

        private final char code;

        PositionStatus(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static PositionStatus fromCode(char code) {
            for (PositionStatus s : values()) {
                if (s.code == code) return s;
            }
            throw new IllegalArgumentException("Invalid position status: " + code);
        }
    }

    // --- Getters and Setters ---

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getPositionDate() { return positionDate; }
    public void setPositionDate(String positionDate) { this.positionDate = positionDate; }

    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }

    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PositionStatus getStatus() { return status; }
    public void setStatus(PositionStatus status) { this.status = status; }

    public LocalDateTime getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDateTime lastMaintDate) { this.lastMaintDate = lastMaintDate; }

    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }

    /**
     * Builds the composite key (portfolioId + date + investmentId)
     * corresponding to POS-KEY in the COBOL copybook.
     */
    public String getCompositeKey() {
        return portfolioId + positionDate + investmentId;
    }

    @Override
    public String toString() {
        return "PositionRecord{" +
                "portfolioId='" + portfolioId + '\'' +
                ", investmentId='" + investmentId + '\'' +
                ", quantity=" + quantity +
                ", marketValue=" + marketValue +
                ", status=" + status +
                '}';
    }
}
