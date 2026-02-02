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
 * Portfolio entity - migrated from COBOL copybook PORTFLIO.cpy
 * Represents the Portfolio Master Record
 */
@Entity
@Table(name = "portfolios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "account_no", length = 10, nullable = false)
    private String accountNo;

    @Column(name = "client_name", length = 30)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 1)
    private ClientType clientType;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "last_maint_date")
    private LocalDate lastMaintDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 1)
    private PortfolioStatus status;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "last_user", length = 8)
    private String lastUser;

    @Column(name = "last_trans_date")
    private LocalDate lastTransDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (createDate == null) {
            createDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastMaintDate = LocalDate.now();
    }

    public enum ClientType {
        I, // Individual
        C, // Corporate
        T  // Trust
    }

    public enum PortfolioStatus {
        A, // Active
        C, // Closed
        S  // Suspended
    }
}
