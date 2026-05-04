package com.coggtm.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Composite primary key for InvestmentPosition.
 * Maps to COBOL POS-KEY in POSREC.cpy (POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InvestmentPositionId implements Serializable {

    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "POSITION_DATE", nullable = false)
    private LocalDate positionDate;
}
