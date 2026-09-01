package com.clbs.portfolio.domain;

import com.clbs.portfolio.domain.enums.PositionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "position_record")
public class PositionRecord {
    @EmbeddedId
    @NotNull
    private PositionRecordKey key;

    @NotNull
    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @NotNull
    @Column(name = "cost_basis", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @NotNull
    @Column(name = "market_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @NotNull
    @Size(max = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "status", length = 1, nullable = false, columnDefinition = "CHAR(1)")
    private PositionStatus status;

    @NotNull
    @Column(name = "last_maint_at", nullable = false)
    private Instant lastMaintAt;

    @NotNull
    @Size(max = 8)
    @Column(name = "last_maint_user", length = 8, nullable = false)
    private String lastMaintUser;

    protected PositionRecord() {
    }

    public PositionRecord(PositionRecordKey key, BigDecimal quantity, BigDecimal costBasis,
                          BigDecimal marketValue, String currencyCode, PositionStatus status,
                          Instant lastMaintAt, String lastMaintUser) {
        this.key = key;
        this.quantity = quantity;
        this.costBasis = costBasis;
        this.marketValue = marketValue;
        this.currencyCode = currencyCode;
        this.status = status;
        this.lastMaintAt = lastMaintAt;
        this.lastMaintUser = lastMaintUser;
    }

    public PositionRecordKey getKey() {
        return key;
    }

    public void setKey(PositionRecordKey key) {
        this.key = key;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public PositionStatus getStatus() {
        return status;
    }

    public void setStatus(PositionStatus status) {
        this.status = status;
    }

    public Instant getLastMaintAt() {
        return lastMaintAt;
    }

    public void setLastMaintAt(Instant lastMaintAt) {
        this.lastMaintAt = lastMaintAt;
    }

    public String getLastMaintUser() {
        return lastMaintUser;
    }

    public void setLastMaintUser(String lastMaintUser) {
        this.lastMaintUser = lastMaintUser;
    }
}
