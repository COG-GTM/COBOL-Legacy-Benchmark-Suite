package com.cog.portfolio.domain;

import com.cog.portfolio.common.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * POSREC.cpy POSITION-RECORD (VSAM POSFILE).
 * PREVIOUS_VALUE / DESCRIPTION come from the RPTPOS00 / INQPORT record views
 * (POS-PREVIOUS-VALUE, POS-FUND-NAME) which are not in the copybook.
 */
@Entity
@Table(name = "POSITION_RECORD")
public class Position {

    @EmbeddedId
    private PositionId id;

    /** POS-QUANTITY PIC S9(11)V9(4) COMP-3 */
    @Column(name = "QUANTITY", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO.setScale(4);

    /** POS-COST-BASIS PIC S9(13)V9(2) COMP-3 */
    @Column(name = "COST_BASIS", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis = BigDecimal.ZERO.setScale(2);

    /** POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3 */
    @Column(name = "MARKET_VALUE", precision = 15, scale = 2, nullable = false)
    private BigDecimal marketValue = BigDecimal.ZERO.setScale(2);

    @Column(name = "PREVIOUS_VALUE", precision = 15, scale = 2)
    private BigDecimal previousValue;

    @Column(name = "DESCRIPTION", length = 30)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "CURRENCY", length = 3, nullable = false)
    private CurrencyCode currency = CurrencyCode.USD;

    @Convert(converter = PositionStatus.Converter.class)
    @Column(name = "STATUS", length = 1, nullable = false)
    private PositionStatus status = PositionStatus.ACTIVE;

    @Column(name = "LAST_MAINT_DATE")
    private LocalDateTime lastMaintDate;

    @Column(name = "LAST_MAINT_USER", length = 8)
    private String lastMaintUser;

    protected Position() {
    }

    public Position(PositionId id) {
        this.id = id;
    }

    public PositionId getId() {
        return id;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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

    public BigDecimal getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(BigDecimal previousValue) {
        this.previousValue = previousValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyCode currency) {
        this.currency = currency;
    }

    public PositionStatus getStatus() {
        return status;
    }

    public void setStatus(PositionStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastMaintDate() {
        return lastMaintDate;
    }

    public void setLastMaintDate(LocalDateTime lastMaintDate) {
        this.lastMaintDate = lastMaintDate;
    }

    public String getLastMaintUser() {
        return lastMaintUser;
    }

    public void setLastMaintUser(String lastMaintUser) {
        this.lastMaintUser = lastMaintUser;
    }
}
