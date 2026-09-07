package com.portfolio.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "POSHIST")
public class PositionHistory {
  @EmbeddedId private PositionHistoryId id;

  @Column(name = "TRANS_TYPE", columnDefinition = "char(2)")
  private String transType;

  @Column(name = "SECURITY_ID", columnDefinition = "char(12)")
  private String securityId;

  @Column(precision = 15, scale = 3)
  private BigDecimal quantity;

  @Column(precision = 15, scale = 3)
  private BigDecimal price;

  @Column(precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(precision = 15, scale = 2)
  private BigDecimal fees;

  @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "COST_BASIS", precision = 15, scale = 2)
  private BigDecimal costBasis;

  @Column(name = "GAIN_LOSS", precision = 15, scale = 2)
  private BigDecimal gainLoss;

  private LocalDate processDate;
  private LocalTime processTime;

  @Column(name = "PROGRAM_ID", columnDefinition = "char(8)")
  private String programId;

  @Column(name = "USER_ID", columnDefinition = "char(8)")
  private String userId;

  private LocalDateTime auditTimestamp;

  public PositionHistory() {}

  public PositionHistoryId getId() {
    return id;
  }

  public void setId(PositionHistoryId id) {
    this.id = id;
  }

  public String getTransType() {
    return transType;
  }

  public void setTransType(String transType) {
    this.transType = transType;
  }

  public String getSecurityId() {
    return securityId;
  }

  public void setSecurityId(String securityId) {
    this.securityId = securityId;
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

  public BigDecimal getFees() {
    return fees;
  }

  public void setFees(BigDecimal fees) {
    this.fees = fees;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public BigDecimal getCostBasis() {
    return costBasis;
  }

  public void setCostBasis(BigDecimal costBasis) {
    this.costBasis = costBasis;
  }

  public BigDecimal getGainLoss() {
    return gainLoss;
  }

  public void setGainLoss(BigDecimal gainLoss) {
    this.gainLoss = gainLoss;
  }

  public LocalDate getProcessDate() {
    return processDate;
  }

  public void setProcessDate(LocalDate processDate) {
    this.processDate = processDate;
  }

  public LocalTime getProcessTime() {
    return processTime;
  }

  public void setProcessTime(LocalTime processTime) {
    this.processTime = processTime;
  }

  public String getProgramId() {
    return programId;
  }

  public void setProgramId(String programId) {
    this.programId = programId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public LocalDateTime getAuditTimestamp() {
    return auditTimestamp;
  }

  public void setAuditTimestamp(LocalDateTime auditTimestamp) {
    this.auditTimestamp = auditTimestamp;
  }
}
