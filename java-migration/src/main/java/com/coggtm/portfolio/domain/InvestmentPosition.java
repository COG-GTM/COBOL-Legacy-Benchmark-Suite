package com.coggtm.portfolio.domain;

import com.coggtm.portfolio.domain.enums.PositionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity mapped from COBOL copybook POSREC.cpy and DB2 table INVESTMENT_POSITIONS.
 *
 * <p>COBOL field mapping:</p>
 * <ul>
 *   <li>POS-KEY composite (portfolio + date + investment) → @EmbeddedId</li>
 *   <li>POS-QUANTITY (PIC S9(11)V9(4) COMP-3) → quantity (BigDecimal 15,4)</li>
 *   <li>POS-COST-BASIS (PIC S9(13)V9(2) COMP-3) → costBasis (BigDecimal 15,2)</li>
 *   <li>POS-MARKET-VALUE (PIC S9(13)V9(2) COMP-3) → marketValue (BigDecimal 15,2)</li>
 *   <li>POS-STATUS 88-levels → PositionStatus enum</li>
 * </ul>
 */
@Entity
@Table(name = "INVESTMENT_POSITIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentPosition {

    @EmbeddedId
    private InvestmentPositionId id;

    @NotNull
    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @NotNull
    @Column(name = "COST_BASIS", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @NotNull
    @Column(name = "MARKET_VALUE", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @NotNull
    @Size(max = 3)
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 1)
    private PositionStatus status;

    @NotNull
    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private LocalDateTime lastMaintDate;

    @NotNull
    @Size(max = 8)
    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;
}
