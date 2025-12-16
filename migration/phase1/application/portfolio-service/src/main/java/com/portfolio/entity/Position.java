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
 * Position entity - migrated from POSFILE VSAM file.
 * 
 * Original COBOL structure from POSREC.cpy:
 * - Key: Portfolio ID (8 bytes) + Position Date (8 bytes) + Investment ID (10 bytes)
 * - Record Length: 200 bytes (as defined in PORTDFN.csd)
 * 
 * @see src/copybook/common/POSREC.cpy
 * @see src/cics/PORTDFN.csd - POSFILE definition
 */
@Entity
@Table(name = "positions", schema = "portfolio",
       uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "position_date", "investment_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false, length = 8)
    private String portfolioId;

    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    @Column(name = "investment_id", nullable = false, length = 10)
    private String investmentId;

    @Column(name = "cusip", length = 9)
    private String cusip;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "cost_basis", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal costBasis = BigDecimal.ZERO;

    @Column(name = "market_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal marketValue = BigDecimal.ZERO;

    @Column(name = "average_cost", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PositionStatus status = PositionStatus.ACTIVE;

    @Column(name = "last_transaction_id", length = 20)
    private String lastTransactionId;

    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;

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

    public enum PositionStatus {
        ACTIVE,
        CLOSED,
        PENDING
    }
}
