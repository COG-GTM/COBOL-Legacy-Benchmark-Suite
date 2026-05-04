package com.coggtm.portfolio.domain;

import com.coggtm.portfolio.domain.enums.AccountType;
import com.coggtm.portfolio.domain.enums.PortfolioStatus;
import com.coggtm.portfolio.domain.enums.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity mapped from COBOL copybook PORTFLIO.cpy and DB2 table PORTFOLIO_MASTER.
 *
 * <p>COBOL field mapping:</p>
 * <ul>
 *   <li>PORT-ID (PIC X(8)) → portfolioId</li>
 *   <li>PORT-TOTAL-VALUE (PIC S9(13)V99 COMP-3) → totalValue (BigDecimal 15,2)</li>
 *   <li>PORT-CASH-BALANCE (PIC S9(13)V99 COMP-3) → cashBalance (BigDecimal 15,2)</li>
 *   <li>PORT-STATUS 88-levels → PortfolioStatus enum</li>
 * </ul>
 */
@Entity
@Table(name = "PORTFOLIO_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ACCOUNT_TYPE", length = 2, nullable = false)
    private AccountType accountType;

    @NotNull
    @Size(max = 2)
    @Column(name = "BRANCH_ID", length = 2, nullable = false)
    private String branchId;

    @NotNull
    @Size(max = 10)
    @Column(name = "CLIENT_ID", length = 10, nullable = false)
    private String clientId;

    @NotNull
    @Size(max = 50)
    @Column(name = "PORTFOLIO_NAME", length = 50, nullable = false)
    private String portfolioName;

    @NotNull
    @Size(max = 3)
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "RISK_LEVEL", length = 1, nullable = false)
    private RiskLevel riskLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 1, nullable = false)
    private PortfolioStatus status;

    @Column(name = "TOTAL_VALUE", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "CASH_BALANCE", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    @NotNull
    @Column(name = "OPEN_DATE", nullable = false)
    private LocalDate openDate;

    @Column(name = "CLOSE_DATE")
    private LocalDate closeDate;

    @NotNull
    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private LocalDateTime lastMaintDate;

    @NotNull
    @Size(max = 8)
    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;
}
