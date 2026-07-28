package com.clbs.portfolio.model;

/**
 * Translation of {@code 01 AUDIT-RECORD} in {@code src/copybook/common/AUDITLOG.cpy}, the record
 * {@code PORTTRAN} builds in {@code 2300-UPDATE-AUDIT-TRAIL} and passes to {@code AUDPROC}.
 *
 * <pre>
 * 05 AUD-HEADER
 *    10 AUD-TIMESTAMP    PIC X(26)
 *    10 AUD-SYSTEM-ID    PIC X(8)
 *    10 AUD-USER-ID      PIC X(8)
 *    10 AUD-PROGRAM      PIC X(8)
 *    10 AUD-TERMINAL     PIC X(8)
 * 05 AUD-TYPE            PIC X(4)      -&gt; {@link AuditType}
 * 05 AUD-ACTION          PIC X(8)      -&gt; {@link AuditAction}
 * 05 AUD-STATUS          PIC X(4)      -&gt; {@link AuditStatus}
 * 05 AUD-KEY-INFO
 *    10 AUD-PORTFOLIO-ID PIC X(8)
 *    10 AUD-ACCOUNT-NO   PIC X(10)
 * 05 AUD-BEFORE-IMAGE    PIC X(100)
 * 05 AUD-AFTER-IMAGE     PIC X(100)
 * 05 AUD-MESSAGE         PIC X(100)
 * </pre>
 */
public class AuditRecord {

    public static final int TIMESTAMP_LENGTH = 26;
    public static final int SYSTEM_ID_LENGTH = 8;
    public static final int USER_ID_LENGTH = 8;
    public static final int PROGRAM_LENGTH = 8;
    public static final int TERMINAL_LENGTH = 8;
    public static final int PORTFOLIO_ID_LENGTH = 8;
    public static final int ACCOUNT_NO_LENGTH = 10;
    public static final int IMAGE_LENGTH = 100;
    public static final int MESSAGE_LENGTH = 100;

    private String audTimestamp = CobolText.spaces(TIMESTAMP_LENGTH);
    private String audSystemId = CobolText.spaces(SYSTEM_ID_LENGTH);
    private String audUserId = CobolText.spaces(USER_ID_LENGTH);
    private String audProgram = CobolText.spaces(PROGRAM_LENGTH);
    private String audTerminal = CobolText.spaces(TERMINAL_LENGTH);
    private String audType = CobolText.spaces(AuditType.LENGTH);
    private String audAction = CobolText.spaces(AuditAction.LENGTH);
    private String audStatus = CobolText.spaces(AuditStatus.LENGTH);
    private String audPortfolioId = CobolText.spaces(PORTFOLIO_ID_LENGTH);
    private String audAccountNo = CobolText.spaces(ACCOUNT_NO_LENGTH);
    private String audBeforeImage = CobolText.spaces(IMAGE_LENGTH);
    private String audAfterImage = CobolText.spaces(IMAGE_LENGTH);
    private String audMessage = CobolText.spaces(MESSAGE_LENGTH);

    public AuditRecord() {
    }

    /** Copy constructor; {@code PORTTRAN} reuses one audit area for every transaction. */
    public AuditRecord(AuditRecord other) {
        this.audTimestamp = other.audTimestamp;
        this.audSystemId = other.audSystemId;
        this.audUserId = other.audUserId;
        this.audProgram = other.audProgram;
        this.audTerminal = other.audTerminal;
        this.audType = other.audType;
        this.audAction = other.audAction;
        this.audStatus = other.audStatus;
        this.audPortfolioId = other.audPortfolioId;
        this.audAccountNo = other.audAccountNo;
        this.audBeforeImage = other.audBeforeImage;
        this.audAfterImage = other.audAfterImage;
        this.audMessage = other.audMessage;
    }

    /** {@code INITIALIZE AUDIT-RECORD} - every alphanumeric field back to spaces. */
    public void initialize() {
        this.audTimestamp = CobolText.spaces(TIMESTAMP_LENGTH);
        this.audSystemId = CobolText.spaces(SYSTEM_ID_LENGTH);
        this.audUserId = CobolText.spaces(USER_ID_LENGTH);
        this.audProgram = CobolText.spaces(PROGRAM_LENGTH);
        this.audTerminal = CobolText.spaces(TERMINAL_LENGTH);
        this.audType = CobolText.spaces(AuditType.LENGTH);
        this.audAction = CobolText.spaces(AuditAction.LENGTH);
        this.audStatus = CobolText.spaces(AuditStatus.LENGTH);
        this.audPortfolioId = CobolText.spaces(PORTFOLIO_ID_LENGTH);
        this.audAccountNo = CobolText.spaces(ACCOUNT_NO_LENGTH);
        this.audBeforeImage = CobolText.spaces(IMAGE_LENGTH);
        this.audAfterImage = CobolText.spaces(IMAGE_LENGTH);
        this.audMessage = CobolText.spaces(MESSAGE_LENGTH);
    }

    /** {@code AUD-HEADER} - the timestamp, system, user, program and terminal fields. */
    public String getAudHeader() {
        return audTimestamp + audSystemId + audUserId + audProgram + audTerminal;
    }

    /** {@code AUD-KEY-INFO} - the portfolio id and account number. */
    public String getAudKeyInfo() {
        return audPortfolioId + audAccountNo;
    }

    public String getAudTimestamp() {
        return audTimestamp;
    }

    public void setAudTimestamp(String audTimestamp) {
        this.audTimestamp = CobolText.picX(audTimestamp, TIMESTAMP_LENGTH);
    }

    public String getAudSystemId() {
        return audSystemId;
    }

    public void setAudSystemId(String audSystemId) {
        this.audSystemId = CobolText.picX(audSystemId, SYSTEM_ID_LENGTH);
    }

    public String getAudUserId() {
        return audUserId;
    }

    public void setAudUserId(String audUserId) {
        this.audUserId = CobolText.picX(audUserId, USER_ID_LENGTH);
    }

    public String getAudProgram() {
        return audProgram;
    }

    public void setAudProgram(String audProgram) {
        this.audProgram = CobolText.picX(audProgram, PROGRAM_LENGTH);
    }

    public String getAudTerminal() {
        return audTerminal;
    }

    public void setAudTerminal(String audTerminal) {
        this.audTerminal = CobolText.picX(audTerminal, TERMINAL_LENGTH);
    }

    /** The raw four bytes of {@code AUD-TYPE}. */
    public String getAudType() {
        return audType;
    }

    public void setAudType(String audType) {
        this.audType = CobolText.picX(audType, AuditType.LENGTH);
    }

    public void setAudType(AuditType type) {
        setAudType(type == null ? null : type.code());
    }

    /** The interpretation of {@code AUD-TYPE}, or {@code null} when no level-88 matches. */
    public AuditType getAuditType() {
        return AuditType.fromCode(audType);
    }

    /** The raw eight bytes of {@code AUD-ACTION}. */
    public String getAudAction() {
        return audAction;
    }

    public void setAudAction(String audAction) {
        this.audAction = CobolText.picX(audAction, AuditAction.LENGTH);
    }

    public void setAudAction(AuditAction action) {
        setAudAction(action == null ? null : action.code());
    }

    /** The interpretation of {@code AUD-ACTION}, or {@code null} when no level-88 matches. */
    public AuditAction getAuditAction() {
        return AuditAction.fromCode(audAction);
    }

    /** The raw four bytes of {@code AUD-STATUS}. */
    public String getAudStatus() {
        return audStatus;
    }

    public void setAudStatus(String audStatus) {
        this.audStatus = CobolText.picX(audStatus, AuditStatus.LENGTH);
    }

    public void setAudStatus(AuditStatus status) {
        setAudStatus(status == null ? null : status.code());
    }

    /** The interpretation of {@code AUD-STATUS}, or {@code null} when no level-88 matches. */
    public AuditStatus getAuditStatus() {
        return AuditStatus.fromCode(audStatus);
    }

    public String getAudPortfolioId() {
        return audPortfolioId;
    }

    public void setAudPortfolioId(String audPortfolioId) {
        this.audPortfolioId = CobolText.picX(audPortfolioId, PORTFOLIO_ID_LENGTH);
    }

    public String getAudAccountNo() {
        return audAccountNo;
    }

    public void setAudAccountNo(String audAccountNo) {
        this.audAccountNo = CobolText.picX(audAccountNo, ACCOUNT_NO_LENGTH);
    }

    public String getAudBeforeImage() {
        return audBeforeImage;
    }

    public void setAudBeforeImage(String audBeforeImage) {
        this.audBeforeImage = CobolText.picX(audBeforeImage, IMAGE_LENGTH);
    }

    public String getAudAfterImage() {
        return audAfterImage;
    }

    public void setAudAfterImage(String audAfterImage) {
        this.audAfterImage = CobolText.picX(audAfterImage, IMAGE_LENGTH);
    }

    /** {@code AUD-MESSAGE}, space-padded to 100 characters. */
    public String getAudMessage() {
        return audMessage;
    }

    /** {@code AUD-MESSAGE} without its trailing pad, for assertions and logging. */
    public String getAudMessageTrimmed() {
        return CobolText.trim(audMessage);
    }

    public void setAudMessage(String audMessage) {
        this.audMessage = CobolText.picX(audMessage, MESSAGE_LENGTH);
    }

    @Override
    public String toString() {
        return "AuditRecord[program=" + CobolText.trim(audProgram)
                + ", type=" + CobolText.trim(audType)
                + ", action=" + CobolText.trim(audAction)
                + ", status=" + CobolText.trim(audStatus)
                + ", portfolio=" + CobolText.trim(audPortfolioId)
                + ", message=" + getAudMessageTrimmed() + "]";
    }
}
