package com.portfolio.modernization.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * History Record Entity
 * 
 * Modernized from COBOL copybook: src/copybook/common/HISTREC.cpy
 * Maps to database table: HISTORY_LOG
 * 
 * Original COBOL structure:
 * <pre>
 * 01  HISTORY-RECORD.
 *     05  HIST-KEY.
 *         10  HIST-PORTFOLIO-ID  PIC X(08).
 *         10  HIST-DATE          PIC X(08).
 *         10  HIST-TIME          PIC X(06).
 *         10  HIST-SEQ-NO        PIC X(04).
 *     05  HIST-DATA.
 *         10  HIST-RECORD-TYPE   PIC X(02).
 *         10  HIST-ACTION-CODE   PIC X(01).
 *         10  HIST-BEFORE-IMAGE  PIC X(400).
 *         10  HIST-AFTER-IMAGE   PIC X(400).
 *         10  HIST-REASON-CODE   PIC X(04).
 *     05  HIST-AUDIT.
 *         10  HIST-PROCESS-DATE  PIC X(26).
 *         10  HIST-PROCESS-USER  PIC X(08).
 * </pre>
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Entity
@Table(name = "HISTORY_LOG", indexes = {
    @Index(name = "IDX_HIST_PORTFOLIO_DATE", columnList = "portfolioId, historyDate"),
    @Index(name = "IDX_HIST_RECORD_TYPE", columnList = "recordType, historyDate"),
    @Index(name = "IDX_HIST_ACTION", columnList = "actionCode, historyDate"),
    @Index(name = "IDX_HIST_PROCESS_DATE", columnList = "processDate")
})
public class HistoryRecord {

    /**
     * Record type constants (from HIST-RECORD-TYPE 88-level conditions)
     */
    public static final String RECORD_TYPE_PORTFOLIO = "PT";
    public static final String RECORD_TYPE_POSITION = "PS";
    public static final String RECORD_TYPE_TRANSACTION = "TR";

    /**
     * Action code constants (from HIST-ACTION-CODE 88-level conditions)
     */
    public static final String ACTION_ADD = "A";
    public static final String ACTION_CHANGE = "C";
    public static final String ACTION_DELETE = "D";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HISTORY_ID")
    private Long historyId;

    @NotNull(message = "Portfolio ID is required")
    @Size(max = 8, message = "Portfolio ID cannot exceed 8 characters")
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @NotNull(message = "History date is required")
    @Column(name = "HISTORY_DATE", nullable = false)
    private LocalDate historyDate;

    @Column(name = "HISTORY_TIME")
    private LocalTime historyTime;

    @Size(max = 4, message = "Sequence number cannot exceed 4 characters")
    @Column(name = "SEQUENCE_NO", length = 4)
    private String sequenceNo;

    @NotNull(message = "Record type is required")
    @Size(max = 2, message = "Record type must be 2 characters")
    @Column(name = "RECORD_TYPE", length = 2, nullable = false)
    private String recordType;

    @NotNull(message = "Action code is required")
    @Size(max = 1, message = "Action code must be 1 character")
    @Column(name = "ACTION_CODE", length = 1, nullable = false)
    private String actionCode;

    @Column(name = "BEFORE_IMAGE", length = 4000)
    private String beforeImage;

    @Column(name = "AFTER_IMAGE", length = 4000)
    private String afterImage;

    @Size(max = 4, message = "Reason code cannot exceed 4 characters")
    @Column(name = "REASON_CODE", length = 4)
    private String reasonCode;

    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDateTime processDate;

    @Size(max = 8, message = "Process user cannot exceed 8 characters")
    @Column(name = "PROCESS_USER", length = 8, nullable = false)
    private String processUser;

    @Column(name = "VSAM_MIGRATION_DATE")
    private LocalDateTime vsamMigrationDate;

    @Size(max = 26, message = "VSAM record key cannot exceed 26 characters")
    @Column(name = "VSAM_RECORD_KEY", length = 26)
    private String vsamRecordKey;

    @Column(name = "ENTITY_ID", length = 50)
    private String entityId;

    @Column(name = "ENTITY_TYPE", length = 30)
    private String entityType;

    @Column(name = "CHANGE_SUMMARY", length = 500)
    private String changeSummary;

    @Column(name = "REQUEST_ID", length = 36)
    private String requestId;

    @Column(name = "CORRELATION_ID", length = 36)
    private String correlationId;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public HistoryRecord() {
        this.processDate = LocalDateTime.now();
        this.historyDate = LocalDate.now();
        this.historyTime = LocalTime.now();
    }

    public HistoryRecord(String portfolioId, String recordType, String actionCode) {
        this();
        this.portfolioId = portfolioId;
        this.recordType = recordType;
        this.actionCode = actionCode;
    }

    @PrePersist
    public void prePersist() {
        if (processDate == null) {
            processDate = LocalDateTime.now();
        }
        if (historyDate == null) {
            historyDate = LocalDate.now();
        }
        if (historyTime == null) {
            historyTime = LocalTime.now();
        }
    }

    /**
     * Validates history record based on business rules
     * @return true if record is valid
     */
    public boolean isValidRecord() {
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            return false;
        }
        if (historyDate == null) {
            return false;
        }
        if (!isValidRecordType()) {
            return false;
        }
        if (!isValidActionCode()) {
            return false;
        }
        return true;
    }

    /**
     * Checks if record type is valid
     */
    public boolean isValidRecordType() {
        if (recordType == null) return false;
        return recordType.equals(RECORD_TYPE_PORTFOLIO) ||
               recordType.equals(RECORD_TYPE_POSITION) ||
               recordType.equals(RECORD_TYPE_TRANSACTION);
    }

    /**
     * Checks if action code is valid
     */
    public boolean isValidActionCode() {
        if (actionCode == null) return false;
        return actionCode.equals(ACTION_ADD) ||
               actionCode.equals(ACTION_CHANGE) ||
               actionCode.equals(ACTION_DELETE);
    }

    /**
     * Checks if this is a portfolio history record
     */
    public boolean isPortfolioRecord() {
        return RECORD_TYPE_PORTFOLIO.equals(recordType);
    }

    /**
     * Checks if this is a position history record
     */
    public boolean isPositionRecord() {
        return RECORD_TYPE_POSITION.equals(recordType);
    }

    /**
     * Checks if this is a transaction history record
     */
    public boolean isTransactionRecord() {
        return RECORD_TYPE_TRANSACTION.equals(recordType);
    }

    /**
     * Checks if this is an add action
     */
    public boolean isAddAction() {
        return ACTION_ADD.equals(actionCode);
    }

    /**
     * Checks if this is a change action
     */
    public boolean isChangeAction() {
        return ACTION_CHANGE.equals(actionCode);
    }

    /**
     * Checks if this is a delete action
     */
    public boolean isDeleteAction() {
        return ACTION_DELETE.equals(actionCode);
    }

    /**
     * Gets human-readable record type description
     */
    public String getRecordTypeDescription() {
        if (recordType == null) return "Unknown";
        return switch (recordType) {
            case RECORD_TYPE_PORTFOLIO -> "Portfolio";
            case RECORD_TYPE_POSITION -> "Position";
            case RECORD_TYPE_TRANSACTION -> "Transaction";
            default -> recordType;
        };
    }

    /**
     * Gets human-readable action description
     */
    public String getActionDescription() {
        if (actionCode == null) return "Unknown";
        return switch (actionCode) {
            case ACTION_ADD -> "Added";
            case ACTION_CHANGE -> "Changed";
            case ACTION_DELETE -> "Deleted";
            default -> actionCode;
        };
    }

    /**
     * Creates VSAM record key for migration tracking
     * Format: HIST-PORTFOLIO-ID + HIST-DATE + HIST-TIME + HIST-SEQ-NO
     */
    public String createVsamRecordKey() {
        StringBuilder sb = new StringBuilder();
        if (portfolioId != null) {
            sb.append(String.format("%-8s", portfolioId));
        }
        if (historyDate != null) {
            sb.append(historyDate.format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        if (historyTime != null) {
            sb.append(historyTime.format(DateTimeFormatter.ofPattern("HHmmss")));
        }
        if (sequenceNo != null) {
            sb.append(String.format("%-4s", sequenceNo));
        }
        return sb.toString();
    }

    /**
     * Creates a history record for an entity add operation
     */
    public static HistoryRecord createAddRecord(String portfolioId, String recordType,
                                                String afterImage, String processUser) {
        HistoryRecord record = new HistoryRecord(portfolioId, recordType, ACTION_ADD);
        record.setAfterImage(afterImage);
        record.setProcessUser(processUser);
        return record;
    }

    /**
     * Creates a history record for an entity change operation
     */
    public static HistoryRecord createChangeRecord(String portfolioId, String recordType,
                                                   String beforeImage, String afterImage,
                                                   String reasonCode, String processUser) {
        HistoryRecord record = new HistoryRecord(portfolioId, recordType, ACTION_CHANGE);
        record.setBeforeImage(beforeImage);
        record.setAfterImage(afterImage);
        record.setReasonCode(reasonCode);
        record.setProcessUser(processUser);
        return record;
    }

    /**
     * Creates a history record for an entity delete operation
     */
    public static HistoryRecord createDeleteRecord(String portfolioId, String recordType,
                                                   String beforeImage, String reasonCode,
                                                   String processUser) {
        HistoryRecord record = new HistoryRecord(portfolioId, recordType, ACTION_DELETE);
        record.setBeforeImage(beforeImage);
        record.setReasonCode(reasonCode);
        record.setProcessUser(processUser);
        return record;
    }

    /**
     * Generates a change summary from before and after images
     */
    public void generateChangeSummary() {
        if (beforeImage == null && afterImage == null) {
            this.changeSummary = "No changes recorded";
            return;
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(getActionDescription())
               .append(" ")
               .append(getRecordTypeDescription())
               .append(" record for portfolio ")
               .append(portfolioId);
        
        if (reasonCode != null && !reasonCode.trim().isEmpty()) {
            summary.append(" (Reason: ").append(reasonCode).append(")");
        }
        
        this.changeSummary = summary.toString();
    }

    // Getters and Setters

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getHistoryDate() {
        return historyDate;
    }

    public void setHistoryDate(LocalDate historyDate) {
        this.historyDate = historyDate;
    }

    public LocalTime getHistoryTime() {
        return historyTime;
    }

    public void setHistoryTime(LocalTime historyTime) {
        this.historyTime = historyTime;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
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

    public LocalDateTime getVsamMigrationDate() {
        return vsamMigrationDate;
    }

    public void setVsamMigrationDate(LocalDateTime vsamMigrationDate) {
        this.vsamMigrationDate = vsamMigrationDate;
    }

    public String getVsamRecordKey() {
        return vsamRecordKey;
    }

    public void setVsamRecordKey(String vsamRecordKey) {
        this.vsamRecordKey = vsamRecordKey;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryRecord that = (HistoryRecord) o;
        return Objects.equals(historyId, that.historyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(historyId);
    }

    @Override
    public String toString() {
        return "HistoryRecord{" +
                "historyId=" + historyId +
                ", portfolioId='" + portfolioId + '\'' +
                ", historyDate=" + historyDate +
                ", recordType='" + recordType + '\'' +
                ", actionCode='" + actionCode + '\'' +
                ", reasonCode='" + reasonCode + '\'' +
                '}';
    }
}
