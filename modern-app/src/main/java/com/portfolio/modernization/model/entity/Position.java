package com.portfolio.modernization.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "position", indexes = {
    @Index(name = "idx_position_date", columnList = "position_date, portfolio_id"),
    @Index(name = "idx_position_investment", columnList = "investment_id")
})
@IdClass(PositionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "status", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private PositionStatus status;

    @Column(name = "last_maintenance_user", length = 8)
    private String lastMaintenanceUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", insertable = false, updatable = false)
    private Portfolio portfolio;

    public enum PositionStatus {
        A, // Active
        C, // Closed
        P  // Pending
    }
}
