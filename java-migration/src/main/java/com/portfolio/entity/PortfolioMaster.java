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
 * Portfolio Master Entity
 * Migrated from: VSAM PORTMSTR (KSDS, 400 bytes)
 * COBOL Copybook: PORTFLIO.cpy
 * 
 * Key Structure:
 * - Portfolio ID (8 bytes)
 * - Account Type (2 bytes)
 * - Branch ID (2 bytes)
 */
@Entity
@Table(name = "portfolio_master",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_portfolio_master_key",
                        columnNames = {"portfolio_id", "account_type", "branch_id"})
        },
        indexes = {
                @Index(name = "idx_portfolio_master_client", columnList = "client_id, status"),
                @Index(name = "idx_portfolio_master_status", columnList = "status, open_date"),
                @Index(name = "idx_portfolio_master_account", columnList = "account_no")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioMaster extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Portfolio Identifier
     * COBOL: PORT-ID PIC X(8)
     */
    @NotBlank(message = "Portfolio ID is required")
    @Size(max = 8, message = "Portfolio ID must not exceed 8 characters")
    @Column(name = "portfolio_id", nullable = false, length = 8)
    private String portfolioId;

    /**
     * Account Type
     * COBOL: PORT-ACCOUNT-TYPE PIC X(2)
     */
    @NotBlank(message = "Account type is required")
    @Size(max = 2, message = "Account type must not exceed 2 characters")
    @Column(name = "account_type", nullable = false, length = 2)
    private String accountType;

    /**
     * Branch Identifier
     * COBOL: PORT-BRANCH-ID PIC X(2)
     */
    @NotBlank(message = "Branch ID is required")
    @Size(max = 2, message = "Branch ID must not exceed 2 characters")
    @Column(name = "branch_id", nullable = false, length = 2)
    private String branchId;

    /**
     * Account Number
     * COBOL: PORT-ACCOUNT-NO PIC X(10)
     */
    @NotBlank(message = "Account number is required")
    @Size(max = 10, message = "Account number must not exceed 10 characters")
    @Column(name = "account_no", nullable = false, length = 10)
    private String accountNo;

    /**
     * Client Identifier
     * COBOL: PORT-CLIENT-ID PIC X(10)
     */
    @NotBlank(message = "Client ID is required")
    @Size(max = 10, message = "Client ID must not exceed 10 characters")
    @Column(name = "client_id", nullable = false, length = 10)
    private String clientId;

    /**
     * Client Name
     * COBOL: PORT-CLIENT-NAME PIC X(30)
     */
    @NotBlank(message = "Client name is required")
    @Size(max = 30, message = "Client name must not exceed 30 characters")
    @Column(name = "client_name", nullable = false, length = 30)
    private String clientName;

    /**
     * Client Type
     * COBOL: PORT-CLIENT-TYPE PIC X(1)
     * Values: I=Individual, C=Corporate, T=Trust
     */
    @NotNull(message = "Client type is required")
    @Pattern(regexp = "[ICT]", message = "Client type must be I (Individual), C (Corporate), or T (Trust)")
    @Column(name = "client_type", nullable = false, length = 1)
    private String clientType;

    /**
     * Portfolio Name
     * COBOL: PORT-PORTFOLIO-NAME PIC X(50)
     */
    @Size(max = 50, message = "Portfolio name must not exceed 50 characters")
    @Column(name = "portfolio_name", length = 50)
    private String portfolioName;

    /**
     * Currency Code
     * COBOL: PORT-CURRENCY PIC X(3)
     */
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    /**
     * Risk Level
     * Values: L=Low, M=Medium, H=High
     */
    @Pattern(regexp = "[LMH]", message = "Risk level must be L (Low), M (Medium), or H (High)")
    @Column(name = "risk_level", length = 1)
    private String riskLevel;

    /**
     * Portfolio Status
     * COBOL: PORT-STATUS PIC X(1)
     * Values: A=Active, C=Closed, S=Suspended
     */
    @NotNull(message = "Status is required")
    @Pattern(regexp = "[ACS]", message = "Status must be A (Active), C (Closed), or S (Suspended)")
    @Column(name = "status", nullable = false, length = 1)
    @Builder.Default
    private String status = "A";

    /**
     * Total Portfolio Value
     * COBOL: PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Total value is required")
    @DecimalMin(value = "-9999999999999.99", message = "Total value is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Total value exceeds maximum")
    @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    /**
     * Cash Balance
     * COBOL: PORT-CASH-BALANCE PIC S9(13)V99 COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Cash balance is required")
    @DecimalMin(value = "-9999999999999.99", message = "Cash balance is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Cash balance exceeds maximum")
    @Column(name = "cash_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cashBalance = BigDecimal.ZERO;

    /**
     * Portfolio Open Date
     * COBOL: PORT-CREATE-DATE PIC 9(8)
     */
    @NotNull(message = "Open date is required")
    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    /**
     * Portfolio Close Date
     * COBOL: PORT-CLOSE-DATE PIC 9(8)
     */
    @Column(name = "close_date")
    private LocalDate closeDate;

    /**
     * Check if portfolio is active
     */
    public boolean isActive() {
        return "A".equals(this.status);
    }

    /**
     * Check if portfolio is closed
     */
    public boolean isClosed() {
        return "C".equals(this.status);
    }

    /**
     * Check if portfolio is suspended
     */
    public boolean isSuspended() {
        return "S".equals(this.status);
    }

    /**
     * Check if client is individual
     */
    public boolean isIndividual() {
        return "I".equals(this.clientType);
    }

    /**
     * Check if client is corporate
     */
    public boolean isCorporate() {
        return "C".equals(this.clientType);
    }

    /**
     * Check if client is trust
     */
    public boolean isTrust() {
        return "T".equals(this.clientType);
    }
}
