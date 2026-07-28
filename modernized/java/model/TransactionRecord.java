package com.clbs.portfolio.model;

import java.math.BigDecimal;

/**
 * Translation of {@code 01 TRANSACTION-RECORD} in {@code src/copybook/common/TRNREC.cpy}, the
 * record area of the sequential {@code TRANSACTION-FILE} read by {@code PORTTRAN}.
 *
 * <pre>
 * 05 TRN-KEY
 *    10 TRN-DATE           PIC X(08)
 *    10 TRN-TIME           PIC X(06)
 *    10 TRN-PORTFOLIO-ID   PIC X(08)
 *    10 TRN-SEQUENCE-NO    PIC X(06)
 * 05 TRN-DATA
 *    10 TRN-INVESTMENT-ID  PIC X(10)
 *    10 TRN-TYPE           PIC X(02)          -&gt; {@link TransactionType}
 *    10 TRN-QUANTITY       PIC S9(11)V9(4) COMP-3
 *    10 TRN-PRICE          PIC S9(11)V9(4) COMP-3
 *    10 TRN-AMOUNT         PIC S9(13)V9(2) COMP-3
 *    10 TRN-CURRENCY       PIC X(03)
 *    10 TRN-STATUS         PIC X(01)          -&gt; {@link TransactionStatus}
 * 05 TRN-AUDIT
 *    10 TRN-PROCESS-DATE   PIC X(26)
 *    10 TRN-PROCESS-USER   PIC X(08)
 * 05 TRN-FILLER            PIC X(50)
 * </pre>
 *
 * <p>The record is a mutable buffer, like the COBOL record area it stands for. Alphanumeric fields
 * are stored space-padded to their declared length and the two coded fields keep their raw bytes so
 * that validation can reject - and echo - values no level-88 covers.
 */
public class TransactionRecord {

    public static final int DATE_LENGTH = 8;
    public static final int TIME_LENGTH = 6;
    public static final int PORTFOLIO_ID_LENGTH = 8;
    public static final int SEQUENCE_NO_LENGTH = 6;
    public static final int INVESTMENT_ID_LENGTH = 10;
    public static final int CURRENCY_LENGTH = 3;
    public static final int PROCESS_DATE_LENGTH = 26;
    public static final int PROCESS_USER_LENGTH = 8;
    public static final int FILLER_LENGTH = 50;

    private String trnDate = CobolText.spaces(DATE_LENGTH);
    private String trnTime = CobolText.spaces(TIME_LENGTH);
    private String trnPortfolioId = CobolText.spaces(PORTFOLIO_ID_LENGTH);
    private String trnSequenceNo = CobolText.spaces(SEQUENCE_NO_LENGTH);
    private String trnInvestmentId = CobolText.spaces(INVESTMENT_ID_LENGTH);
    private String trnType = CobolText.spaces(TransactionType.LENGTH);
    private BigDecimal trnQuantity = CobolDecimal.ZERO_QUANTITY;
    private BigDecimal trnPrice = CobolDecimal.ZERO_QUANTITY;
    private BigDecimal trnAmount = CobolDecimal.ZERO_AMOUNT;
    private String trnCurrency = CobolText.spaces(CURRENCY_LENGTH);
    private String trnStatus = CobolText.spaces(TransactionStatus.LENGTH);
    private String trnProcessDate = CobolText.spaces(PROCESS_DATE_LENGTH);
    private String trnProcessUser = CobolText.spaces(PROCESS_USER_LENGTH);
    private String trnFiller = CobolText.spaces(FILLER_LENGTH);

    public TransactionRecord() {
    }

    /** Copy constructor; the sequential read overwrites one shared record area per iteration. */
    public TransactionRecord(TransactionRecord other) {
        this.trnDate = other.trnDate;
        this.trnTime = other.trnTime;
        this.trnPortfolioId = other.trnPortfolioId;
        this.trnSequenceNo = other.trnSequenceNo;
        this.trnInvestmentId = other.trnInvestmentId;
        this.trnType = other.trnType;
        this.trnQuantity = other.trnQuantity;
        this.trnPrice = other.trnPrice;
        this.trnAmount = other.trnAmount;
        this.trnCurrency = other.trnCurrency;
        this.trnStatus = other.trnStatus;
        this.trnProcessDate = other.trnProcessDate;
        this.trnProcessUser = other.trnProcessUser;
        this.trnFiller = other.trnFiller;
    }

    /** {@code TRN-KEY} - the concatenated date, time, portfolio id and sequence number. */
    public String getTrnKey() {
        return trnDate + trnTime + trnPortfolioId + trnSequenceNo;
    }

    public String getTrnDate() {
        return trnDate;
    }

    public void setTrnDate(String trnDate) {
        this.trnDate = CobolText.picX(trnDate, DATE_LENGTH);
    }

    public String getTrnTime() {
        return trnTime;
    }

    public void setTrnTime(String trnTime) {
        this.trnTime = CobolText.picX(trnTime, TIME_LENGTH);
    }

    public String getTrnPortfolioId() {
        return trnPortfolioId;
    }

    public void setTrnPortfolioId(String trnPortfolioId) {
        this.trnPortfolioId = CobolText.picX(trnPortfolioId, PORTFOLIO_ID_LENGTH);
    }

    public String getTrnSequenceNo() {
        return trnSequenceNo;
    }

    public void setTrnSequenceNo(String trnSequenceNo) {
        this.trnSequenceNo = CobolText.picX(trnSequenceNo, SEQUENCE_NO_LENGTH);
    }

    public String getTrnInvestmentId() {
        return trnInvestmentId;
    }

    public void setTrnInvestmentId(String trnInvestmentId) {
        this.trnInvestmentId = CobolText.picX(trnInvestmentId, INVESTMENT_ID_LENGTH);
    }

    /** The raw two bytes of {@code TRN-TYPE}, which need not be a recognised code. */
    public String getTrnType() {
        return trnType;
    }

    public void setTrnType(String trnType) {
        this.trnType = CobolText.picX(trnType, TransactionType.LENGTH);
    }

    public void setTrnType(TransactionType type) {
        setTrnType(type == null ? null : type.code());
    }

    /** The interpretation of {@code TRN-TYPE}, or {@code null} when no level-88 matches. */
    public TransactionType getTransactionType() {
        return TransactionType.fromCode(trnType);
    }

    /** {@code 88 TRN-TYPE-BUY}. */
    public boolean isTrnTypeBuy() {
        return getTransactionType() == TransactionType.BUY;
    }

    /** {@code 88 TRN-TYPE-SELL}. */
    public boolean isTrnTypeSell() {
        return getTransactionType() == TransactionType.SELL;
    }

    /** {@code 88 TRN-TYPE-TRANS}. */
    public boolean isTrnTypeTrans() {
        return getTransactionType() == TransactionType.TRANSFER;
    }

    /** {@code 88 TRN-TYPE-FEE}. */
    public boolean isTrnTypeFee() {
        return getTransactionType() == TransactionType.FEE;
    }

    public BigDecimal getTrnQuantity() {
        return trnQuantity;
    }

    public void setTrnQuantity(BigDecimal trnQuantity) {
        this.trnQuantity = CobolDecimal.quantity(trnQuantity);
    }

    public void setTrnQuantity(String trnQuantity) {
        this.trnQuantity = CobolDecimal.quantity(trnQuantity);
    }

    public BigDecimal getTrnPrice() {
        return trnPrice;
    }

    public void setTrnPrice(BigDecimal trnPrice) {
        this.trnPrice = CobolDecimal.quantity(trnPrice);
    }

    public void setTrnPrice(String trnPrice) {
        this.trnPrice = CobolDecimal.quantity(trnPrice);
    }

    public BigDecimal getTrnAmount() {
        return trnAmount;
    }

    public void setTrnAmount(BigDecimal trnAmount) {
        this.trnAmount = CobolDecimal.amount(trnAmount);
    }

    public void setTrnAmount(String trnAmount) {
        this.trnAmount = CobolDecimal.amount(trnAmount);
    }

    public String getTrnCurrency() {
        return trnCurrency;
    }

    public void setTrnCurrency(String trnCurrency) {
        this.trnCurrency = CobolText.picX(trnCurrency, CURRENCY_LENGTH);
    }

    /** The raw byte of {@code TRN-STATUS}, which need not be a recognised code. */
    public String getTrnStatus() {
        return trnStatus;
    }

    public void setTrnStatus(String trnStatus) {
        this.trnStatus = CobolText.picX(trnStatus, TransactionStatus.LENGTH);
    }

    public void setTrnStatus(TransactionStatus status) {
        setTrnStatus(status == null ? null : status.code());
    }

    /** The interpretation of {@code TRN-STATUS}, or {@code null} when no level-88 matches. */
    public TransactionStatus getTransactionStatus() {
        return TransactionStatus.fromCode(trnStatus);
    }

    public String getTrnProcessDate() {
        return trnProcessDate;
    }

    public void setTrnProcessDate(String trnProcessDate) {
        this.trnProcessDate = CobolText.picX(trnProcessDate, PROCESS_DATE_LENGTH);
    }

    public String getTrnProcessUser() {
        return trnProcessUser;
    }

    public void setTrnProcessUser(String trnProcessUser) {
        this.trnProcessUser = CobolText.picX(trnProcessUser, PROCESS_USER_LENGTH);
    }

    public String getTrnFiller() {
        return trnFiller;
    }

    public void setTrnFiller(String trnFiller) {
        this.trnFiller = CobolText.picX(trnFiller, FILLER_LENGTH);
    }

    @Override
    public String toString() {
        return "TransactionRecord[key=" + CobolText.trim(getTrnKey())
                + ", investment=" + CobolText.trim(trnInvestmentId)
                + ", type=" + CobolText.trim(trnType)
                + ", quantity=" + trnQuantity
                + ", price=" + trnPrice
                + ", amount=" + trnAmount
                + ", status=" + CobolText.trim(trnStatus) + "]";
    }
}
