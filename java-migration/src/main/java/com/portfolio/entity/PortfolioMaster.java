package com.portfolio.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity mapped from COBOL copybook PORTFLIO.cpy (Portfolio Master Record).
 * <p>
 * Source copybook: src/copybook/common/PORTFLIO.cpy
 * VSAM file: PORTMSTR (KSDS, 148-byte fixed-length records)
 * COBOL program: PORTMSTR.cbl (CRUD operations)
 */
@Entity
@Table(name = "portfolio_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioMaster {

    /**
     * COBOL: PORT-ID PIC X(8)
     * Portfolio identifier, must start with 'PORT' followed by numeric digits.
     */
    @Id
    @Column(name = "port_id", length = 8, nullable = false)
    private String portId;

    /**
     * COBOL: PORT-ACCOUNT-NO PIC X(10)
     * Account number associated with this portfolio.
     */
    @Column(name = "port_account_no", length = 10, nullable = false)
    private String portAccountNo;

    /**
     * COBOL: PORT-CLIENT-NAME PIC X(30)
     * Client name for this portfolio.
     */
    @Column(name = "port_client_name", length = 30)
    private String portClientName;

    /**
     * COBOL: PORT-CLIENT-TYPE PIC X(1)
     * Client type: 'I' = Individual, 'C' = Corporate, 'T' = Trust.
     * Level 88: PORT-INDIVIDUAL='I', PORT-CORPORATE='C', PORT-TRUST='T'
     */
    @Column(name = "port_client_type", length = 1)
    private String portClientType;

    /**
     * COBOL: PORT-CREATE-DATE PIC 9(8)
     * Portfolio creation date (stored as YYYYMMDD in COBOL).
     */
    @Column(name = "port_create_date")
    private LocalDate portCreateDate;

    /**
     * COBOL: PORT-LAST-MAINT PIC 9(8)
     * Date of last maintenance (stored as YYYYMMDD in COBOL).
     */
    @Column(name = "port_last_maint")
    private LocalDate portLastMaint;

    /**
     * COBOL: PORT-STATUS PIC X(1)
     * Portfolio status: 'A' = Active, 'C' = Closed, 'S' = Suspended.
     * Level 88: PORT-ACTIVE='A', PORT-CLOSED='C', PORT-SUSPENDED='S'
     */
    @Column(name = "port_status", length = 1)
    private String portStatus;

    /**
     * COBOL: PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3
     * Total portfolio value. Packed decimal, 8 bytes in COBOL.
     */
    @Column(name = "port_total_value", precision = 15, scale = 2)
    private BigDecimal portTotalValue;

    /**
     * COBOL: PORT-CASH-BALANCE PIC S9(13)V99 COMP-3
     * Cash balance in the portfolio. Packed decimal, 8 bytes in COBOL.
     */
    @Column(name = "port_cash_balance", precision = 15, scale = 2)
    private BigDecimal portCashBalance;

    /**
     * COBOL: PORT-LAST-USER PIC X(8)
     * User ID of last person to modify this record.
     */
    @Column(name = "port_last_user", length = 8)
    private String portLastUser;

    /**
     * COBOL: PORT-LAST-TRANS PIC 9(8)
     * Date of last transaction (stored as YYYYMMDD in COBOL).
     */
    @Column(name = "port_last_trans")
    private LocalDate portLastTrans;
}
