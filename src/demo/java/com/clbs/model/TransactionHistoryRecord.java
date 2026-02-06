package com.clbs.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Java equivalent of COBOL HISTORY-RECORD from HISTREC.cpy
 * This represents the input VSAM file record that HISTLD00 reads.
 * 
 * COBOL Original:
 * <pre>
 *  01  HISTORY-RECORD.
 *      05  HIST-KEY.
 *          10  HIST-PORTFOLIO-ID  PIC X(08).
 *          10  HIST-DATE         PIC X(08).
 *          10  HIST-TIME         PIC X(06).
 *          10  HIST-SEQ-NO       PIC X(04).
 *      05  HIST-DATA.
 *          10  HIST-RECORD-TYPE  PIC X(02).
 *              88  HIST-TYPE-PORT    VALUE 'PT'.
 *              88  HIST-TYPE-POS     VALUE 'PS'.
 *              88  HIST-TYPE-TRN     VALUE 'TR'.
 *          10  HIST-ACTION-CODE  PIC X(01).
 *              88  HIST-ACTION-ADD   VALUE 'A'.
 *              88  HIST-ACTION-CHG   VALUE 'C'.
 *              88  HIST-ACTION-DEL   VALUE 'D'.
 *          10  HIST-BEFORE-IMAGE PIC X(400).
 *          10  HIST-AFTER-IMAGE  PIC X(400).
 *          10  HIST-REASON-CODE  PIC X(04).
 *      05  HIST-AUDIT.
 *          10  HIST-PROCESS-DATE PIC X(26).
 *          10  HIST-PROCESS-USER PIC X(08).
 *      05  HIST-FILLER          PIC X(50).
 * </pre>
 * 
 * Migration Notes:
 * - 88-level condition names converted to enum types
 * - Composite key (HIST-KEY) preserved as separate fields
 * - FILLER field omitted (not needed in Java)
 */
public class TransactionHistoryRecord {

    public enum RecordType {
        PORTFOLIO("PT"),
        POSITION("PS"),
        TRANSACTION("TR");

        private final String code;

        RecordType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static RecordType fromCode(String code) {
            for (RecordType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown record type: " + code);
        }
    }

    public enum ActionCode {
        ADD('A'),
        CHANGE('C'),
        DELETE('D');

        private final char code;

        ActionCode(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static ActionCode fromCode(char code) {
            for (ActionCode action : values()) {
                if (action.code == code) {
                    return action;
                }
            }
            throw new IllegalArgumentException("Unknown action code: " + code);
        }
    }

    private String portfolioId;
    private String histDate;
    private String histTime;
    private String seqNo;
    private RecordType recordType;
    private ActionCode actionCode;
    private String beforeImage;
    private String afterImage;
    private String reasonCode;
    private LocalDateTime processDate;
    private String processUser;
    
    private String accountNo;
    private String transType;
    private String securityId;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal fees;
    private BigDecimal totalAmount;
    private BigDecimal costBasis;
    private BigDecimal gainLoss;

    public TransactionHistoryRecord() {
        this.quantity = BigDecimal.ZERO;
        this.price = BigDecimal.ZERO;
        this.amount = BigDecimal.ZERO;
        this.fees = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.costBasis = BigDecimal.ZERO;
        this.gainLoss = BigDecimal.ZERO;
    }

    public String getCompositeKey() {
        return portfolioId + histDate + histTime + seqNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getHistDate() {
        return histDate;
    }

    public void setHistDate(String histDate) {
        this.histDate = histDate;
    }

    public String getHistTime() {
        return histTime;
    }

    public void setHistTime(String histTime) {
        this.histTime = histTime;
    }

    public String getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(String seqNo) {
        this.seqNo = seqNo;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }

    public ActionCode getActionCode() {
        return actionCode;
    }

    public void setActionCode(ActionCode actionCode) {
        this.actionCode = actionCode;
    }

    public String getBeforeImage() {
        return beforeImage;
    }

    public void setBeforeImage(String beforeImage) {
        this.beforeImage = beforeImage;
    }

    public String getAfterImage() {
        return afterImage;
    }

    public void setAfterImage(String afterImage) {
        this.afterImage = afterImage;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
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

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
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
}
