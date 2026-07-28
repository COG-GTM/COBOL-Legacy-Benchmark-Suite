package com.clbs.portfolio.model;

/**
 * Translation of {@code 01 ERR-MESSAGE} in {@code src/copybook/common/ERRHAND.cpy}, the
 * working-storage area {@code PORTTRAN} fills in before {@code CALL 'ERRPROC'}.
 *
 * <pre>
 * 05 ERR-TIMESTAMP
 *    10 ERR-DATE      PIC X(10)
 *    10 ERR-TIME      PIC X(8)
 * 05 ERR-PROGRAM      PIC X(8)
 * 05 ERR-CATEGORY     PIC X(2)          -&gt; {@link ErrorCategory}
 * 05 ERR-CODE         PIC X(4)          -&gt; {@link ErrorCode}, never populated by PORTTRAN
 * 05 ERR-SEVERITY     PIC S9(4) COMP    -&gt; {@link ErrorSeverity}
 * 05 ERR-TEXT         PIC X(80)
 * 05 ERR-DETAILS      PIC X(256)
 * </pre>
 *
 * <p>{@code PORTTRAN} drives its validation off {@code IF ERR-TEXT = SPACES}, so this class keeps
 * {@code ERR-TEXT} space-padded to its declared 80 characters and exposes {@link #isErrTextSpaces()}
 * for that test. A single instance stands for the single working-storage area: callers that need to
 * retain an error past the next transaction must take a {@link #ErrorMessage(ErrorMessage) copy}.
 */
public class ErrorMessage {

    public static final int DATE_LENGTH = 10;
    public static final int TIME_LENGTH = 8;
    public static final int PROGRAM_LENGTH = 8;
    public static final int TEXT_LENGTH = 80;
    public static final int DETAILS_LENGTH = 256;

    private String errDate = CobolText.spaces(DATE_LENGTH);
    private String errTime = CobolText.spaces(TIME_LENGTH);
    private String errProgram = CobolText.spaces(PROGRAM_LENGTH);
    private String errCategory = CobolText.spaces(ErrorCategory.LENGTH);
    private String errCode = CobolText.spaces(ErrorCode.LENGTH);
    private int errSeverity;
    private String errText = CobolText.spaces(TEXT_LENGTH);
    private String errDetails = CobolText.spaces(DETAILS_LENGTH);

    public ErrorMessage() {
    }

    /** Copy constructor, for retaining the contents of the shared working-storage area. */
    public ErrorMessage(ErrorMessage other) {
        this.errDate = other.errDate;
        this.errTime = other.errTime;
        this.errProgram = other.errProgram;
        this.errCategory = other.errCategory;
        this.errCode = other.errCode;
        this.errSeverity = other.errSeverity;
        this.errText = other.errText;
        this.errDetails = other.errDetails;
    }

    /** {@code MOVE SPACES TO ERR-TEXT}, the reset that opens {@code 2100-VALIDATE-TRANSACTION}. */
    public void clearErrText() {
        this.errText = CobolText.spaces(TEXT_LENGTH);
    }

    /** {@code IF ERR-TEXT = SPACES}. */
    public boolean isErrTextSpaces() {
        return CobolText.isSpaces(errText);
    }

    /** {@code ERR-TIMESTAMP} - the concatenated date and time fields. */
    public String getErrTimestamp() {
        return errDate + errTime;
    }

    public String getErrDate() {
        return errDate;
    }

    public void setErrDate(String errDate) {
        this.errDate = CobolText.picX(errDate, DATE_LENGTH);
    }

    public String getErrTime() {
        return errTime;
    }

    public void setErrTime(String errTime) {
        this.errTime = CobolText.picX(errTime, TIME_LENGTH);
    }

    public String getErrProgram() {
        return errProgram;
    }

    public void setErrProgram(String errProgram) {
        this.errProgram = CobolText.picX(errProgram, PROGRAM_LENGTH);
    }

    /** The raw two bytes of {@code ERR-CATEGORY}. */
    public String getErrCategory() {
        return errCategory;
    }

    public void setErrCategory(String errCategory) {
        this.errCategory = CobolText.picX(errCategory, ErrorCategory.LENGTH);
    }

    public void setErrCategory(ErrorCategory category) {
        setErrCategory(category == null ? null : category.code());
    }

    /** The interpretation of {@code ERR-CATEGORY}, or {@code null} when no entry matches. */
    public ErrorCategory getErrorCategory() {
        return ErrorCategory.fromCode(errCategory);
    }

    /** The raw four bytes of {@code ERR-CODE}; spaces for every error {@code PORTTRAN} raises. */
    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = CobolText.picX(errCode, ErrorCode.LENGTH);
    }

    public void setErrCode(ErrorCode errorCode) {
        setErrCode(errorCode == null ? null : errorCode.code());
    }

    public int getErrSeverity() {
        return errSeverity;
    }

    public void setErrSeverity(int errSeverity) {
        this.errSeverity = errSeverity;
    }

    public void setErrSeverity(ErrorSeverity severity) {
        this.errSeverity = severity == null ? 0 : severity.value();
    }

    /** The interpretation of {@code ERR-SEVERITY}, or {@code null} for an undocumented value. */
    public ErrorSeverity getErrorSeverity() {
        return ErrorSeverity.fromValue(errSeverity);
    }

    /** {@code ERR-TEXT}, space-padded to 80 characters. */
    public String getErrText() {
        return errText;
    }

    /** {@code ERR-TEXT} without its trailing pad, for assertions and logging. */
    public String getErrTextTrimmed() {
        return CobolText.trim(errText);
    }

    public void setErrText(String errText) {
        this.errText = CobolText.picX(errText, TEXT_LENGTH);
    }

    public String getErrDetails() {
        return errDetails;
    }

    public void setErrDetails(String errDetails) {
        this.errDetails = CobolText.picX(errDetails, DETAILS_LENGTH);
    }

    @Override
    public String toString() {
        return "ErrorMessage[program=" + CobolText.trim(errProgram)
                + ", category=" + CobolText.trim(errCategory)
                + ", code=" + CobolText.trim(errCode)
                + ", severity=" + errSeverity
                + ", text=" + getErrTextTrimmed() + "]";
    }
}
