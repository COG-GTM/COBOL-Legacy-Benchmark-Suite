package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mirrors HIST-KEY in HISTREC.cpy (portfolio id + date + time + seq no). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HistoryKey implements Serializable {

    /** HIST-PORTFOLIO-ID PIC X(08). */
    @Column(name = "hist_portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** HIST-DATE PIC X(08) (YYYYMMDD). */
    @Column(name = "hist_date", length = 8, nullable = false)
    private String histDate;

    /** HIST-TIME PIC X(06) (HHMMSS). */
    @Column(name = "hist_time", length = 6, nullable = false)
    private String histTime;

    /** HIST-SEQ-NO PIC X(04). */
    @Column(name = "hist_seq_no", length = 4, nullable = false)
    private String seqNo;
}
