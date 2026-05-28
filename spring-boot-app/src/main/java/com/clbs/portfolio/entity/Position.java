package com.clbs.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "position_date", length = 8)
    private String positionDate;

    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "last_maint_date")
    private LocalDateTime lastMaintDate;

    @Column(name = "last_maint_user", length = 8)
    private String lastMaintUser;

    @Column(name = "realized_gain_loss", precision = 18, scale = 2)
    private BigDecimal realizedGainLoss;
}
