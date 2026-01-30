package com.portfolio.model.entity;

import com.portfolio.model.enums.ClientType;
import com.portfolio.model.enums.PortfolioStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "portfolios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 8)
    @Column(name = "portfolio_id", unique = true, nullable = false, length = 8)
    private String portfolioId;

    @NotBlank
    @Size(max = 10)
    @Column(name = "account_no", nullable = false, length = 10)
    private String accountNo;

    @Size(max = 30)
    @Column(name = "client_name", length = 30)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type")
    private ClientType clientType;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "last_maint_date")
    private LocalDate lastMaintDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PortfolioStatus status;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "total_units", precision = 15, scale = 4)
    private BigDecimal totalUnits;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Size(max = 8)
    @Column(name = "last_user", length = 8)
    private String lastUser;

    @Column(name = "last_trans_date")
    private LocalDate lastTransDate;

    @PrePersist
    protected void onCreate() {
        if (createDate == null) {
            createDate = LocalDate.now();
        }
        if (status == null) {
            status = PortfolioStatus.ACTIVE;
        }
        if (totalValue == null) {
            totalValue = BigDecimal.ZERO;
        }
        if (cashBalance == null) {
            cashBalance = BigDecimal.ZERO;
        }
        if (totalUnits == null) {
            totalUnits = BigDecimal.ZERO;
        }
        if (totalCost == null) {
            totalCost = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastMaintDate = LocalDate.now();
    }
}
