package com.portfolio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Position entity - migrated from COBOL copybook POSREC.cpy
 * Represents portfolio position records
 */
@Entity
@Table(name = "positions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(PositionId.class)
public class Position {

    @Id
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Id
    @Column(name = "position_date")
    private LocalDate positionDate;

    @Id
    @Column(name = "investment_id", length = 10)
    private String investmentId;

    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 15, scale = 2)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 1)
    private PositionStatus status;

    @Column(name = "last_maint_date")
    private LocalDateTime lastMaintDate;

    @Column(name = "last_maint_user", length = 8)
    private String lastMaintUser;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastMaintDate = LocalDateTime.now();
    }

    public enum PositionStatus {
        A, // Active
        C, // Closed
        P  // Pending
    }
}
