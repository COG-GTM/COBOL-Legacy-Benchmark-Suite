package com.portfolio.modernization.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio", indexes = {
    @Index(name = "idx_portfolio_client", columnList = "client_id, status"),
    @Index(name = "idx_portfolio_account", columnList = "account_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "account_number", length = 10, nullable = false)
    private String accountNumber;

    @Column(name = "client_id", length = 10, nullable = false)
    private String clientId;

    @Column(name = "client_name", length = 30, nullable = false)
    private String clientName;

    @Column(name = "client_type", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private ClientType clientType;

    @Column(name = "portfolio_name", length = 50)
    private String portfolioName;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "risk_level", length = 1)
    private String riskLevel;

    @Column(name = "status", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private PortfolioStatus status;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;

    @Column(name = "last_maintenance_user", length = 8)
    private String lastMaintenanceUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
