package com.clbs.portfolio.domain;

import com.clbs.portfolio.domain.enums.TransactionStatus;
import com.clbs.portfolio.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_record")
public class TransactionRecord {
    @EmbeddedId
    @NotNull
    private TransactionRecordKey key;

    @NotNull
    @Size(max = 10)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "investment_id", length = 10, nullable = false, columnDefinition = "CHAR(10)")
    private String investmentId;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "transaction_type", length = 2, nullable = false, columnDefinition = "CHAR(2)")
    private TransactionType transactionType;

    @NotNull
    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @NotNull
    @Column(name = "price", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    @NotNull
    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Size(max = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "status", length = 1, nullable = false, columnDefinition = "CHAR(1)")
    private TransactionStatus status;

    @NotNull
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @NotNull
    @Size(max = 8)
    @Column(name = "process_user", length = 8, nullable = false)
    private String processUser;

    protected TransactionRecord() {
    }

    public TransactionRecord(TransactionRecordKey key, String investmentId, TransactionType transactionType,
                             BigDecimal quantity, BigDecimal price, BigDecimal amount, String currencyCode,
                             TransactionStatus status, Instant processedAt, String processUser) {
        this.key = key;
        this.investmentId = investmentId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.price = price;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.status = status;
        this.processedAt = processedAt;
        this.processUser = processUser;
    }

    public TransactionRecordKey getKey() {
        return key;
    }

    public void setKey(TransactionRecordKey key) {
        this.key = key;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
