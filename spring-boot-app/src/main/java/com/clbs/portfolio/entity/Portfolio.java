package com.clbs.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "account_no", length = 10, nullable = false)
    private String accountNo;

    @Column(name = "client_name", length = 30)
    private String clientName;

    @Column(name = "client_type", length = 1)
    private String clientType;

    @Column(name = "create_date", length = 8)
    private String createDate;

    @Column(name = "last_maint", length = 8)
    private String lastMaint;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "total_value", precision = 18, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 18, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "last_user", length = 8)
    private String lastUser;

    @Column(name = "last_trans", length = 8)
    private String lastTrans;
}
