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
 * Transaction Record — JPA mapping of TRNREC.cpy (TRANSACTION-RECORD).
 */
@Entity
@Table(name = "transaction_record")
@Getter
@Setter
@NoArgsConstructor
public class TransactionRecord {

    @EmbeddedId
    private TransactionKey key;

    /** TRN-INVESTMENT-ID PIC X(10). */
    @Column(name = "trn_investment_id", length = 10, nullable = false)
    private String investmentId;

    /** TRN-TYPE PIC X(02): BU=Buy, SL=Sell, TR=Transfer, FE=Fee. */
    @Column(name = "trn_type", length = 2, nullable = false)
    private String type;

    /** TRN-QUANTITY PIC S9(11)V9(4) COMP-3. */
    @Column(name = "trn_quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** TRN-PRICE PIC S9(11)V9(4) COMP-3. */
    @Column(name = "trn_price", precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    /** TRN-AMOUNT PIC S9(13)V9(2) COMP-3. */
    @Column(name = "trn_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /** TRN-CURRENCY PIC X(03). */
    @Column(name = "trn_currency", length = 3, nullable = false)
    private String currency;

    /** TRN-STATUS PIC X(01): P=Pending, D=Done, F=Failed, R=Reversed. */
    @Column(name = "trn_status", length = 1, nullable = false)
    private String status;

    /** TRN-PROCESS-DATE PIC X(26) (timestamp). */
    @Column(name = "trn_process_date", length = 26, nullable = false)
    private String processDate;

    /** TRN-PROCESS-USER PIC X(08). */
    @Column(name = "trn_process_user", length = 8, nullable = false)
    private String processUser;

    /** TRN-FILLER PIC X(50). */
    @Column(name = "trn_filler", length = 50)
    private String filler;
}
