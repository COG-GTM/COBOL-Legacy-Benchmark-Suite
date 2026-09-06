package com.cog.portfolio.domain;

import com.cog.portfolio.common.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** TRNREC.cpy TRANSACTION-RECORD (VSAM TRANFILE). Quantity and price keep scale 4. */
@Entity
@Table(name = "TRANSACTION_RECORD")
public class Transaction {

    @EmbeddedId
    private TransactionId id;

    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    @Convert(converter = TransactionType.Converter.class)
    @Column(name = "TRN_TYPE", length = 2, nullable = false)
    private TransactionType type;

    /** TRN-QUANTITY PIC S9(11)V9(4) COMP-3 */
    @Column(name = "QUANTITY", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO.setScale(4);

    /** TRN-PRICE PIC S9(11)V9(4) COMP-3 */
    @Column(name = "PRICE", precision = 15, scale = 4, nullable = false)
    private BigDecimal price = BigDecimal.ZERO.setScale(4);

    /** TRN-AMOUNT PIC S9(13)V9(2) COMP-3 */
    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO.setScale(2);

    @Enumerated(EnumType.STRING)
    @Column(name = "CURRENCY", length = 3, nullable = false)
    private CurrencyCode currency = CurrencyCode.USD;

    @Convert(converter = TransactionStatus.Converter.class)
    @Column(name = "STATUS", length = 1, nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "PROCESS_DATE")
    private LocalDateTime processDate;

    @Column(name = "PROCESS_USER", length = 8)
    private String processUser;

    protected Transaction() {
    }

    public Transaction(TransactionId id, String investmentId, TransactionType type) {
        this.id = id;
        this.investmentId = investmentId;
        this.type = type;
    }

    public TransactionId getId() {
        return id;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyCode currency) {
        this.currency = currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public LocalDateTime getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDateTime processDate) {
        this.processDate = processDate;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
