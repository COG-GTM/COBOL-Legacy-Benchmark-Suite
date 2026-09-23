package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** DB2 {@code POSHIST} (V2); host variables in {@code DBTBLS.cpy POSHIST-RECORD}. */
@Entity
@Table(name = "POSHIST")
public class PositionHistory {

  @EmbeddedId private PositionHistoryKey id;

  @Column(name = "TRANS_TYPE", nullable = false, length = 2)
  private String transactionType;

  @Column(name = "SECURITY_ID", nullable = false, length = 12)
  private String securityId;

  @Column(name = "QUANTITY", nullable = false, precision = 15, scale = 3)
  private BigDecimal quantity;

  @Column(name = "PRICE", nullable = false, precision = 15, scale = 3)
  private BigDecimal price;

  @Column(name = "AMOUNT", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(name = "FEES", nullable = false, precision = 15, scale = 2)
  private BigDecimal fees;

  @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "COST_BASIS", nullable = false, precision = 15, scale = 2)
  private BigDecimal costBasis;

  @Column(name = "GAIN_LOSS", nullable = false, precision = 15, scale = 2)
  private BigDecimal gainLoss;

  @Column(name = "PROCESS_DATE", nullable = false)
  private LocalDate processDate;

  @Column(name = "PROCESS_TIME", nullable = false)
  private LocalTime processTime;

  @Column(name = "PROGRAM_ID", nullable = false, length = 8)
  private String programId;

  @Column(name = "USER_ID", nullable = false, length = 8)
  private String userId;

  @Column(name = "AUDIT_TIMESTAMP", nullable = false)
  private LocalDateTime auditTimestamp;

  protected PositionHistory() {}

  public PositionHistory(
      PositionHistoryKey id,
      String transactionType,
      String securityId,
      BigDecimal quantity,
      BigDecimal price,
      BigDecimal amount,
      BigDecimal fees,
      BigDecimal totalAmount,
      BigDecimal costBasis,
      BigDecimal gainLoss,
      LocalDate processDate,
      LocalTime processTime,
      String programId,
      String userId,
      LocalDateTime auditTimestamp) {
    this.id = id;
    this.transactionType = transactionType;
    this.securityId = securityId;
    this.quantity = quantity;
    this.price = price;
    this.amount = amount;
    this.fees = fees;
    this.totalAmount = totalAmount;
    this.costBasis = costBasis;
    this.gainLoss = gainLoss;
    this.processDate = processDate;
    this.processTime = processTime;
    this.programId = programId;
    this.userId = userId;
    this.auditTimestamp = auditTimestamp;
  }

  public PositionHistoryKey getId() {
    return id;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
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
