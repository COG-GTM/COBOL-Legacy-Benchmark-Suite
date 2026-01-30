package com.portfolio.model.entity;

import com.portfolio.model.enums.PositionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 8)
    @Column(name = "portfolio_id", nullable = false, length = 8)
    private String portfolioId;

    @Column(name = "position_date")
    private LocalDate positionDate;

    @Size(max = 10)
    @Column(name = "investment_id", length = 10)
    private String investmentId;

    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 15, scale = 2)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Size(max = 3)
    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PositionStatus status;

    @Column(name = "last_maint_date")
    private LocalDateTime lastMaintDate;

    @Size(max = 8)
    @Column(name = "last_maint_user", length = 8)
    private String lastMaintUser;

    @PrePersist
    protected void onCreate() {
        if (positionDate == null) {
            positionDate = LocalDate.now();
        }
        if (status == null) {
            status = PositionStatus.ACTIVE;
        }
        if (currency == null) {
            currency = "USD";
        }
        if (quantity == null) {
            quantity = BigDecimal.ZERO;
        }
        if (costBasis == null) {
            costBasis = BigDecimal.ZERO;
        }
        if (marketValue == null) {
            marketValue = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastMaintDate = LocalDateTime.now();
    }
}
