package com.clbs.portfolio.entity;

import com.clbs.portfolio.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "position")
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

    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    @Column(name = "investment_id", length = 10, nullable = false)
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
    @Column(name = "status", length = 10, nullable = false)
    private EntityStatus status;

    @Column(name = "last_maint_date")
    private LocalDateTime lastMaintDate;

    @Column(name = "last_maint_user", length = 8)
    private String lastMaintUser;
}
