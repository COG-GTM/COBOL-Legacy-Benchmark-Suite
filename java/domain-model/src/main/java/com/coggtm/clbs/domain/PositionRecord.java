package com.coggtm.clbs.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions", indexes = {
    @Index(name = "idx_pos_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_pos_investment_id", columnList = "investment_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 10)
    @Column(name = "portfolio_id", nullable = false, length = 10)
    private String portfolioId;

    @NotNull
    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    @NotNull
    @Size(max = 10)
    @Column(name = "investment_id", nullable = false, length = 10)
    private String investmentId;

    @NotNull
    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @NotNull
    @Column(name = "cost_basis", nullable = false, precision = 15, scale = 2)
    private BigDecimal costBasis;

    @NotNull
    @Column(name = "market_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal marketValue;

    @NotNull
    @Size(max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull
    @Size(max = 1)
    @Column(name = "status", nullable = false, length = 1)
    private String status;

    @Column(name = "last_maintenance_date")
    private LocalDateTime lastMaintenanceDate;

    @Size(max = 8)
    @Column(name = "last_maintenance_user", length = 8)
    private String lastMaintenanceUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
