package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mirrors POS-KEY in POSREC.cpy (portfolio id + date + investment id). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PositionKey implements Serializable {

    /** POS-PORTFOLIO-ID PIC X(08). */
    @Column(name = "pos_portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** POS-DATE PIC X(08) (YYYYMMDD). */
    @Column(name = "pos_date", length = 8, nullable = false)
    private String posDate;

    /** POS-INVESTMENT-ID PIC X(10). */
    @Column(name = "pos_investment_id", length = 10, nullable = false)
    private String investmentId;
}
