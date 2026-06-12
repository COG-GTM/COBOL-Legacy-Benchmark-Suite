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
 * Portfolio Master Record — JPA mapping of PORTFLIO.cpy (PORT-RECORD).
 * Field order and types follow the copybook exactly.
 */
@Entity
@Table(name = "portfolio_master")
@Getter
@Setter
@NoArgsConstructor
public class PortfolioMaster {

    @EmbeddedId
    private PortfolioKey key;

    /** PORT-CLIENT-NAME PIC X(30). */
    @Column(name = "port_client_name", length = 30, nullable = false)
    private String clientName;

    /** PORT-CLIENT-TYPE PIC X(1): I=Individual, C=Corporate, T=Trust. */
    @Column(name = "port_client_type", length = 1, nullable = false)
    private String clientType;

    /** PORT-CREATE-DATE PIC 9(8) (YYYYMMDD). */
    @Column(name = "port_create_date", nullable = false)
    private Integer createDate;

    /** PORT-LAST-MAINT PIC 9(8) (YYYYMMDD). */
    @Column(name = "port_last_maint", nullable = false)
    private Integer lastMaint;

    /** PORT-STATUS PIC X(1): A=Active, C=Closed, S=Suspended. */
    @Column(name = "port_status", length = 1, nullable = false)
    private String status;

    /** PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3. */
    @Column(name = "port_total_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalValue;

    /** PORT-CASH-BALANCE PIC S9(13)V99 COMP-3. */
    @Column(name = "port_cash_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal cashBalance;

    /** PORT-LAST-USER PIC X(8). */
    @Column(name = "port_last_user", length = 8, nullable = false)
    private String lastUser;

    /** PORT-LAST-TRANS PIC 9(8). */
    @Column(name = "port_last_trans", nullable = false)
    private Integer lastTrans;

    /** PORT-FILLER PIC X(50). */
    @Column(name = "port_filler", length = 50)
    private String filler;
}
