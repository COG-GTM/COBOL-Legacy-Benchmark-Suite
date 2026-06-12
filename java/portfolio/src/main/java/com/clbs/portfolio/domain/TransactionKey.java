package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mirrors TRN-KEY in TRNREC.cpy (date + time + portfolio id + sequence). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TransactionKey implements Serializable {

    /** TRN-DATE PIC X(08) (YYYYMMDD). */
    @Column(name = "trn_date", length = 8, nullable = false)
    private String trnDate;

    /** TRN-TIME PIC X(06) (HHMMSS). */
    @Column(name = "trn_time", length = 6, nullable = false)
    private String trnTime;

    /** TRN-PORTFOLIO-ID PIC X(08). */
    @Column(name = "trn_portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** TRN-SEQUENCE-NO PIC X(06). */
    @Column(name = "trn_sequence_no", length = 6, nullable = false)
    private String sequenceNo;
}
