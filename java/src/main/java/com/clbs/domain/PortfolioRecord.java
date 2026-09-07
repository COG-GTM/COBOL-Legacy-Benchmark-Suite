package com.clbs.domain;

import java.math.BigDecimal;

/**
 * PORTFLIO.cpy PORT-RECORD.
 *
 * <p>COMP-3 amounts (PIC S9(13)V99) are {@link BigDecimal} scaled to 2. Running totals for units
 * and cost used by PORTTRAN are carried here as PORTTRAN's local redefinition of the record;
 * they follow POSREC precision (units scale 4, cost scale 2).
 */
public class PortfolioRecord {

    public static final int SCALE_AMOUNT = 2;
    public static final int SCALE_UNITS = 4;

    private String portId = "";
    private String accountNo = "";
    private String clientName = "";
    private char clientType = 'I';
    private int createDate;
    private int lastMaint;
    private char status = 'A';
    private BigDecimal totalValue = BigDecimal.ZERO.setScale(SCALE_AMOUNT);
    private BigDecimal cashBalance = BigDecimal.ZERO.setScale(SCALE_AMOUNT);
    private BigDecimal totalUnits = BigDecimal.ZERO.setScale(SCALE_UNITS);
    private BigDecimal totalCost = BigDecimal.ZERO.setScale(SCALE_AMOUNT);
    private String lastUser = "";
    private int lastTrans;

    /** PORT-KEY = PORT-ID + PORT-ACCOUNT-NO. */
    public String key() {
        return pad(portId, 8) + pad(accountNo, 10);
    }

    /** Fixed-format PORT-RECORD image as moved into AUD-BEFORE-IMAGE / AUD-AFTER-IMAGE. */
    public String image() {
        String text = key() + pad(clientName, 30) + clientType
                + String.format("%08d%08d", createDate, lastMaint) + status
                + String.format("%016.2f%016.2f", totalValue, cashBalance);
        return pad(text, 100);
    }

    private static String pad(String value, int length) {
        String v = value == null ? "" : value;
        if (v.length() >= length) {
            return v.substring(0, length);
        }
        return v + " ".repeat(length - v.length());
    }

    public String getPortId() {
        return portId;
    }

    public void setPortId(String portId) {
        this.portId = portId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public char getClientType() {
        return clientType;
    }

    public void setClientType(char clientType) {
        this.clientType = clientType;
    }

    public int getCreateDate() {
        return createDate;
    }

    public void setCreateDate(int createDate) {
        this.createDate = createDate;
    }

    public int getLastMaint() {
        return lastMaint;
    }

    public void setLastMaint(int lastMaint) {
        this.lastMaint = lastMaint;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = scale(totalValue, SCALE_AMOUNT);
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = scale(cashBalance, SCALE_AMOUNT);
    }

    public BigDecimal getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(BigDecimal totalUnits) {
        this.totalUnits = scale(totalUnits, SCALE_UNITS);
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = scale(totalCost, SCALE_AMOUNT);
    }

    public String getLastUser() {
        return lastUser;
    }

    public void setLastUser(String lastUser) {
        this.lastUser = lastUser;
    }

    public int getLastTrans() {
        return lastTrans;
    }

    public void setLastTrans(int lastTrans) {
        this.lastTrans = lastTrans;
    }

    private static BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? BigDecimal.ZERO.setScale(scale) : value.setScale(scale, java.math.RoundingMode.HALF_UP);
    }

    public PortfolioRecord copy() {
        PortfolioRecord copy = new PortfolioRecord();
        copy.portId = portId;
        copy.accountNo = accountNo;
        copy.clientName = clientName;
        copy.clientType = clientType;
        copy.createDate = createDate;
        copy.lastMaint = lastMaint;
        copy.status = status;
        copy.totalValue = totalValue;
        copy.cashBalance = cashBalance;
        copy.totalUnits = totalUnits;
        copy.totalCost = totalCost;
        copy.lastUser = lastUser;
        copy.lastTrans = lastTrans;
        return copy;
    }
}
