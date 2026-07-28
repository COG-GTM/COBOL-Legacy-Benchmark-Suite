package com.clbs.portfolio.model;

import java.math.BigDecimal;

/**
 * Translation of {@code 01 POSITION-RECORD} in {@code src/copybook/common/POSREC.cpy}.
 *
 * <pre>
 * 05 POS-KEY
 *    10 POS-PORTFOLIO-ID    PIC X(08)
 *    10 POS-DATE            PIC X(08)
 *    10 POS-INVESTMENT-ID   PIC X(10)
 * 05 POS-DATA
 *    10 POS-QUANTITY        PIC S9(11)V9(4) COMP-3
 *    10 POS-COST-BASIS      PIC S9(13)V9(2) COMP-3
 *    10 POS-MARKET-VALUE    PIC S9(13)V9(2) COMP-3
 *    10 POS-CURRENCY        PIC X(03)
 *    10 POS-STATUS          PIC X(01)        -&gt; {@link PositionStatus}
 * 05 POS-AUDIT
 *    10 POS-LAST-MAINT-DATE PIC X(26)
 *    10 POS-LAST-MAINT-USER PIC X(08)
 * 05 POS-FILLER             PIC X(50)
 * </pre>
 */
public class PositionRecord {

    public static final int PORTFOLIO_ID_LENGTH = 8;
    public static final int DATE_LENGTH = 8;
    public static final int INVESTMENT_ID_LENGTH = 10;
    public static final int CURRENCY_LENGTH = 3;
    public static final int LAST_MAINT_DATE_LENGTH = 26;
    public static final int LAST_MAINT_USER_LENGTH = 8;
    public static final int FILLER_LENGTH = 50;

    private String posPortfolioId = CobolText.spaces(PORTFOLIO_ID_LENGTH);
    private String posDate = CobolText.spaces(DATE_LENGTH);
    private String posInvestmentId = CobolText.spaces(INVESTMENT_ID_LENGTH);
    private BigDecimal posQuantity = CobolDecimal.ZERO_QUANTITY;
    private BigDecimal posCostBasis = CobolDecimal.ZERO_AMOUNT;
    private BigDecimal posMarketValue = CobolDecimal.ZERO_AMOUNT;
    private String posCurrency = CobolText.spaces(CURRENCY_LENGTH);
    private String posStatus = CobolText.spaces(PositionStatus.LENGTH);
    private String posLastMaintDate = CobolText.spaces(LAST_MAINT_DATE_LENGTH);
    private String posLastMaintUser = CobolText.spaces(LAST_MAINT_USER_LENGTH);
    private String posFiller = CobolText.spaces(FILLER_LENGTH);

    public PositionRecord() {
    }

    /** Copy constructor; a VSAM record area is reused across reads. */
    public PositionRecord(PositionRecord other) {
        this.posPortfolioId = other.posPortfolioId;
        this.posDate = other.posDate;
        this.posInvestmentId = other.posInvestmentId;
        this.posQuantity = other.posQuantity;
        this.posCostBasis = other.posCostBasis;
        this.posMarketValue = other.posMarketValue;
        this.posCurrency = other.posCurrency;
        this.posStatus = other.posStatus;
        this.posLastMaintDate = other.posLastMaintDate;
        this.posLastMaintUser = other.posLastMaintUser;
        this.posFiller = other.posFiller;
    }

    /** {@code POS-KEY} - portfolio id, position date and investment id concatenated. */
    public String getPosKey() {
        return posPortfolioId + posDate + posInvestmentId;
    }

    public String getPosPortfolioId() {
        return posPortfolioId;
    }

    public void setPosPortfolioId(String posPortfolioId) {
        this.posPortfolioId = CobolText.picX(posPortfolioId, PORTFOLIO_ID_LENGTH);
    }

    public String getPosDate() {
        return posDate;
    }

    public void setPosDate(String posDate) {
        this.posDate = CobolText.picX(posDate, DATE_LENGTH);
    }

    public String getPosInvestmentId() {
        return posInvestmentId;
    }

    public void setPosInvestmentId(String posInvestmentId) {
        this.posInvestmentId = CobolText.picX(posInvestmentId, INVESTMENT_ID_LENGTH);
    }

    public BigDecimal getPosQuantity() {
        return posQuantity;
    }

    public void setPosQuantity(BigDecimal posQuantity) {
        this.posQuantity = CobolDecimal.quantity(posQuantity);
    }

    public void setPosQuantity(String posQuantity) {
        this.posQuantity = CobolDecimal.quantity(posQuantity);
    }

    public BigDecimal getPosCostBasis() {
        return posCostBasis;
    }

    public void setPosCostBasis(BigDecimal posCostBasis) {
        this.posCostBasis = CobolDecimal.amount(posCostBasis);
    }

    public void setPosCostBasis(String posCostBasis) {
        this.posCostBasis = CobolDecimal.amount(posCostBasis);
    }

    public BigDecimal getPosMarketValue() {
        return posMarketValue;
    }

    public void setPosMarketValue(BigDecimal posMarketValue) {
        this.posMarketValue = CobolDecimal.amount(posMarketValue);
    }

    public void setPosMarketValue(String posMarketValue) {
        this.posMarketValue = CobolDecimal.amount(posMarketValue);
    }

    public String getPosCurrency() {
        return posCurrency;
    }

    public void setPosCurrency(String posCurrency) {
        this.posCurrency = CobolText.picX(posCurrency, CURRENCY_LENGTH);
    }

    /** The raw byte of {@code POS-STATUS}, which need not be a recognised code. */
    public String getPosStatus() {
        return posStatus;
    }

    public void setPosStatus(String posStatus) {
        this.posStatus = CobolText.picX(posStatus, PositionStatus.LENGTH);
    }

    public void setPosStatus(PositionStatus status) {
        setPosStatus(status == null ? null : status.code());
    }

    /** The interpretation of {@code POS-STATUS}, or {@code null} when no level-88 matches. */
    public PositionStatus getPositionStatus() {
        return PositionStatus.fromCode(posStatus);
    }

    /** {@code 88 POS-STATUS-ACTIVE}. */
    public boolean isPosStatusActive() {
        return getPositionStatus() == PositionStatus.ACTIVE;
    }

    /** {@code 88 POS-STATUS-CLOSED}. */
    public boolean isPosStatusClosed() {
        return getPositionStatus() == PositionStatus.CLOSED;
    }

    /** {@code 88 POS-STATUS-PEND}. */
    public boolean isPosStatusPend() {
        return getPositionStatus() == PositionStatus.PENDING;
    }

    public String getPosLastMaintDate() {
        return posLastMaintDate;
    }

    public void setPosLastMaintDate(String posLastMaintDate) {
        this.posLastMaintDate = CobolText.picX(posLastMaintDate, LAST_MAINT_DATE_LENGTH);
    }

    public String getPosLastMaintUser() {
        return posLastMaintUser;
    }

    public void setPosLastMaintUser(String posLastMaintUser) {
        this.posLastMaintUser = CobolText.picX(posLastMaintUser, LAST_MAINT_USER_LENGTH);
    }

    public String getPosFiller() {
        return posFiller;
    }

    public void setPosFiller(String posFiller) {
        this.posFiller = CobolText.picX(posFiller, FILLER_LENGTH);
    }

    @Override
    public String toString() {
        return "PositionRecord[key=" + CobolText.trim(getPosKey())
                + ", quantity=" + posQuantity
                + ", costBasis=" + posCostBasis
                + ", marketValue=" + posMarketValue
                + ", status=" + CobolText.trim(posStatus) + "]";
    }
}
