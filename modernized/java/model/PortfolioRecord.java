package com.clbs.portfolio.model;

import java.math.BigDecimal;

/**
 * Translation of {@code 01 PORT-RECORD} in {@code src/copybook/common/PORTFLIO.cpy}, the record
 * area of the indexed {@code PORTFOLIO-FILE} keyed on {@code PORT-ID}.
 *
 * <pre>
 * 05 PORT-KEY
 *    10 PORT-ID             PIC X(8)         -&gt; RECORD KEY
 *    10 PORT-ACCOUNT-NO     PIC X(10)
 * 05 PORT-CLIENT-INFO
 *    10 PORT-CLIENT-NAME    PIC X(30)
 *    10 PORT-CLIENT-TYPE    PIC X(1)         -&gt; {@link ClientType}
 * 05 PORT-PORTFOLIO-INFO
 *    10 PORT-CREATE-DATE    PIC 9(8)
 *    10 PORT-LAST-MAINT     PIC 9(8)
 *    10 PORT-STATUS         PIC X(1)         -&gt; {@link PortfolioStatus}
 * 05 PORT-FINANCIAL-INFO
 *    10 PORT-TOTAL-VALUE    PIC S9(13)V99 COMP-3
 *    10 PORT-CASH-BALANCE   PIC S9(13)V99 COMP-3
 * 05 PORT-AUDIT-INFO
 *    10 PORT-LAST-USER      PIC X(8)
 *    10 PORT-LAST-TRANS     PIC 9(8)
 * 05 PORT-FILLER            PIC X(50)
 * </pre>
 *
 * <h2>Synthetic holdings fields</h2>
 *
 * <p>{@code PORTTRAN} copies a copybook named {@code PORTREC} that does not exist in the repository
 * and updates two fields, {@code PORT-TOTAL-UNITS} and {@code PORT-TOTAL-COST}, that no copybook
 * defines. {@link #getPortTotalUnits()} and {@link #getPortTotalCost()} below are the deliberate
 * stand-ins for them, typed from the closest documented equivalents in {@code POSREC.cpy}
 * ({@code POS-QUANTITY} {@code PIC S9(11)V9(4)} and {@code POS-COST-BASIS} {@code PIC S9(13)V9(2)}).
 * They are <em>not</em> part of the {@code PORTFLIO.cpy} layout: they are excluded from
 * {@link #toRecordImage()} and the reasoning is recorded in {@code TRANSLATION-NOTES.md}.
 */
public class PortfolioRecord {

    public static final int ID_LENGTH = 8;
    public static final int ACCOUNT_NO_LENGTH = 10;
    public static final int CLIENT_NAME_LENGTH = 30;
    public static final int DATE_DIGITS = 8;
    public static final int LAST_USER_LENGTH = 8;
    public static final int LAST_TRANS_DIGITS = 8;
    public static final int FILLER_LENGTH = 50;

    private String portId = CobolText.spaces(ID_LENGTH);
    private String portAccountNo = CobolText.spaces(ACCOUNT_NO_LENGTH);
    private String portClientName = CobolText.spaces(CLIENT_NAME_LENGTH);
    private String portClientType = CobolText.spaces(ClientType.LENGTH);
    private int portCreateDate;
    private int portLastMaint;
    private String portStatus = CobolText.spaces(PortfolioStatus.LENGTH);
    private BigDecimal portTotalValue = CobolDecimal.ZERO_AMOUNT;
    private BigDecimal portCashBalance = CobolDecimal.ZERO_AMOUNT;
    private String portLastUser = CobolText.spaces(LAST_USER_LENGTH);
    private int portLastTrans;
    private String portFiller = CobolText.spaces(FILLER_LENGTH);

    private BigDecimal portTotalUnits = CobolDecimal.ZERO_QUANTITY;
    private BigDecimal portTotalCost = CobolDecimal.ZERO_AMOUNT;

    public PortfolioRecord() {
    }

    /** Copy constructor; a random {@code READ} overwrites the shared record area in place. */
    public PortfolioRecord(PortfolioRecord other) {
        this.portId = other.portId;
        this.portAccountNo = other.portAccountNo;
        this.portClientName = other.portClientName;
        this.portClientType = other.portClientType;
        this.portCreateDate = other.portCreateDate;
        this.portLastMaint = other.portLastMaint;
        this.portStatus = other.portStatus;
        this.portTotalValue = other.portTotalValue;
        this.portCashBalance = other.portCashBalance;
        this.portLastUser = other.portLastUser;
        this.portLastTrans = other.portLastTrans;
        this.portFiller = other.portFiller;
        this.portTotalUnits = other.portTotalUnits;
        this.portTotalCost = other.portTotalCost;
    }

    /** {@code PORT-KEY} - the record key {@code PORT-ID} followed by {@code PORT-ACCOUNT-NO}. */
    public String getPortKey() {
        return portId + portAccountNo;
    }

    public String getPortId() {
        return portId;
    }

    public void setPortId(String portId) {
        this.portId = CobolText.picX(portId, ID_LENGTH);
    }

    public String getPortAccountNo() {
        return portAccountNo;
    }

    public void setPortAccountNo(String portAccountNo) {
        this.portAccountNo = CobolText.picX(portAccountNo, ACCOUNT_NO_LENGTH);
    }

    public String getPortClientName() {
        return portClientName;
    }

    public void setPortClientName(String portClientName) {
        this.portClientName = CobolText.picX(portClientName, CLIENT_NAME_LENGTH);
    }

    /** The raw byte of {@code PORT-CLIENT-TYPE}, which need not be a recognised code. */
    public String getPortClientType() {
        return portClientType;
    }

    public void setPortClientType(String portClientType) {
        this.portClientType = CobolText.picX(portClientType, ClientType.LENGTH);
    }

    public void setPortClientType(ClientType clientType) {
        setPortClientType(clientType == null ? null : clientType.code());
    }

    /** The interpretation of {@code PORT-CLIENT-TYPE}, or {@code null} when no level-88 matches. */
    public ClientType getClientType() {
        return ClientType.fromCode(portClientType);
    }

    public int getPortCreateDate() {
        return portCreateDate;
    }

    public void setPortCreateDate(int portCreateDate) {
        this.portCreateDate = CobolText.pic9(portCreateDate, DATE_DIGITS);
    }

    public int getPortLastMaint() {
        return portLastMaint;
    }

    public void setPortLastMaint(int portLastMaint) {
        this.portLastMaint = CobolText.pic9(portLastMaint, DATE_DIGITS);
    }

    /** The raw byte of {@code PORT-STATUS}, which need not be a recognised code. */
    public String getPortStatus() {
        return portStatus;
    }

    public void setPortStatus(String portStatus) {
        this.portStatus = CobolText.picX(portStatus, PortfolioStatus.LENGTH);
    }

    public void setPortStatus(PortfolioStatus status) {
        setPortStatus(status == null ? null : status.code());
    }

    /** The interpretation of {@code PORT-STATUS}, or {@code null} when no level-88 matches. */
    public PortfolioStatus getPortfolioStatus() {
        return PortfolioStatus.fromCode(portStatus);
    }

    /** {@code 88 PORT-ACTIVE}. */
    public boolean isPortActive() {
        return getPortfolioStatus() == PortfolioStatus.ACTIVE;
    }

    /** {@code 88 PORT-CLOSED}. */
    public boolean isPortClosed() {
        return getPortfolioStatus() == PortfolioStatus.CLOSED;
    }

    /** {@code 88 PORT-SUSPENDED}. */
    public boolean isPortSuspended() {
        return getPortfolioStatus() == PortfolioStatus.SUSPENDED;
    }

    public BigDecimal getPortTotalValue() {
        return portTotalValue;
    }

    public void setPortTotalValue(BigDecimal portTotalValue) {
        this.portTotalValue = CobolDecimal.amount(portTotalValue);
    }

    public void setPortTotalValue(String portTotalValue) {
        this.portTotalValue = CobolDecimal.amount(portTotalValue);
    }

    public BigDecimal getPortCashBalance() {
        return portCashBalance;
    }

    public void setPortCashBalance(BigDecimal portCashBalance) {
        this.portCashBalance = CobolDecimal.amount(portCashBalance);
    }

    public void setPortCashBalance(String portCashBalance) {
        this.portCashBalance = CobolDecimal.amount(portCashBalance);
    }

    public String getPortLastUser() {
        return portLastUser;
    }

    public void setPortLastUser(String portLastUser) {
        this.portLastUser = CobolText.picX(portLastUser, LAST_USER_LENGTH);
    }

    public int getPortLastTrans() {
        return portLastTrans;
    }

    public void setPortLastTrans(int portLastTrans) {
        this.portLastTrans = CobolText.pic9(portLastTrans, LAST_TRANS_DIGITS);
    }

    public String getPortFiller() {
        return portFiller;
    }

    public void setPortFiller(String portFiller) {
        this.portFiller = CobolText.picX(portFiller, FILLER_LENGTH);
    }

    /**
     * Stand-in for {@code PORT-TOTAL-UNITS}, the holdings quantity {@code PORTTRAN} adds to on a buy
     * and subtracts from on a sell. Typed as {@code POS-QUANTITY}: {@code PIC S9(11)V9(4) COMP-3}.
     */
    public BigDecimal getPortTotalUnits() {
        return portTotalUnits;
    }

    public void setPortTotalUnits(BigDecimal portTotalUnits) {
        this.portTotalUnits = CobolDecimal.quantity(portTotalUnits);
    }

    public void setPortTotalUnits(String portTotalUnits) {
        this.portTotalUnits = CobolDecimal.quantity(portTotalUnits);
    }

    /**
     * Stand-in for {@code PORT-TOTAL-COST}, the cost basis {@code PORTTRAN} adds to on a buy and
     * subtracts from on a sell or fee. Typed as {@code POS-COST-BASIS}:
     * {@code PIC S9(13)V9(2) COMP-3}.
     */
    public BigDecimal getPortTotalCost() {
        return portTotalCost;
    }

    public void setPortTotalCost(BigDecimal portTotalCost) {
        this.portTotalCost = CobolDecimal.amount(portTotalCost);
    }

    public void setPortTotalCost(String portTotalCost) {
        this.portTotalCost = CobolDecimal.amount(portTotalCost);
    }

    /**
     * The {@code 01 PORT-RECORD} buffer as characters, for group moves such as
     * {@code MOVE PORT-RECORD TO AUD-BEFORE-IMAGE}. Only fields declared in {@code PORTFLIO.cpy}
     * appear, in declaration order; packed fields are rendered by {@link CobolDecimal#image}.
     */
    public String toRecordImage() {
        return portId
                + portAccountNo
                + portClientName
                + portClientType
                + CobolText.pic9Image(portCreateDate, DATE_DIGITS)
                + CobolText.pic9Image(portLastMaint, DATE_DIGITS)
                + portStatus
                + CobolDecimal.image(portTotalValue, CobolDecimal.AMOUNT_DIGITS, CobolDecimal.AMOUNT_SCALE)
                + CobolDecimal.image(portCashBalance, CobolDecimal.AMOUNT_DIGITS, CobolDecimal.AMOUNT_SCALE)
                + portLastUser
                + CobolText.pic9Image(portLastTrans, LAST_TRANS_DIGITS)
                + portFiller;
    }

    @Override
    public String toString() {
        return "PortfolioRecord[id=" + CobolText.trim(portId)
                + ", account=" + CobolText.trim(portAccountNo)
                + ", status=" + CobolText.trim(portStatus)
                + ", totalValue=" + portTotalValue
                + ", cashBalance=" + portCashBalance
                + ", totalUnits=" + portTotalUnits
                + ", totalCost=" + portTotalCost + "]";
    }
}
