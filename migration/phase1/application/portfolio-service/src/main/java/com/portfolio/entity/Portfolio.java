package com.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Portfolio entity - migrated from PORTMSTR VSAM file.
 * 
 * Original COBOL structure:
 * - Key: Portfolio ID (8 bytes) + Account Type (2 bytes) + Branch ID (2 bytes)
 * - Record Length: 400 bytes
 * 
 * @see src/database/vsam/vsam-definitions.txt
 */
@Entity
@Table(name = "portfolios", schema = "portfolio",
       uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "account_type", "branch_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false, length = 8)
    private String portfolioId;

    @Column(name = "account_type", nullable = false, length = 2)
    private String accountType;

    @Column(name = "branch_id", nullable = false, length = 2)
    private String branchId;

    @Column(name = "client_id", nullable = false, length = 10)
    private String clientId;

    @Column(name = "portfolio_name", nullable = false, length = 50)
    private String portfolioName;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Column(name = "risk_level", nullable = false, length = 1)
    @Builder.Default
    private String riskLevel = "M";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PortfolioStatus status = PortfolioStatus.ACTIVE;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "total_value", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(name = "total_cost_basis", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalCostBasis = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", length = 8)
    private String createdBy;

    @Column(name = "updated_by", length = 8)
    private String updatedBy;

    public enum PortfolioStatus {
        ACTIVE,
        CLOSED,
        SUSPENDED
    }
}
