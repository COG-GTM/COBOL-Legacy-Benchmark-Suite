package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Position Record — JPA mapping of POSREC.cpy (POSITION-RECORD).
 */
@Entity
@Table(name = "position_record")
@Getter
@Setter
@NoArgsConstructor
public class PositionRecord {

    @EmbeddedId
    private PositionKey key;

    /** POS-QUANTITY PIC S9(11)V9(4) COMP-3. */
    @Column(name = "pos_quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** POS-COST-BASIS PIC S9(13)V9(2) COMP-3. */
    @Column(name = "pos_cost_basis", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3. */
    @Column(name = "pos_market_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal marketValue;

    /** POS-CURRENCY PIC X(03). */
    @Column(name = "pos_currency", length = 3, nullable = false)
    private String currency;

    /** POS-STATUS PIC X(01): A=Active, C=Closed, P=Pending. */
    @Column(name = "pos_status", length = 1, nullable = false)
    private String status;

    /** POS-LAST-MAINT-DATE PIC X(26). */
    @Column(name = "pos_last_maint_date", length = 26, nullable = false)
    private String lastMaintDate;

    /** POS-LAST-MAINT-USER PIC X(08). */
    @Column(name = "pos_last_maint_user", length = 8, nullable = false)
    private String lastMaintUser;

    /** POS-FILLER PIC X(50). */
    @Column(name = "pos_filler", length = 50)
    private String filler;
}
