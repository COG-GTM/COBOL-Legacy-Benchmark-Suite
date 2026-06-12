package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mirrors PORT-KEY in PORTFLIO.cpy (PORT-ID + PORT-ACCOUNT-NO). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PortfolioKey implements Serializable {

    /** PORT-ID PIC X(8). */
    @Column(name = "port_id", length = 8, nullable = false)
    private String portId;

    /** PORT-ACCOUNT-NO PIC X(10). */
    @Column(name = "port_account_no", length = 10, nullable = false)
    private String accountNo;
}
