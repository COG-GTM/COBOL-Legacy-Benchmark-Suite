package com.clbs.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "position_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", length = 8)
    private String accountNo;

    @Column(name = "portfolio_id", length = 10)
    private String portfolioId;

    @Column(name = "trans_date", length = 10)
    private String transDate;

    @Column(name = "trans_time", length = 8)
    private String transTime;

    @Column(name = "trans_type", length = 2)
    private String transType;

    @Column(name = "security_id", length = 12)
    private String securityId;

    @Column(name = "quantity", precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 3)
    private BigDecimal price;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "fees", precision = 15, scale = 2)
    private BigDecimal fees;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "cost_basis", precision = 15, scale = 2)
    private BigDecimal costBasis;

    @Column(name = "gain_loss", precision = 15, scale = 2)
    private BigDecimal gainLoss;

    @Column(name = "process_date", length = 10)
    private String processDateStr;

    @Column(name = "process_time", length = 8)
    private String processTime;

    @Column(name = "program_id", length = 8)
    private String programId;

    @Column(name = "user_id", length = 8)
    private String userId;

    @Column(name = "audit_timestamp")
    private LocalDateTime auditTimestamp;
}
