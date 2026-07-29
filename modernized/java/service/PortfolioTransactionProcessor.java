package com.clbs.portfolio.service;

import com.clbs.portfolio.model.AuditAction;
import com.clbs.portfolio.model.AuditRecord;
import com.clbs.portfolio.model.AuditStatus;
import com.clbs.portfolio.model.AuditType;
import com.clbs.portfolio.model.CobolDecimal;
import com.clbs.portfolio.model.CobolText;
import com.clbs.portfolio.model.ErrorCategory;
import com.clbs.portfolio.model.ErrorMessage;
import com.clbs.portfolio.model.PortfolioRecord;
import com.clbs.portfolio.model.TransactionRecord;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Translation of {@code src/programs/portfolio/PORTTRAN.cbl}, "Portfolio Transaction Processing".
 *
 * <p>Every method below is named after the paragraph it translates and names that paragraph in its
 * Javadoc. The program's working storage becomes instance state: one {@link ErrorMessage} area, one
 * {@link AuditRecord} area, one transaction record area, one portfolio record area and the three
 * counters, all reused across transactions exactly as the COBOL reuses them.
 *
 * <h2>The main flow only validates - the update logic is dead code</h2>
 *
 * <p><strong>{@code 0000-MAIN} never updates a portfolio and never writes an audit record.</strong>
 * {@code 2000-PROCESS-TRANSACTIONS} reads a record and performs {@code 2100-VALIDATE-TRANSACTION},
 * and nothing performs {@code 2200-UPDATE-POSITIONS}; the whole subtree below it -
 * {@code 2210-PROCESS-BUY}, {@code 2220-PROCESS-SELL}, {@code 2230-PROCESS-TRANSFER},
 * {@code 2240-PROCESS-FEE}, {@code 2300-UPDATE-AUDIT-TRAIL} and {@code 2310-WRITE-AUDIT-RECORD} -
 * is unreachable. That is G2 in {@code TRANSLATION-NOTES.md} and it is reproduced faithfully:
 * {@link #main()} counts and validates, so running it over a stream of perfectly valid buys leaves
 * the repository untouched and the audit processor uncalled. The update paragraphs are translated
 * anyway, as public methods the main flow does not call, so the logic they contain is captured and
 * can be tested directly.
 *
 * <h2>How the error strings are rendered</h2>
 *
 * <p>The texts are the COBOL literals byte for byte and are exposed as constants so tests can
 * assert against the same characters the source spells. Two of them are built by {@code STRING} and
 * one is built by {@code MOVE}, and the difference is preserved:
 *
 * <ul>
 *   <li>{@code MOVE 'literal' TO ERR-TEXT} replaces the whole 80-byte field, blank-padding it -
 *       {@link ErrorMessage#setErrText(String)}.</li>
 *   <li>{@code STRING ... DELIMITED BY SIZE INTO ERR-TEXT} overlays from the left and leaves the
 *       rest of the field as it was; it does not pad. Both {@code STRING} statements send
 *       {@code DELIMITED BY SIZE}, so the sending fields contribute their trailing spaces too:
 *       {@code 'Invalid Portfolio ID: '} is followed by all eight bytes of
 *       {@code TRN-PORTFOLIO-ID} and {@code 'Invalid Transaction Type: '} by both bytes of
 *       {@code TRN-TYPE}. {@link #stringInto} reproduces that, and because
 *       {@code 2100-VALIDATE-TRANSACTION} clears {@code ERR-TEXT} first, the residue is spaces in
 *       the main flow.</li>
 * </ul>
 *
 * <h2>How the audit message is rendered</h2>
 *
 * <p>{@code 2300-UPDATE-AUDIT-TRAIL} strings two {@code COMP-3} fields into {@code AUD-MESSAGE},
 * which IBM Enterprise COBOL does not allow (G8). The packed senders are rendered the way
 * {@link CobolDecimal#image} renders them - a sign character followed by the unscaled digits, so
 * {@code TRN-AMOUNT} contributes {@code +} plus fifteen digits and {@code TRN-QUANTITY} the same -
 * giving, for the seeded buy of 100 units for 12,500.00:
 *
 * <pre>
 * Transaction: BU Amount: +000000001250000 Units: +000000001000000
 * </pre>
 *
 * <p>The remaining discrepancies this class embodies are G1 (the holdings fields
 * {@code PORT-TOTAL-UNITS} and {@code PORT-TOTAL-COST} come from a copybook that does not exist),
 * G3 (transfers are unimplemented), G6 (errors carry no code and no severity) and G7 (the audit
 * status is decided by a possibly stale file status).
 *
 * <p>Nothing here does I/O: the files become {@link PortfolioRepository} and
 * {@link TransactionSource}, the subroutine calls become {@link AuditProcessor} and
 * {@link ErrorProcessor}, and all four are constructor-injected.
 */
public class PortfolioTransactionProcessor {

    /** {@code MOVE 'PORTTRAN' TO ERR-PROGRAM} and {@code MOVE 'PORTTRAN' TO AUD-PROGRAM}. */
    public static final String PROGRAM_ID = "PORTTRAN";

    /** The file status {@code 0000-MAIN} requires before it processes anything. */
    public static final String STATUS_SUCCESS = "00";

    /** {@code UNTIL ... OR WS-ERROR-COUNT > 100}. */
    public static final int ERROR_LIMIT = 100;

    public static final String ERR_PORTFOLIO_ID_REQUIRED = "Portfolio ID is required";
    public static final String ERR_INVALID_PORTFOLIO_ID_PREFIX = "Invalid Portfolio ID: ";
    public static final String ERR_INVALID_TRANSACTION_TYPE_PREFIX = "Invalid Transaction Type: ";
    public static final String ERR_QUANTITY_NOT_POSITIVE = "Quantity must be greater than zero";
    public static final String ERR_PRICE_NOT_POSITIVE = "Price must be greater than zero";
    public static final String ERR_AMOUNT_NOT_POSITIVE = "Amount must be greater than zero";
    public static final String ERR_PORTFOLIO_NOT_FOUND_FOR_UPDATE = "Portfolio not found for update";
    public static final String ERR_INSUFFICIENT_UNITS = "Insufficient units for sale";
    public static final String ERR_TRANSFER_NOT_IMPLEMENTED = "Transfer processing not implemented";
    public static final String ERR_PORTFOLIO_NOT_FOUND_FOR_FEE = "Portfolio not found for fee";
    public static final String ERR_UPDATING_PORTFOLIO = "Error updating portfolio";
    public static final String ERR_WRITING_AUDIT_RECORD = "Error writing audit record";
    public static final String ERR_OPENING_TRANSACTION_FILE = "Error opening transaction file";
    public static final String ERR_OPENING_PORTFOLIO_FILE = "Error opening portfolio file";

    /** The digits of the {@code PIC 9(8) COMP} counters in {@code WS-COUNTERS}. */
    private static final int COUNTER_DIGITS = 8;

    private final PortfolioRepository portfolioRepository;
    private final TransactionSource transactionSource;
    private final AuditProcessor auditProcessor;
    private final ErrorProcessor errorProcessor;
    private final Clock clock;
    private final String userId;

    /** {@code COPY ERRHAND} - the single {@code ERR-MESSAGE} working-storage area. */
    private final ErrorMessage errorMessage = new ErrorMessage();

    /** {@code COPY AUDITLOG} - the single {@code AUDIT-RECORD} working-storage area. */
    private final AuditRecord auditRecord = new AuditRecord();

    /** The {@code TRANSACTION-FILE} record area, refilled by every {@code READ}. */
    private TransactionRecord transactionRecord = new TransactionRecord();

    /** The {@code PORTFOLIO-FILE} record area; a failed {@code READ} leaves it as it was. */
    private PortfolioRecord portfolioRecord = new PortfolioRecord();

    /** {@code WS-TRAN-STATUS}. */
    private String wsTranStatus = CobolText.spaces(2);

    private int wsReadCount;
    private int wsProcessCount;
    private int wsErrorCount;

    /** {@code WS-EOF-FLAG}: {@code 88 END-OF-FILE} / {@code 88 MORE-RECORDS}. */
    private boolean endOfFile;

    /** The lines {@code 3000-TERMINATE} sends to SYSOUT with {@code DISPLAY}. */
    private final List<String> displayLines = new ArrayList<>();

    public PortfolioTransactionProcessor(
            PortfolioRepository portfolioRepository,
            TransactionSource transactionSource,
            AuditProcessor auditProcessor,
            ErrorProcessor errorProcessor) {
        this(
                portfolioRepository,
                transactionSource,
                auditProcessor,
                errorProcessor,
                Clock.systemDefaultZone(),
                System.getProperty("user.name", ""));
    }

    /**
     * As the four-argument constructor, with the two values {@code 2300-UPDATE-AUDIT-TRAIL} takes
     * from the runtime supplied explicitly so a test can pin them.
     *
     * @param clock  the source of {@code FUNCTION CURRENT-DATE}
     * @param userId the value of {@code FUNCTION USER-ID}, stored in the eight bytes of
     *               {@code AUD-USER-ID}
     */
    public PortfolioTransactionProcessor(
            PortfolioRepository portfolioRepository,
            TransactionSource transactionSource,
            AuditProcessor auditProcessor,
            ErrorProcessor errorProcessor,
            Clock clock,
            String userId) {
        this.portfolioRepository = portfolioRepository;
        this.transactionSource = transactionSource;
        this.auditProcessor = auditProcessor;
        this.errorProcessor = errorProcessor;
        this.clock = clock;
        this.userId = userId;
    }

    /**
     * {@code 0000-MAIN}. Initialises, processes transactions while the transaction file opened
     * cleanly and neither the end of file nor the error cutoff has been reached, then terminates.
     *
     * <p>The cutoff is the COBOL test, unchanged: {@code PERFORM UNTIL ... OR WS-ERROR-COUNT > 100}
     * is evaluated <em>before</em> each iteration, so the loop runs one more time on an error count
     * of exactly 100 and stops once an iteration has pushed the count past it.
     */
    public void main() {
        initialize();

        if (STATUS_SUCCESS.equals(wsTranStatus)) {
            while (!endOfFile && wsErrorCount <= ERROR_LIMIT) {
                processTransactions();
            }
        }

        terminate();
    }

    /**
     * {@code 1000-INITIALIZE}. Clears the file statuses and counters, sets {@code MORE-RECORDS} and
     * opens both files, raising an error for each open that does not leave {@code '00'}.
     */
    public void initialize() {
        wsTranStatus = CobolText.spaces(2);
        wsReadCount = 0;
        wsProcessCount = 0;
        wsErrorCount = 0;
        endOfFile = false;

        wsTranStatus = CobolText.picX(transactionSource.open(), 2);
        if (!STATUS_SUCCESS.equals(wsTranStatus)) {
            errorMessage.setErrText(ERR_OPENING_TRANSACTION_FILE);
            errorRoutine();
        }

        if (!STATUS_SUCCESS.equals(CobolText.picX(portfolioRepository.open(), 2))) {
            errorMessage.setErrText(ERR_OPENING_PORTFOLIO_FILE);
            errorRoutine();
        }
    }

    /**
     * {@code 2000-PROCESS-TRANSACTIONS}. Reads one transaction; at end of file it sets the flag,
     * otherwise it counts the record and validates it.
     *
     * <p>It does <em>not</em> perform {@code 2200-UPDATE-POSITIONS}: see the class Javadoc and G2.
     */
    public void processTransactions() {
        TransactionRecord read = transactionSource.read();
        if (read == null) {
            endOfFile = true;
            return;
        }
        transactionRecord = new TransactionRecord(read);
        wsReadCount = CobolText.pic9(wsReadCount + 1L, COUNTER_DIGITS);
        validateTransaction();
    }

    /**
     * {@code 2100-VALIDATE-TRANSACTION}. Clears {@code ERR-TEXT}, then runs the three checks in
     * order, each guarded by {@code IF ERR-TEXT = SPACES} so the first failure short-circuits the
     * rest. A clean record increments {@code WS-PROCESS-COUNT}; a failed one is logged.
     */
    public void validateTransaction() {
        errorMessage.clearErrText();

        checkPortfolio();
        if (errorMessage.isErrTextSpaces()) {
            checkTransactionType();
        }
        if (errorMessage.isErrTextSpaces()) {
            checkAmounts();
        }

        if (errorMessage.isErrTextSpaces()) {
            wsProcessCount = CobolText.pic9(wsProcessCount + 1L, COUNTER_DIGITS);
        } else {
            errorRoutine();
        }
    }

    /**
     * {@code 2110-CHECK-PORTFOLIO}. A blank portfolio id fails immediately; otherwise the id is
     * moved into the record key and read, and an {@code INVALID KEY} builds the echo message.
     *
     * <p>{@code MOVE TRN-PORTFOLIO-ID TO PORT-ID} happens before the read and is not undone by it,
     * so after a failed read the record area holds the requested key over the previous record's
     * remaining fields - which is what {@code 2300-UPDATE-AUDIT-TRAIL} would go on to copy.
     */
    public void checkPortfolio() {
        if (CobolText.isSpaces(transactionRecord.getTrnPortfolioId())) {
            errorMessage.setErrText(ERR_PORTFOLIO_ID_REQUIRED);
            return;
        }

        portfolioRecord.setPortId(transactionRecord.getTrnPortfolioId());
        Optional<PortfolioRecord> found = portfolioRepository.findById(portfolioRecord.getPortId());
        if (found.isPresent()) {
            portfolioRecord = new PortfolioRecord(found.get());
        } else {
            errorMessage.setErrText(
                    stringInto(
                            errorMessage.getErrText(),
                            ErrorMessage.TEXT_LENGTH,
                            ERR_INVALID_PORTFOLIO_ID_PREFIX,
                            transactionRecord.getTrnPortfolioId()));
        }
    }

    /**
     * {@code 2120-CHECK-TRANSACTION-TYPE}. {@code BU}, {@code SL}, {@code TR} and {@code FE} pass;
     * anything else is echoed back in the error text.
     */
    public void checkTransactionType() {
        String type = transactionRecord.getTrnType();
        switch (type) {
            case "BU":
            case "SL":
            case "TR":
            case "FE":
                break;
            default:
                errorMessage.setErrText(
                        stringInto(
                                errorMessage.getErrText(),
                                ErrorMessage.TEXT_LENGTH,
                                ERR_INVALID_TRANSACTION_TYPE_PREFIX,
                                type));
                break;
        }
    }

    /**
     * {@code 2130-CHECK-AMOUNTS}. The quantity check applies to every transaction type, including
     * {@code TR}; the price and amount checks are written {@code AND TRN-TYPE NOT = 'TR'} and so
     * exempt transfers, which is the only way a transaction can validate with a zero price and a
     * zero amount. Each failure exits the paragraph.
     */
    public void checkAmounts() {
        if (CobolDecimal.isNotPositive(transactionRecord.getTrnQuantity())) {
            errorMessage.setErrText(ERR_QUANTITY_NOT_POSITIVE);
            return;
        }

        boolean transfer = "TR".equals(transactionRecord.getTrnType());

        if (CobolDecimal.isNotPositive(transactionRecord.getTrnPrice()) && !transfer) {
            errorMessage.setErrText(ERR_PRICE_NOT_POSITIVE);
            return;
        }

        if (CobolDecimal.isNotPositive(transactionRecord.getTrnAmount()) && !transfer) {
            errorMessage.setErrText(ERR_AMOUNT_NOT_POSITIVE);
        }
    }

    /**
     * {@code 2200-UPDATE-POSITIONS}. Dispatches on {@code TRN-TYPE} and then always updates the
     * audit trail.
     *
     * <p>Nothing performs this paragraph (G2); it is reachable only by calling it. The
     * {@code EVALUATE} has no {@code WHEN OTHER}, so an unrecognised type updates nothing and still
     * writes an audit record (G11).
     */
    public void updatePositions() {
        switch (transactionRecord.getTrnType()) {
            case "BU":
                processBuy();
                break;
            case "SL":
                processSell();
                break;
            case "TR":
                processTransfer();
                break;
            case "FE":
                processFee();
                break;
            default:
                break;
        }

        updateAuditTrail();
    }

    /**
     * {@code 2210-PROCESS-BUY}. Re-reads the portfolio, adds the transaction quantity to
     * {@code PORT-TOTAL-UNITS} and the amount to {@code PORT-TOTAL-COST}, and rewrites the record.
     * Both fields are the synthetic ones described in G1.
     */
    public void processBuy() {
        if (!readPortfolioForUpdate(ERR_PORTFOLIO_NOT_FOUND_FOR_UPDATE)) {
            return;
        }

        portfolioRecord.setPortTotalUnits(
                portfolioRecord.getPortTotalUnits().add(transactionRecord.getTrnQuantity()));
        portfolioRecord.setPortTotalCost(
                portfolioRecord.getPortTotalCost().add(transactionRecord.getTrnAmount()));

        rewritePortfolio();
    }

    /**
     * {@code 2220-PROCESS-SELL}. Re-reads the portfolio, refuses the sale when the holding is
     * smaller than the quantity sold, otherwise subtracts the quantity from
     * {@code PORT-TOTAL-UNITS} and the amount from {@code PORT-TOTAL-COST} and rewrites.
     */
    public void processSell() {
        if (!readPortfolioForUpdate(ERR_PORTFOLIO_NOT_FOUND_FOR_UPDATE)) {
            return;
        }

        if (portfolioRecord.getPortTotalUnits().compareTo(transactionRecord.getTrnQuantity()) < 0) {
            errorMessage.setErrText(ERR_INSUFFICIENT_UNITS);
            errorRoutine();
            return;
        }

        portfolioRecord.setPortTotalUnits(
                portfolioRecord.getPortTotalUnits().subtract(transactionRecord.getTrnQuantity()));
        portfolioRecord.setPortTotalCost(
                portfolioRecord.getPortTotalCost().subtract(transactionRecord.getTrnAmount()));

        rewritePortfolio();
    }

    /**
     * {@code 2230-PROCESS-TRANSFER}. The paragraph is two statements long: it sets the error text
     * and logs it. No transfer is performed and no file is touched, so the audit status that
     * follows is decided by whatever the previous file operation left behind (G3, G7).
     */
    public void processTransfer() {
        errorMessage.setErrText(ERR_TRANSFER_NOT_IMPLEMENTED);
        errorRoutine();
    }

    /**
     * {@code 2240-PROCESS-FEE}. Re-reads the portfolio, subtracts the fee amount from
     * {@code PORT-TOTAL-COST} - the units are left alone and no other field is touched - and
     * rewrites. Note the error text for a missing portfolio differs from the buy and sell paths.
     */
    public void processFee() {
        if (!readPortfolioForUpdate(ERR_PORTFOLIO_NOT_FOUND_FOR_FEE)) {
            return;
        }

        portfolioRecord.setPortTotalCost(
                portfolioRecord.getPortTotalCost().subtract(transactionRecord.getTrnAmount()));

        rewritePortfolio();
    }

    /**
     * The {@code MOVE TRN-PORTFOLIO-ID TO PORT-ID} / {@code READ PORTFOLIO-FILE} / {@code INVALID
     * KEY} prologue shared by {@code 2210}, {@code 2220} and {@code 2240}, which differ only in the
     * text they report.
     *
     * @return whether the record was read, that is, whether the paragraph continues
     */
    private boolean readPortfolioForUpdate(String notFoundText) {
        portfolioRecord.setPortId(transactionRecord.getTrnPortfolioId());
        Optional<PortfolioRecord> found = portfolioRepository.findById(portfolioRecord.getPortId());
        if (!found.isPresent()) {
            errorMessage.setErrText(notFoundText);
            errorRoutine();
            return false;
        }
        portfolioRecord = new PortfolioRecord(found.get());
        return true;
    }

    /**
     * The {@code REWRITE PORTFOLIO-RECORD} / {@code INVALID KEY} epilogue shared by {@code 2210},
     * {@code 2220} and {@code 2240}.
     */
    private void rewritePortfolio() {
        portfolioRepository.update(portfolioRecord);
        if (PortfolioRepository.isInvalidKey(portfolioRepository.getFileStatus())) {
            errorMessage.setErrText(ERR_UPDATING_PORTFOLIO);
            errorRoutine();
        }
    }

    /**
     * {@code 2300-UPDATE-AUDIT-TRAIL}. Builds the audit record from the transaction and the
     * portfolio record area and writes it.
     *
     * <p>{@code AUD-STATUS} is {@code SUCC} when {@code WS-PORT-STATUS} is {@code '00'} and
     * {@code FAIL} otherwise, and {@code WS-PORT-STATUS} is the status of the last operation on the
     * portfolio file - the rewrite on the buy and sell paths, but something older on any path that
     * did no I/O (G7). {@code AUD-BEFORE-IMAGE} takes the record area as characters, truncated to
     * its hundred bytes by the group move, and {@code AUD-MESSAGE} is the {@code STRING} over
     * packed fields described in the class Javadoc (G8).
     */
    public void updateAuditTrail() {
        auditRecord.initialize();

        auditRecord.setAudTimestamp(currentDate());
        auditRecord.setAudProgram(PROGRAM_ID);
        auditRecord.setAudUserId(userId);
        auditRecord.setAudType(AuditType.TRANSACTION);

        switch (transactionRecord.getTrnType()) {
            case "BU":
                auditRecord.setAudAction(AuditAction.CREATE);
                break;
            case "SL":
                auditRecord.setAudAction(AuditAction.DELETE);
                break;
            case "TR":
            case "FE":
                auditRecord.setAudAction(AuditAction.UPDATE);
                break;
            default:
                break;
        }

        if (STATUS_SUCCESS.equals(portfolioRepository.getFileStatus())) {
            auditRecord.setAudStatus(AuditStatus.SUCCESS);
        } else {
            auditRecord.setAudStatus(AuditStatus.FAILURE);
        }

        auditRecord.setAudPortfolioId(transactionRecord.getTrnPortfolioId());
        auditRecord.setAudAccountNo(portfolioRecord.getPortAccountNo());

        auditRecord.setAudBeforeImage(portfolioRecord.toRecordImage());

        auditRecord.setAudMessage(
                stringInto(
                        auditRecord.getAudMessage(),
                        AuditRecord.MESSAGE_LENGTH,
                        "Transaction: ",
                        transactionRecord.getTrnType(),
                        " Amount: ",
                        CobolDecimal.image(
                                transactionRecord.getTrnAmount(),
                                CobolDecimal.AMOUNT_DIGITS,
                                CobolDecimal.AMOUNT_SCALE),
                        " Units: ",
                        CobolDecimal.image(
                                transactionRecord.getTrnQuantity(),
                                CobolDecimal.QUANTITY_DIGITS,
                                CobolDecimal.QUANTITY_SCALE)));

        writeAuditRecord();
    }

    /**
     * {@code 2310-WRITE-AUDIT-RECORD}. Calls {@code AUDPROC} and reports a non-zero result as an
     * error.
     *
     * <p>The COBOL tests the {@code RETURN-CODE} special register, which {@code AUDPROC} never sets
     * (G4); the translation checks the status the subroutine reports, which is what the test was
     * reaching for.
     */
    public void writeAuditRecord() {
        int returnCode = auditProcessor.process(auditRecord);
        if (returnCode != AuditProcessor.RETURN_SUCCESS) {
            errorMessage.setErrText(ERR_WRITING_AUDIT_RECORD);
            errorRoutine();
        }
    }

    /**
     * {@code 3000-TERMINATE}. Closes both files and displays the three counters.
     *
     * <p>The files are closed unconditionally, including after an open that failed. The counters
     * are {@code PIC 9(8) COMP}, so {@code DISPLAY} renders each as eight digits; the lines are
     * collected in {@link #getDisplayLines()} rather than printed, since SYSOUT is not a Java
     * concept.
     */
    public void terminate() {
        transactionSource.close();
        portfolioRepository.close();

        displayLines.add("Transactions Read:    " + CobolText.pic9Image(wsReadCount, COUNTER_DIGITS));
        displayLines.add("Transactions Process: " + CobolText.pic9Image(wsProcessCount, COUNTER_DIGITS));
        displayLines.add("Errors Encountered:   " + CobolText.pic9Image(wsErrorCount, COUNTER_DIGITS));
    }

    /**
     * {@code 9000-ERROR-ROUTINE}. Counts the error, stamps the category and the program on the
     * shared {@code ERR-MESSAGE} area and calls {@code ERRPROC}.
     *
     * <p>{@code ERR-CODE} and {@code ERR-SEVERITY} are deliberately left as the caller found them -
     * the paragraph never assigns either, so every error reaches the subroutine with a blank code
     * and severity zero (G6).
     */
    public void errorRoutine() {
        wsErrorCount = CobolText.pic9(wsErrorCount + 1L, COUNTER_DIGITS);
        errorMessage.setErrCategory(ErrorCategory.PROCESSING);
        errorMessage.setErrProgram(PROGRAM_ID);

        errorProcessor.process(errorMessage);
    }

    /**
     * {@code STRING sender-1 ... DELIMITED BY SIZE INTO receiver}: the senders are laid over the
     * receiver from the left, the rest of the receiver keeps whatever it held, and characters past
     * the end of the receiver are discarded - neither statement in {@code PORTTRAN} has an
     * {@code ON OVERFLOW} phrase.
     */
    private static String stringInto(String receiver, int length, String... senders) {
        StringBuilder buffer = new StringBuilder(CobolText.picX(receiver, length));
        int pointer = 0;
        for (String sender : senders) {
            for (int i = 0; i < sender.length() && pointer < length; i++, pointer++) {
                buffer.setCharAt(pointer, sender.charAt(i));
            }
        }
        return buffer.toString();
    }

    /**
     * {@code FUNCTION CURRENT-DATE} - the 21 characters {@code YYYYMMDDhhmmssnn} plus the offset
     * from Greenwich Mean Time as {@code +hhmm} or {@code -hhmm}, which {@code MOVE} pads out to
     * the 26 bytes of {@code AUD-TIMESTAMP}.
     */
    private String currentDate() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        int offsetMinutes = now.getOffset().getTotalSeconds() / 60;
        char sign = offsetMinutes < 0 ? '-' : '+';
        int absoluteMinutes = Math.abs(offsetMinutes);
        return String.format(
                "%04d%02d%02d%02d%02d%02d%02d%s%02d%02d",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour(),
                now.getMinute(),
                now.getSecond(),
                now.getNano() / 10_000_000,
                sign,
                absoluteMinutes / 60,
                absoluteMinutes % 60);
    }

    /** {@code ERR-MESSAGE} - the shared error area, as the last error left it. */
    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    /** {@code AUDIT-RECORD} - the shared audit area, as the last audit write left it. */
    public AuditRecord getAuditRecord() {
        return auditRecord;
    }

    /** The {@code TRANSACTION-FILE} record area. */
    public TransactionRecord getTransactionRecord() {
        return transactionRecord;
    }

    /**
     * Fills the {@code TRANSACTION-FILE} record area, standing in for the {@code READ} that would
     * have filled it, so that the paragraphs the main flow never reaches can be driven directly.
     */
    public void setTransactionRecord(TransactionRecord transactionRecord) {
        this.transactionRecord = new TransactionRecord(transactionRecord);
    }

    /** The {@code PORTFOLIO-FILE} record area. */
    public PortfolioRecord getPortfolioRecord() {
        return portfolioRecord;
    }

    /** {@code WS-TRAN-STATUS}. */
    public String getWsTranStatus() {
        return wsTranStatus;
    }

    /** {@code WS-PORT-STATUS}, which lives with the file it belongs to. */
    public String getWsPortStatus() {
        return portfolioRepository.getFileStatus();
    }

    /** {@code WS-READ-COUNT}. */
    public int getWsReadCount() {
        return wsReadCount;
    }

    /** {@code WS-PROCESS-COUNT}. */
    public int getWsProcessCount() {
        return wsProcessCount;
    }

    /** {@code WS-ERROR-COUNT}. */
    public int getWsErrorCount() {
        return wsErrorCount;
    }

    /** {@code 88 END-OF-FILE}. */
    public boolean isEndOfFile() {
        return endOfFile;
    }

    /** The {@code DISPLAY} output of {@code 3000-TERMINATE}, in order. */
    public List<String> getDisplayLines() {
        return Collections.unmodifiableList(displayLines);
    }
}
