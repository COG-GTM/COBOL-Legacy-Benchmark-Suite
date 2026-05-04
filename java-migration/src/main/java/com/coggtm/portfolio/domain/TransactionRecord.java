package com.coggtm.portfolio.domain;

import com.coggtm.portfolio.domain.enums.TransactionStatus;
import com.coggtm.portfolio.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JPA entity mapped from COBOL copybook TRNREC.cpy and DB2 table TRANSACTION_HISTORY.
 *
 * <p>COBOL field mapping:</p>
 * <ul>
 *   <li>TRN-KEY composite → transactionId (CHAR 20, format YYYYMMDDHHMMSS + seq)</li>
 *   <li>TRN-TYPE 88-levels → TransactionType enum (BU/SL/TR/FE)</li>
 *   <li>TRN-QUANTITY (PIC S9(11)V9(4) COMP-3) → quantity (BigDecimal 15,4)</li>
 *   <li>TRN-PRICE (PIC S9(11)V9(4) COMP-3) → price (BigDecimal 15,4)</li>
 *   <li>TRN-AMOUNT (PIC S9(13)V9(2) COMP-3) → amount (BigDecimal 15,2)</li>
 *   <li>TRN-STATUS 88-levels → TransactionStatus enum</li>
 * </ul>
 */
@Entity
@Table(name = "TRANSACTION_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {

    @Id
    @Column(name = "TRANSACTION_ID", length = 20, nullable = false)
    private String transactionId;

    @NotNull
    @Size(max = 8)
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @NotNull
    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @NotNull
    @Column(name = "TRANSACTION_TIME", nullable = false)
    private LocalTime transactionTime;

    @NotNull
    @Size(max = 10)
    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_TYPE", length = 2, nullable = false)
    private TransactionType transactionType;

    @NotNull
    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @NotNull
    @Column(name = "PRICE", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    @NotNull
    @Column(name = "AMOUNT", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Size(max = 3)
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 1, nullable = false)
    private TransactionStatus status;

    @NotNull
    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDateTime processDate;

    @NotNull
    @Size(max = 8)
    @Column(name = "PROCESS_USER", length = 8, nullable = false)
    private String processUser;
}
