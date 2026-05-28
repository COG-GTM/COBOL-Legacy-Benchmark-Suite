package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Portfolio master record.
 * From COBOL copybook: src/copybook/common/PORTFLIO.cpy (PORT-RECORD).
 */
@Entity
@Table(name = "portfolio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    /** PORT-ID — PIC X(8) */
    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** PORT-ACCOUNT-NO — PIC X(10) */
    @Column(name = "account_no", length = 10, nullable = false)
    private String accountNo;

    /** PORT-CLIENT-NAME — PIC X(30) */
    @Column(name = "client_name", length = 30)
    private String clientName;

    /** PORT-CLIENT-TYPE — PIC X(1): I=Individual, C=Corporate, T=Trust */
    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 12, nullable = false)
    private ClientType clientType;

    /** PORT-CREATE-DATE — PIC 9(8) */
    @Column(name = "create_date")
    private LocalDate createDate;

    /** PORT-LAST-MAINT — PIC 9(8) */
    @Column(name = "last_maint_date")
    private LocalDate lastMaintDate;

    /** PORT-STATUS — PIC X(1): A=Active, C=Closed, S=Suspended */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 12, nullable = false)
    private PortfolioStatus status;

    /** PORT-TOTAL-VALUE — PIC S9(13)V99 COMP-3 */
    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    /** PORT-CASH-BALANCE — PIC S9(13)V99 COMP-3 */
    @Column(name = "cash_balance", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    /** PORT-LAST-USER — PIC X(8) */
    @Column(name = "last_user", length = 8)
    private String lastUser;

    /** PORT-LAST-TRANS — PIC 9(8) */
    @Column(name = "last_trans_date")
    private LocalDate lastTransDate;

    /** PORT-FILLER — PIC X(50), reserved for future use */
    @Column(name = "filler", length = 50)
    private String filler;

    public enum ClientType {
        INDIVIDUAL,
        CORPORATE,
        TRUST
    }

    public enum PortfolioStatus {
        ACTIVE,
        CLOSED,
        SUSPENDED
    }
}
