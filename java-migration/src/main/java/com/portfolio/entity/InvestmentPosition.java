package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Investment Position Entity
 * Migrated from: VSAM POSHIST (KSDS, 350 bytes)
 * COBOL Copybook: POSREC.cpy
 * 
 * Key Structure:
 * - Portfolio ID (8 bytes)
 * - Position Date (8 bytes, YYYYMMDD)
 * - Investment ID (10 bytes)
 */
@Entity
@Table(name = "investment_positions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_investment_positions_key",
                        columnNames = {"portfolio_id", "investment_id", "position_date"})
        },
        indexes = {
                @Index(name = "idx_positions_portfolio", columnList = "portfolio_id, position_date"),
                @Index(name = "idx_positions_date", columnList = "position_date, portfolio_id"),
                @Index(name = "idx_positions_investment", columnList = "investment_id, position_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentPosition extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Portfolio Identifier
     * COBOL: POS-PORTFOLIO-ID PIC X(08)
     */
    @NotBlank(message = "Portfolio ID is required")
    @Size(max = 8, message = "Portfolio ID must not exceed 8 characters")
    @Column(name = "portfolio_id", nullable = false, length = 8)
    private String portfolioId;

    /**
     * Investment Identifier
     * COBOL: POS-INVESTMENT-ID PIC X(10)
     */
    @NotBlank(message = "Investment ID is required")
    @Size(max = 10, message = "Investment ID must not exceed 10 characters")
    @Column(name = "investment_id", nullable = false, length = 10)
    private String investmentId;

    /**
     * Position Date
     * COBOL: POS-DATE PIC X(08) (YYYYMMDD)
     */
    @NotNull(message = "Position date is required")
    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    /**
     * Holding Quantity
     * COBOL: POS-QUANTITY PIC S9(11)V9(4) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "-99999999999.9999", message = "Quantity is below minimum")
    @DecimalMax(value = "99999999999.9999", message = "Quantity exceeds maximum")
    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * Total Cost Basis
     * COBOL: POS-COST-BASIS PIC S9(13)V9(2) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Cost basis is required")
    @DecimalMin(value = "-9999999999999.99", message = "Cost basis is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Cost basis exceeds maximum")
    @Column(name = "cost_basis", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal costBasis = BigDecimal.ZERO;

    /**
     * Current Market Value
     * COBOL: POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Market value is required")
    @DecimalMin(value = "-9999999999999.99", message = "Market value is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Market value exceeds maximum")
    @Column(name = "market_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal marketValue = BigDecimal.ZERO;

    /**
     * Average Cost per Unit
     * Calculated field for reporting
     */
    @NotNull(message = "Average cost is required")
    @Column(name = "average_cost", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal averageCost = BigDecimal.ZERO;

    /**
     * Currency Code
     * COBOL: POS-CURRENCY PIC X(03)
     */
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    /**
     * Position Status
     * COBOL: POS-STATUS PIC X(01)
     * Values: A=Active, C=Closed, P=Pending
     */
    @NotNull(message = "Status is required")
    @Pattern(regexp = "[ACP]", message = "Status must be A (Active), C (Closed), or P (Pending)")
    @Column(name = "status", nullable = false, length = 1)
    @Builder.Default
    private String status = "A";

    /**
     * Check if position is active
     */
    public boolean isActive() {
        return "A".equals(this.status);
    }

    /**
     * Check if position is closed
     */
    public boolean isClosed() {
        return "C".equals(this.status);
    }

    /**
     * Check if position is pending
     */
    public boolean isPending() {
        return "P".equals(this.status);
    }

    /**
     * Calculate unrealized gain/loss
     * @return The difference between market value and cost basis
     */
    public BigDecimal getUnrealizedGainLoss() {
        return this.marketValue.subtract(this.costBasis);
    }

    /**
     * Calculate unrealized gain/loss percentage
     * @return The percentage gain/loss relative to cost basis
     */
    public BigDecimal getUnrealizedGainLossPercent() {
        if (this.costBasis.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getUnrealizedGainLoss()
                .divide(this.costBasis, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
