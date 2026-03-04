package com.investment.portfolio.model;

import java.time.LocalDateTime;

/**
 * History Record - Java equivalent of HISTREC.cpy
 * Maps the COBOL HISTORY-RECORD copybook structure.
 */
public class HistoryRecord {

    /** Key fields */
    private String portfolioId;        // HIST-PORTFOLIO-ID: PIC X(08)
    private String historyDate;        // HIST-DATE: PIC X(08) YYYYMMDD
    private String historyTime;        // HIST-TIME: PIC X(06) HHMMSS
    private String sequenceNumber;     // HIST-SEQ-NO: PIC X(04)

    /** History data */
    private RecordType recordType;     // HIST-RECORD-TYPE: PIC X(02)
    private ActionCode actionCode;     // HIST-ACTION-CODE: PIC X(01)
    private String beforeImage;        // HIST-BEFORE-IMAGE: PIC X(400)
    private String afterImage;         // HIST-AFTER-IMAGE: PIC X(400)
    private String reasonCode;         // HIST-REASON-CODE: PIC X(04)

    /** Audit fields */
    private LocalDateTime processDate; // HIST-PROCESS-DATE: PIC X(26)
    private String processUser;        // HIST-PROCESS-USER: PIC X(08)

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
            for (RecordType r : values()) {
                if (r.code.equals(code)) return r;
            }
            throw new IllegalArgumentException("Invalid record type: " + code);
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
            for (ActionCode a : values()) {
                if (a.code == code) return a;
            }
            throw new IllegalArgumentException("Invalid action code: " + code);
        }
    }

    // --- Getters and Setters ---

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getHistoryDate() { return historyDate; }
    public void setHistoryDate(String historyDate) { this.historyDate = historyDate; }

    public String getHistoryTime() { return historyTime; }
    public void setHistoryTime(String historyTime) { this.historyTime = historyTime; }

    public String getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(String sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public RecordType getRecordType() { return recordType; }
    public void setRecordType(RecordType recordType) { this.recordType = recordType; }

    public ActionCode getActionCode() { return actionCode; }
    public void setActionCode(ActionCode actionCode) { this.actionCode = actionCode; }

    public String getBeforeImage() { return beforeImage; }
    public void setBeforeImage(String beforeImage) { this.beforeImage = beforeImage; }

    public String getAfterImage() { return afterImage; }
    public void setAfterImage(String afterImage) { this.afterImage = afterImage; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public LocalDateTime getProcessDate() { return processDate; }
    public void setProcessDate(LocalDateTime processDate) { this.processDate = processDate; }

    public String getProcessUser() { return processUser; }
    public void setProcessUser(String processUser) { this.processUser = processUser; }

    public String getCompositeKey() {
        return portfolioId + historyDate + historyTime + sequenceNumber;
    }

    @Override
    public String toString() {
        return "HistoryRecord{" +
                "portfolioId='" + portfolioId + '\'' +
                ", recordType=" + recordType +
                ", actionCode=" + actionCode +
                ", reasonCode='" + reasonCode + '\'' +
                '}';
    }
}
