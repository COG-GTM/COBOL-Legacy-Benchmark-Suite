package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity mapped from COBOL copybook PORTFLIO.cpy (Portfolio Master Record).
 * <p>
 * COBOL record layout (VSAM KSDS, 400-byte fixed-length record):
 * <pre>
 * 01  PORT-RECORD.
 *     05  PORT-KEY.
 *         10  PORT-ID             PIC X(8)
 *         10  PORT-ACCOUNT-NO     PIC X(10)
 *     05  PORT-CLIENT-INFO.
 *         10  PORT-CLIENT-NAME    PIC X(30)
 *         10  PORT-CLIENT-TYPE    PIC X(1)   [I=Individual, C=Corporate, T=Trust]
 *     05  PORT-PORTFOLIO-INFO.
 *         10  PORT-CREATE-DATE    PIC 9(8)
 *         10  PORT-LAST-MAINT     PIC 9(8)
 *         10  PORT-STATUS         PIC X(1)   [A=Active, C=Closed, S=Suspended]
 *     05  PORT-FINANCIAL-INFO.
 *         10  PORT-TOTAL-VALUE    PIC S9(13)V99 COMP-3
 *         10  PORT-CASH-BALANCE   PIC S9(13)V99 COMP-3
 *     05  PORT-AUDIT-INFO.
 *         10  PORT-LAST-USER      PIC X(8)
 *         10  PORT-LAST-TRANS     PIC 9(8)
 *     05  PORT-FILLER            PIC X(50)
 * </pre>
 */
@Entity
@Table(name = "portfolio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    /** PORT-ID — PIC X(8). Portfolio identifier, must start with 'PORT' followed by 4 digits. */
    @Id
    @Column(name = "port_id", length = 8, nullable = false)
    private String portId;

    /** PORT-ACCOUNT-NO — PIC X(10). Account number, 10-digit numeric string. */
    @Column(name = "account_no", length = 10, nullable = false)
    private String accountNo;

    /** PORT-CLIENT-NAME — PIC X(30). Client display name. */
    @Column(name = "client_name", length = 30, nullable = false)
    private String clientName;

    /** PORT-CLIENT-TYPE — PIC X(1). I=Individual, C=Corporate, T=Trust. */
    @Column(name = "client_type", length = 1, nullable = false)
    private String clientType;

    /** PORT-CREATE-DATE — PIC 9(8). Portfolio creation date (YYYYMMDD). */
    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;

    /** PORT-LAST-MAINT — PIC 9(8). Last maintenance date (YYYYMMDD). */
    @Column(name = "last_maint_date")
    private LocalDate lastMaintDate;

    /** PORT-STATUS — PIC X(1). A=Active, C=Closed, S=Suspended. */
    @Column(name = "status", length = 1, nullable = false)
    private String status;

    /** PORT-TOTAL-VALUE — PIC S9(13)V99 COMP-3. Total portfolio market value. */
    @Column(name = "total_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalValue;

    /** PORT-CASH-BALANCE — PIC S9(13)V99 COMP-3. Available cash balance. */
    @Column(name = "cash_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal cashBalance;

    /** PORT-LAST-USER — PIC X(8). Last user who modified the record. */
    @Column(name = "last_user", length = 8)
    private String lastUser;

    /** PORT-LAST-TRANS — PIC 9(8). Last transaction date (YYYYMMDD). */
    @Column(name = "last_trans_date")
    private LocalDate lastTransDate;
}
