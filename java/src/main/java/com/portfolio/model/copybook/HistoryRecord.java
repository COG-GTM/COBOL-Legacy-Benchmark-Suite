package com.portfolio.model.copybook;

/**
 * Migrated from copybook {@code src/copybook/common/HISTREC.cpy} (01 HISTORY-RECORD).
 *
 * <p>VSAM history/audit-trail record. Key = HIST-KEY (portfolio id + date + time + seq no).
 */
public class HistoryRecord {

    /** HIST-PORTFOLIO-ID PIC X(08). */
    private String portfolioId;

    /** HIST-DATE PIC X(08) — YYYYMMDD. */
    private String date;

    /** HIST-TIME PIC X(06) — HHMMSS. */
    private String time;

    /** HIST-SEQ-NO PIC X(04). */
    private String seqNo;

    /** HIST-RECORD-TYPE PIC X(02) — PT=Portfolio, PS=Position, TR=Transaction (level-88s). */
    private String recordType;

    /** HIST-ACTION-CODE PIC X(01) — A=Add, C=Change, D=Delete (level-88s). */
    private String actionCode;

    /** HIST-BEFORE-IMAGE PIC X(400). */
    private String beforeImage;

    /** HIST-AFTER-IMAGE PIC X(400). */
    private String afterImage;

    /** HIST-REASON-CODE PIC X(04). */
    private String reasonCode;

    /** HIST-PROCESS-DATE PIC X(26). */
    private String processDate;

    /** HIST-PROCESS-USER PIC X(08). */
    private String processUser;

    public static final String TYPE_PORTFOLIO = "PT";
    public static final String TYPE_POSITION = "PS";
    public static final String TYPE_TRANSACTION = "TR";

    public static final String ACTION_ADD = "A";
    public static final String ACTION_CHANGE = "C";
    public static final String ACTION_DELETE = "D";

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getSeqNo() { return seqNo; }
    public void setSeqNo(String seqNo) { this.seqNo = seqNo; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getActionCode() { return actionCode; }
    public void setActionCode(String actionCode) { this.actionCode = actionCode; }
    public String getBeforeImage() { return beforeImage; }
    public void setBeforeImage(String beforeImage) { this.beforeImage = beforeImage; }
    public String getAfterImage() { return afterImage; }
    public void setAfterImage(String afterImage) { this.afterImage = afterImage; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getProcessDate() { return processDate; }
    public void setProcessDate(String processDate) { this.processDate = processDate; }
    public String getProcessUser() { return processUser; }
    public void setProcessUser(String processUser) { this.processUser = processUser; }
}
