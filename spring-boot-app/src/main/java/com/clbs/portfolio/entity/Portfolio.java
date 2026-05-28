package com.clbs.portfolio.entity;

import com.clbs.portfolio.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "account_no", length = 10, nullable = false)
    private String accountNo;

    @Column(name = "client_name", length = 30)
    private String clientName;

    @Column(name = "client_type", length = 1)
    private String clientType;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "last_maint_date")
    private LocalDate lastMaintDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private EntityStatus status;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "last_user", length = 8)
    private String lastUser;

    @Column(name = "last_trans_date")
    private LocalDate lastTransDate;
}
