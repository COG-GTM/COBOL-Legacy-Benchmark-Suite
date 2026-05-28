package com.clbs.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "history_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", length = 10, nullable = false)
    private String accountNo;

    @Column(name = "portfolio_id", length = 10, nullable = false)
    private String portfolioId;

    @Column(name = "trans_date", length = 10, nullable = false)
    private String transDate;

    @Column(name = "trans_time", length = 8, nullable = false)
    private String transTime;

    @Column(name = "trans_type", length = 2, nullable = false)
    private String transType;

    @Column(name = "security_id", length = 12, nullable = false)
    private String securityId;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "fees", precision = 18, scale = 2)
    private BigDecimal fees;

    @Column(name = "total_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "cost_basis", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "gain_loss", precision = 18, scale = 2, nullable = false)
    private BigDecimal gainLoss;

    @Column(name = "status", length = 10)
    private String status;
}
