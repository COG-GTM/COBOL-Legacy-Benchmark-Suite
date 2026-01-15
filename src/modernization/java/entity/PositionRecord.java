package com.portfolio.modernization.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Position Record Entity
 * 
 * Modernized from COBOL copybook: src/copybook/common/POSREC.cpy
 * Maps to database table: POSITION_MASTER
 * 
 * Original COBOL structure:
 * <pre>
 * 01  POSITION-RECORD.
 *     05  POS-KEY.
 *         10  POS-PORTFOLIO-ID   PIC X(08).
 *         10  POS-DATE           PIC X(08).
 *         10  POS-INVESTMENT-ID  PIC X(10).
 *     05  POS-DATA.
 *         10  POS-QUANTITY       PIC S9(11)V9(4) COMP-3.
 *         10  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3.
 *         10  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3.
 *         10  POS-CURRENCY       PIC X(03).
 *         10  POS-STATUS         PIC X(01).
 *     05  POS-AUDIT.
 *         10  POS-LAST-MAINT-DATE   PIC X(26).
 *         10  POS-LAST-MAINT-USER   PIC X(08).
 * </pre>
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Entity
@Table(name = "POSITION_MASTER", indexes = {
    @Index(name = "IDX_POS_ACCOUNT", columnList = "accountNumber, status"),
    @Index(name = "IDX_POS_FUND_UPDATE", columnList = "fundId, lastUpdate"),
    @Index(name = "IDX_POS_MARKET_VALUE", columnList = "marketValue, status")
})
public class PositionRecord {

    /**
     * Position status constants (from POS-STATUS 88-level conditions)
     */
    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_CLOSED = "C";
    public static final String STATUS_PENDING = "P";

    @Id
    @Column(name = "PORTFOLIO_ID", length = 20, nullable = false)
    private String portfolioId;

    @NotNull(message = "Account number is required")
    @Size(max = 15, message = "Account number cannot exceed 15 characters")
    @Column(name = "ACCOUNT_NUMBER", length = 15, nullable = false)
    private String accountNumber;

    @NotNull(message = "Fund ID is required")
    @Size(max = 10, message = "Fund ID cannot exceed 10 characters")
    @Column(name = "FUND_ID", length = 10, nullable = false)
    private String fundId;

    @DecimalMin(value = "0.0000", message = "Units must be non-negative")
    @Digits(integer = 11, fraction = 4, message = "Units precision exceeded")
    @Column(name = "UNITS", precision = 15, scale = 4)
    private BigDecimal units;

    @Digits(integer = 13, fraction = 2, message = "Cost basis precision exceeded")
    @Column(name = "COST_BASIS", precision = 15, scale = 2)
    private BigDecimal costBasis;

    @Digits(integer = 13, fraction = 2, message = "Market value precision exceeded")
    @Column(name = "MARKET_VALUE", precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Size(max = 1, message = "Status must be 1 character")
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status = STATUS_ACTIVE;

    @Column(name = "POSITION_DATE", nullable = false)
    private LocalDate positionDate;

    @Column(name = "LAST_UPDATE", nullable = false)
    private LocalDateTime lastUpdate;

    @Size(max = 8, message = "Last maintenance user cannot exceed 8 characters")
    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;

    @Column(name = "VSAM_MIGRATION_DATE")
    private LocalDateTime vsamMigrationDate;

    @Size(max = 26, message = "VSAM record key cannot exceed 26 characters")
    @Column(name = "VSAM_RECORD_KEY", length = 26)
    private String vsamRecordKey;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public PositionRecord() {
        this.lastUpdate = LocalDateTime.now();
        this.positionDate = LocalDate.now();
    }

    public PositionRecord(String portfolioId, String accountNumber, String fundId) {
        this();
        this.portfolioId = portfolioId;
        this.accountNumber = accountNumber;
        this.fundId = fundId;
    }

    @PrePersist
    @PreUpdate
    public void prePersistOrUpdate() {
        this.lastUpdate = LocalDateTime.now();
    }

    /**
     * Validates position based on business rules from POSUPD00.cbl
     * Preserves original business logic from COBOL program
     * @return true if position is valid
     */
    public boolean isValidPosition() {
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            return false;
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return false;
        }
        if (fundId == null || fundId.trim().isEmpty()) {
            return false;
        }
        if (units != null && units.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (!isValidStatus()) {
            return false;
        }
        return true;
    }

    /**
     * Checks if status is valid
     */
    public boolean isValidStatus() {
        if (status == null) return false;
        return status.equals(STATUS_ACTIVE) || 
               status.equals(STATUS_CLOSED) || 
               status.equals(STATUS_PENDING);
    }

    /**
     * Checks if position is active
     */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /**
     * Checks if position is closed
     */
    public boolean isClosed() {
        return STATUS_CLOSED.equals(status);
    }

    /**
     * Checks if position is pending
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    /**
     * Checks if position has holdings (units > 0)
     */
    public boolean hasHoldings() {
        return units != null && units.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calculates unrealized gain/loss
     * @return market value minus cost basis
     */
    public BigDecimal calculateUnrealizedGainLoss() {
        if (marketValue == null || costBasis == null) {
            return BigDecimal.ZERO;
        }
        return marketValue.subtract(costBasis);
    }

    /**
     * Calculates unrealized gain/loss percentage
     * @return percentage gain/loss
     */
    public BigDecimal calculateUnrealizedGainLossPercent() {
        if (costBasis == null || costBasis.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal gainLoss = calculateUnrealizedGainLoss();
        return gainLoss.divide(costBasis, 4, RoundingMode.HALF_UP)
                       .multiply(new BigDecimal("100"));
    }

    /**
     * Calculates average cost per unit
     * @return cost basis divided by units
     */
    public BigDecimal calculateAverageCost() {
        if (units == null || units.compareTo(BigDecimal.ZERO) == 0 || costBasis == null) {
            return BigDecimal.ZERO;
        }
        return costBasis.divide(units, 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculates current price per unit
     * @return market value divided by units
     */
    public BigDecimal calculateCurrentPrice() {
        if (units == null || units.compareTo(BigDecimal.ZERO) == 0 || marketValue == null) {
            return BigDecimal.ZERO;
        }
        return marketValue.divide(units, 4, RoundingMode.HALF_UP);
    }

    /**
     * Updates market value based on current price
     * @param currentPrice current price per unit
     */
    public void updateMarketValue(BigDecimal currentPrice) {
        if (units != null && currentPrice != null) {
            this.marketValue = units.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
            this.lastUpdate = LocalDateTime.now();
        }
    }

    /**
     * Adds units to position (for buy transactions)
     * @param additionalUnits units to add
     * @param purchasePrice price per unit
     */
    public void addUnits(BigDecimal additionalUnits, BigDecimal purchasePrice) {
        if (additionalUnits == null || purchasePrice == null) {
            return;
        }
        
        BigDecimal additionalCost = additionalUnits.multiply(purchasePrice);
        
        if (this.units == null) {
            this.units = BigDecimal.ZERO;
        }
        if (this.costBasis == null) {
            this.costBasis = BigDecimal.ZERO;
        }
        
        this.units = this.units.add(additionalUnits);
        this.costBasis = this.costBasis.add(additionalCost).setScale(2, RoundingMode.HALF_UP);
        this.lastUpdate = LocalDateTime.now();
    }

    /**
     * Removes units from position (for sell transactions)
     * @param unitsToRemove units to remove
     * @return cost basis of removed units (for gain/loss calculation)
     */
    public BigDecimal removeUnits(BigDecimal unitsToRemove) {
        if (unitsToRemove == null || this.units == null || 
            unitsToRemove.compareTo(this.units) > 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal avgCost = calculateAverageCost();
        BigDecimal removedCostBasis = unitsToRemove.multiply(avgCost).setScale(2, RoundingMode.HALF_UP);
        
        this.units = this.units.subtract(unitsToRemove);
        this.costBasis = this.costBasis.subtract(removedCostBasis).setScale(2, RoundingMode.HALF_UP);
        this.lastUpdate = LocalDateTime.now();
        
        if (this.units.compareTo(BigDecimal.ZERO) == 0) {
            this.status = STATUS_CLOSED;
        }
        
        return removedCostBasis;
    }

    /**
     * Closes the position
     */
    public void closePosition() {
        this.status = STATUS_CLOSED;
        this.lastUpdate = LocalDateTime.now();
    }

    /**
     * Activates the position
     */
    public void activatePosition() {
        this.status = STATUS_ACTIVE;
        this.lastUpdate = LocalDateTime.now();
    }

    /**
     * Creates VSAM record key for migration tracking
     * Format: POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID
     */
    public String createVsamRecordKey() {
        StringBuilder sb = new StringBuilder();
        if (portfolioId != null) {
            sb.append(String.format("%-8s", portfolioId));
        }
        if (positionDate != null) {
            sb.append(positionDate.format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        if (fundId != null) {
            sb.append(String.format("%-10s", fundId));
        }
        return sb.toString();
    }

    /**
     * Checks if this is a high-value position
     * @param threshold market value threshold
     * @return true if market value exceeds threshold
     */
    public boolean isHighValuePosition(BigDecimal threshold) {
        if (marketValue == null || threshold == null) {
            return false;
        }
        return marketValue.compareTo(threshold) > 0;
    }

    // Getters and Setters

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getFundId() {
        return fundId;
    }

    public void setFundId(String fundId) {
        this.fundId = fundId;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public void setUnits(BigDecimal units) {
        this.units = units;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(LocalDate positionDate) {
        this.positionDate = positionDate;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getLastMaintUser() {
        return lastMaintUser;
    }

    public void setLastMaintUser(String lastMaintUser) {
        this.lastMaintUser = lastMaintUser;
    }

    public LocalDateTime getVsamMigrationDate() {
        return vsamMigrationDate;
    }

    public void setVsamMigrationDate(LocalDateTime vsamMigrationDate) {
        this.vsamMigrationDate = vsamMigrationDate;
    }

    public String getVsamRecordKey() {
        return vsamRecordKey;
    }

    public void setVsamRecordKey(String vsamRecordKey) {
        this.vsamRecordKey = vsamRecordKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionRecord that = (PositionRecord) o;
        return Objects.equals(portfolioId, that.portfolioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId);
    }

    @Override
    public String toString() {
        return "PositionRecord{" +
                "portfolioId='" + portfolioId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", fundId='" + fundId + '\'' +
                ", units=" + units +
                ", costBasis=" + costBasis +
                ", marketValue=" + marketValue +
                ", status='" + status + '\'' +
                '}';
    }
}
