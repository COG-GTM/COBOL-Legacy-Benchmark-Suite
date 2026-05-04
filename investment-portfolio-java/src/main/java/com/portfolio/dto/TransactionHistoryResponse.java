package com.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionHistoryResponse {

    private LocalDate date;
    private String type;
    private BigDecimal units;
    private BigDecimal price;
    private BigDecimal amount;

    public TransactionHistoryResponse() {
    }

    public TransactionHistoryResponse(LocalDate date, String type, BigDecimal units,
                                      BigDecimal price, BigDecimal amount) {
        this.date = date;
        this.type = type;
        this.units = units;
        this.price = price;
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public void setUnits(BigDecimal units) {
        this.units = units;
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
}
