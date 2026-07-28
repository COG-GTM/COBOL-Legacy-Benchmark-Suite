package com.clbs.portfolio.service;

import com.clbs.portfolio.harness.RecordingAuditProcessor;
import com.clbs.portfolio.harness.RecordingErrorProcessor;
import com.clbs.portfolio.harness.TestData;
import com.clbs.portfolio.model.AuditRecord;
import com.clbs.portfolio.model.AuditStatus;
import com.clbs.portfolio.model.AuditType;
import com.clbs.portfolio.model.CobolDecimal;
import com.clbs.portfolio.model.CobolText;
import com.clbs.portfolio.model.ErrorCategory;
import com.clbs.portfolio.model.ErrorMessage;
import com.clbs.portfolio.model.PortfolioRecord;
import com.clbs.portfolio.model.TransactionRecord;
import com.clbs.portfolio.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the translation of {@code PORTTRAN.cbl}.
 *
 * <p>They are grouped the way the program is: the validation paragraphs that the main flow reaches,
 * the update paragraphs that it does not (G2), the audit trail, and the control flow of
 * {@code 0000-MAIN} itself. Several tests exist to pin a defect rather than a feature; each of
 * those names the {@code G} entry in {@code TRANSLATION-NOTES.md} that describes it.
 */
class PortfolioTransactionProcessorTest {

    /** 2024-03-20 15:30:45.12 UTC, so {@code FUNCTION CURRENT-DATE} is fully determined. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2024-03-20T15:30:45.120Z"), ZoneOffset.UTC);

    private static final String CURRENT_DATE = "2024032015304512+0000";

    private InMemoryPortfolioRepository repository;
    private RecordingAuditProcessor auditProcessor;
    private RecordingErrorProcessor errorProcessor;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPortfolioRepository()
                .seed(TestData.growthPortfolio())
                .seed(TestData.incomePortfolio());
        auditProcessor = new RecordingAuditProcessor();
        errorProcessor = new RecordingErrorProcessor();
    }

    private PortfolioTransactionProcessor processorOver(TransactionSource source) {
        return new PortfolioTransactionProcessor(
                repository, source, auditProcessor, errorProcessor, FIXED_CLOCK, TestData.USER_ID);
    }

    /** Runs {@code 0000-MAIN} over the given transactions. */
    private PortfolioTransactionProcessor runMain(TransactionRecord... transactions) {
        PortfolioTransactionProcessor processor =
                processorOver(new ListTransactionSource(transactions));
        processor.main();
        return processor;
    }

    /**
     * A processor whose transaction file is empty and already open, for driving the paragraphs the
     * main flow never performs.
     */
    private PortfolioTransactionProcessor processorFor(TransactionRecord transaction) {
        PortfolioTransactionProcessor processor = processorOver(new ListTransactionSource());
        processor.initialize();
        processor.setTransactionRecord(transaction);
        return processor;
    }

    @Nested
    @DisplayName("2100-VALIDATE-TRANSACTION")
    class Validation {

        @Test
        @DisplayName("a buy passes every check")
        void buyValidates() {
            PortfolioTransactionProcessor processor = runMain(TestData.buyTransaction());

            assertEquals(1, processor.getWsReadCount());
            assertEquals(1, processor.getWsProcessCount());
            assertEquals(0, processor.getWsErrorCount());
            assertEquals(0, errorProcessor.count());
        }

        @Test
        @DisplayName("a sell passes every check")
        void sellValidates() {
            PortfolioTransactionProcessor processor = runMain(TestData.sellTransaction());

            assertEquals(1, processor.getWsProcessCount());
            assertEquals(0, errorProcessor.count());
        }

        @Test
        @DisplayName("a fee passes every check")
        void feeValidates() {
            PortfolioTransactionProcessor processor = runMain(TestData.feeTransaction());

            assertEquals(1, processor.getWsProcessCount());
            assertEquals(0, errorProcessor.count());
        }

        @Test
        @DisplayName("2130 exempts TR from the price and amount checks, so a transfer validates "
                + "with both at zero")
        void transferValidatesWithZeroPriceAndAmount() {
            TransactionRecord transfer = TestData.transferTransaction();
            assertEquals(CobolDecimal.ZERO_QUANTITY, transfer.getTrnPrice());
            assertEquals(CobolDecimal.ZERO_AMOUNT, transfer.getTrnAmount());

            PortfolioTransactionProcessor processor = runMain(transfer);

            assertEquals(1, processor.getWsProcessCount());
            assertEquals(0, errorProcessor.count());
        }

        @Test
        @DisplayName("2130 does not exempt TR from the quantity check")
        void transferStillNeedsAQuantity() {
            TransactionRecord transfer = TestData.transferTransaction();
            transfer.setTrnQuantity("0.0000");

            runMain(transfer);

            assertEquals(
                    PortfolioTransactionProcessor.ERR_QUANTITY_NOT_POSITIVE,
                    errorProcessor.lastMessage());
        }

        @Test
        @DisplayName("2110 rejects a blank portfolio id")
        void blankPortfolioId() {
            TransactionRecord transaction = TestData.buyTransaction();
            transaction.setTrnPortfolioId("");

            PortfolioTransactionProcessor processor = runMain(transaction);

            assertEquals(
                    PortfolioTransactionProcessor.ERR_PORTFOLIO_ID_REQUIRED,
                    errorProcessor.lastMessage());
            assertEquals(0, processor.getWsProcessCount());
            assertEquals(1, processor.getWsErrorCount());
        }

        @Test
        @DisplayName("2110 echoes an unknown portfolio id, STRING DELIMITED BY SIZE")
        void unknownPortfolioId() {
            TransactionRecord transaction = TestData.buyTransaction();
            transaction.setTrnPortfolioId(TestData.MISSING_PORTFOLIO_ID);

            PortfolioTransactionProcessor processor = runMain(transaction);

            assertEquals("Invalid Portfolio ID: PORT9999", errorProcessor.lastMessage());
            assertEquals(1, processor.getWsErrorCount());
        }

        @Test
        @DisplayName("the STRING leaves the rest of the 80-byte ERR-TEXT as it found it")
        void stringDoesNotPadErrText() {
            TransactionRecord transaction = TestData.buyTransaction();
            transaction.setTrnPortfolioId("PORT99  ");

            PortfolioTransactionProcessor processor = runMain(transaction);

            String errText = processor.getErrorMessage().getErrText();
            assertEquals(ErrorMessage.TEXT_LENGTH, errText.length());
            assertEquals(
                    CobolText.picX("Invalid Portfolio ID: PORT99  ", ErrorMessage.TEXT_LENGTH),
                    errText);
        }

        @Test
        @DisplayName("2120 echoes a transaction type no level-88 covers")
        void unrecognisedTransactionType() {
            PortfolioTransactionProcessor processor =
                    runMain(TestData.transactionWithRawType("XX"));

            assertEquals("Invalid Transaction Type: XX", errorProcessor.lastMessage());
            assertEquals(0, processor.getWsProcessCount());
        }

        @Test
        @DisplayName("2130 rejects a non-positive quantity")
        void nonPositiveQuantity() {
            TransactionRecord zero = TestData.buyTransaction();
            zero.setTrnQuantity("0.0000");
            TransactionRecord negative = TestData.buyTransaction();
            negative.setTrnQuantity("-1.0000");

            runMain(zero, negative);

            assertEquals(
                    Arrays.asList(
                            PortfolioTransactionProcessor.ERR_QUANTITY_NOT_POSITIVE,
                            PortfolioTransactionProcessor.ERR_QUANTITY_NOT_POSITIVE),
                    errorProcessor.messages());
        }

        @Test
        @DisplayName("2130 rejects a non-positive price on a type other than TR")
        void nonPositivePrice() {
            TransactionRecord transaction = TestData.buyTransaction();
            transaction.setTrnPrice("0.0000");

            runMain(transaction);

            assertEquals(
                    PortfolioTransactionProcessor.ERR_PRICE_NOT_POSITIVE,
                    errorProcessor.lastMessage());
        }

        @Test
        @DisplayName("2130 rejects a non-positive amount on a type other than TR")
        void nonPositiveAmount() {
            TransactionRecord transaction = TestData.buyTransaction();
            transaction.setTrnAmount("0.00");

            runMain(transaction);

            assertEquals(
                    PortfolioTransactionProcessor.ERR_AMOUNT_NOT_POSITIVE,
                    errorProcessor.lastMessage());
        }

        @Test
        @DisplayName("several faults report only the first check to fail")
        void shortCircuitsOnTheFirstFault() {
            TransactionRecord transaction = TestData.buyTransaction();
            transaction.setTrnPortfolioId("");
            transaction.setTrnType("ZZ");
            transaction.setTrnQuantity("0.0000");

            runMain(transaction);

            assertEquals(
                    Collections.singletonList(
                            PortfolioTransactionProcessor.ERR_PORTFOLIO_ID_REQUIRED),
                    errorProcessor.messages());
        }

        @Test
        @DisplayName("a bad type hides a bad quantity, since 2130 never runs")
        void typeCheckPrecedesTheAmountChecks() {
            TransactionRecord transaction = TestData.transactionWithRawType("ZZ");
            transaction.setTrnQuantity("0.0000");

            runMain(transaction);

            assertEquals(
                    Collections.singletonList("Invalid Transaction Type: ZZ"),
                    errorProcessor.messages());
        }

        @Test
        @DisplayName("ERR-TEXT is cleared before each transaction, so a clean record clears the "
                + "previous error")
        void errTextIsClearedPerTransaction() {
            TransactionRecord bad = TestData.buyTransaction();
            bad.setTrnPortfolioId("");

            PortfolioTransactionProcessor processor = runMain(bad, TestData.buyTransaction());

            assertTrue(processor.getErrorMessage().isErrTextSpaces());
            assertEquals(1, processor.getWsProcessCount());
            assertEquals(1, processor.getWsErrorCount());
        }
    }

    @Nested
    @DisplayName("9000-ERROR-ROUTINE")
    class ErrorRoutine {

        @Test
        @DisplayName("stamps the category and the program and leaves the code and severity unset "
                + "(G6)")
        void categoryAndProgramOnly() {
            runMain(TestData.transactionWithRawType("XX"));

            assertEquals(1, errorProcessor.count());
            ErrorMessage logged = errorProcessor.errors().get(0);
            assertEquals(ErrorCategory.PROCESSING, logged.getErrorCategory());
            assertEquals(
                    CobolText.picX(
                            PortfolioTransactionProcessor.PROGRAM_ID, ErrorMessage.PROGRAM_LENGTH),
                    logged.getErrProgram());
            assertTrue(CobolText.isSpaces(logged.getErrCode()));
            assertEquals(0, logged.getErrSeverity());
        }
    }

    @Nested
    @DisplayName("G2 - the main flow validates and nothing else")
    class DeadCode {

        @Test
        @DisplayName("a valid buy leaves the portfolio file untouched and writes no audit record")
        void validBuyChangesNothing() {
            PortfolioTransactionProcessor processor = runMain(TestData.buyTransaction());

            assertEquals(1, processor.getWsProcessCount());
            assertEquals(0, repository.updateCount());
            assertEquals(0, auditProcessor.count());
            assertEquals(
                    CobolDecimal.quantity("1000.0000"),
                    repository.stored(TestData.GROWTH_PORTFOLIO_ID).get().getPortTotalUnits());
            assertEquals(
                    CobolDecimal.amount("10000000.00"),
                    repository.stored(TestData.GROWTH_PORTFOLIO_ID).get().getPortTotalCost());
        }

        @Test
        @DisplayName("one of every type through the main flow still updates nothing")
        void everyTypeChangesNothing() {
            PortfolioTransactionProcessor processor =
                    runMain(
                            TestData.buyTransaction(),
                            TestData.sellTransaction(),
                            TestData.transferTransaction(),
                            TestData.feeTransaction());

            assertEquals(4, processor.getWsReadCount());
            assertEquals(4, processor.getWsProcessCount());
            assertEquals(0, repository.updateCount());
            assertEquals(0, auditProcessor.count());
        }
    }

    @Nested
    @DisplayName("2200-UPDATE-POSITIONS and its children, called directly")
    class Updates {

        @Test
        @DisplayName("2210 adds the quantity to the units and the amount to the cost (G1)")
        void buyIncreasesUnitsAndCost() {
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());

            processor.processBuy();

            PortfolioRecord stored = repository.stored(TestData.GROWTH_PORTFOLIO_ID).get();
            assertEquals(CobolDecimal.quantity("1100.0000"), stored.getPortTotalUnits());
            assertEquals(CobolDecimal.amount("10012500.00"), stored.getPortTotalCost());
            assertEquals(1, repository.updateCount());
            assertEquals(0, errorProcessor.count());
        }

        @Test
        @DisplayName("the rewrite writes back the whole record area, not just the fields the "
                + "paragraph touched (G9)")
        void rewriteWritesBackTheWholeRecordArea() {
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());

            processor.processBuy();

            PortfolioRecord stored = repository.stored(TestData.GROWTH_PORTFOLIO_ID).get();
            assertEquals(
                    TestData.growthPortfolio().getPortClientName(), stored.getPortClientName());
            assertEquals(TestData.growthPortfolio().getPortAccountNo(), stored.getPortAccountNo());
            assertEquals(TestData.growthPortfolio().getPortStatus(), stored.getPortStatus());
        }

        @Test
        @DisplayName("2220 subtracts the quantity from the units and the amount from the cost")
        void sellDecreasesUnitsAndCost() {
            PortfolioTransactionProcessor processor = processorFor(TestData.sellTransaction());

            processor.processSell();

            PortfolioRecord stored = repository.stored(TestData.GROWTH_PORTFOLIO_ID).get();
            assertEquals(CobolDecimal.quantity("950.0000"), stored.getPortTotalUnits());
            assertEquals(CobolDecimal.amount("9993750.00"), stored.getPortTotalCost());
            assertEquals(0, errorProcessor.count());
        }

        @Test
        @DisplayName("2220 refuses a sale larger than the holding")
        void sellWithInsufficientUnits() {
            TransactionRecord sell = TestData.sellTransaction();
            sell.setTrnQuantity("1000.0001");

            PortfolioTransactionProcessor processor = processorFor(sell);
            processor.processSell();

            assertEquals(
                    PortfolioTransactionProcessor.ERR_INSUFFICIENT_UNITS,
                    errorProcessor.lastMessage());
            assertEquals(0, repository.updateCount());
            assertEquals(
                    CobolDecimal.quantity("1000.0000"),
                    repository.stored(TestData.GROWTH_PORTFOLIO_ID).get().getPortTotalUnits());
        }

        @Test
        @DisplayName("2220 allows a sale of exactly the holding")
        void sellOfTheWholeHolding() {
            TransactionRecord sell = TestData.sellTransaction();
            sell.setTrnQuantity("1000.0000");

            PortfolioTransactionProcessor processor = processorFor(sell);
            processor.processSell();

            assertEquals(
                    CobolDecimal.ZERO_QUANTITY,
                    repository.stored(TestData.GROWTH_PORTFOLIO_ID).get().getPortTotalUnits());
            assertEquals(0, errorProcessor.count());
        }

        @Test
        @DisplayName("2240 subtracts the fee from the cost and leaves the units alone")
        void feeSubtractsFromCostOnly() {
            PortfolioTransactionProcessor processor = processorFor(TestData.feeTransaction());

            processor.processFee();

            PortfolioRecord stored = repository.stored(TestData.GROWTH_PORTFOLIO_ID).get();
            assertEquals(CobolDecimal.amount("9999954.50"), stored.getPortTotalCost());
            assertEquals(CobolDecimal.quantity("1000.0000"), stored.getPortTotalUnits());
            assertEquals(0, errorProcessor.count());
        }

        @Test
        @DisplayName("2230 is unimplemented and says so (G3)")
        void transferIsUnimplemented() {
            PortfolioTransactionProcessor processor = processorFor(TestData.transferTransaction());

            processor.processTransfer();

            assertEquals(
                    PortfolioTransactionProcessor.ERR_TRANSFER_NOT_IMPLEMENTED,
                    errorProcessor.lastMessage());
            assertEquals(1, processor.getWsErrorCount());
            assertEquals(0, repository.updateCount());
        }

        @Test
        @DisplayName("2210 and 2220 report a missing portfolio as 'not found for update'")
        void missingPortfolioOnBuyAndSell() {
            TransactionRecord buy = TestData.buyTransaction();
            buy.setTrnPortfolioId(TestData.MISSING_PORTFOLIO_ID);

            PortfolioTransactionProcessor processor = processorFor(buy);
            processor.processBuy();
            processor.processSell();

            assertEquals(
                    Arrays.asList(
                            PortfolioTransactionProcessor.ERR_PORTFOLIO_NOT_FOUND_FOR_UPDATE,
                            PortfolioTransactionProcessor.ERR_PORTFOLIO_NOT_FOUND_FOR_UPDATE),
                    errorProcessor.messages());
            assertEquals(0, repository.updateCount());
        }

        @Test
        @DisplayName("2240 reports a missing portfolio with its own text")
        void missingPortfolioOnFee() {
            TransactionRecord fee = TestData.feeTransaction();
            fee.setTrnPortfolioId(TestData.MISSING_PORTFOLIO_ID);

            PortfolioTransactionProcessor processor = processorFor(fee);
            processor.processFee();

            assertEquals(
                    PortfolioTransactionProcessor.ERR_PORTFOLIO_NOT_FOUND_FOR_FEE,
                    errorProcessor.lastMessage());
        }

        @Test
        @DisplayName("a rewrite that takes the INVALID KEY branch reports an update error")
        void rewriteFailure() {
            repository.rewriteStatus(PortfolioRepository.STATUS_NOT_FOUND);
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());

            processor.processBuy();

            assertEquals(
                    PortfolioTransactionProcessor.ERR_UPDATING_PORTFOLIO,
                    errorProcessor.lastMessage());
            assertEquals(
                    CobolDecimal.quantity("1000.0000"),
                    repository.stored(TestData.GROWTH_PORTFOLIO_ID).get().getPortTotalUnits());
        }

        @Test
        @DisplayName("2200 dispatches on the type and always updates the audit trail")
        void dispatchesAndAudits() {
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());

            processor.updatePositions();

            assertEquals(1, repository.updateCount());
            assertEquals(1, auditProcessor.count());
        }

        @Test
        @DisplayName("2200 has no WHEN OTHER, so an unrecognised type updates nothing and is "
                + "audited with a blank action (G11)")
        void unrecognisedTypeStillAudits() {
            PortfolioTransactionProcessor processor =
                    processorFor(TestData.transactionWithRawType("XX"));

            processor.updatePositions();

            assertEquals(0, repository.updateCount());
            assertEquals(1, auditProcessor.count());
            assertTrue(CobolText.isSpaces(auditProcessor.last().getAudAction()));
        }
    }

    @Nested
    @DisplayName("2300-UPDATE-AUDIT-TRAIL and 2310-WRITE-AUDIT-RECORD")
    class AuditTrail {

        @Test
        @DisplayName("a completed rewrite is audited as SUCC (G7)")
        void successfulUpdateIsAuditedSucc() {
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());

            processor.updatePositions();

            AuditRecord audit = auditProcessor.last();
            assertNotNull(audit);
            assertEquals(
                    CobolText.picX(CURRENT_DATE, AuditRecord.TIMESTAMP_LENGTH),
                    audit.getAudTimestamp());
            assertEquals(
                    CobolText.picX(
                            PortfolioTransactionProcessor.PROGRAM_ID, AuditRecord.PROGRAM_LENGTH),
                    audit.getAudProgram());
            assertEquals(
                    CobolText.picX(TestData.USER_ID, AuditRecord.USER_ID_LENGTH),
                    audit.getAudUserId());
            assertEquals(AuditType.TRANSACTION, audit.getAuditType());
            assertEquals("CREATE  ", audit.getAudAction());
            assertEquals(AuditStatus.SUCCESS, audit.getAuditStatus());
            assertEquals(TestData.GROWTH_PORTFOLIO_ID, audit.getAudPortfolioId());
            assertEquals(
                    TestData.growthPortfolio().getPortAccountNo(), audit.getAudAccountNo());
        }

        @Test
        @DisplayName("a sell is audited as DELETE and a fee as UPDATE")
        void actionPerType() {
            PortfolioTransactionProcessor processor = processorFor(TestData.sellTransaction());
            processor.updatePositions();
            assertEquals("DELETE  ", auditProcessor.last().getAudAction());

            processor.setTransactionRecord(TestData.feeTransaction());
            processor.updatePositions();
            assertEquals("UPDATE  ", auditProcessor.last().getAudAction());
        }

        @Test
        @DisplayName("a failed file status is audited as FAIL (G7)")
        void failedUpdateIsAuditedFail() {
            repository.rewriteStatus(PortfolioRepository.STATUS_NOT_FOUND);
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());

            processor.updatePositions();

            assertEquals(AuditStatus.FAILURE, auditProcessor.last().getAuditStatus());
            assertEquals(
                    PortfolioTransactionProcessor.ERR_UPDATING_PORTFOLIO,
                    errorProcessor.messages().get(0));
        }

        @Test
        @DisplayName("the transfer path does no I/O, so the audit status is whatever the previous "
                + "operation left (G7)")
        void transferInheritsTheStaleFileStatus() {
            PortfolioTransactionProcessor processor = processorFor(TestData.transferTransaction());
            processor.setTransactionRecord(TestData.buyTransaction());
            processor.processBuy();
            assertEquals(PortfolioRepository.STATUS_SUCCESS, processor.getWsPortStatus());

            TransactionRecord missingPortfolio = TestData.buyTransaction();
            missingPortfolio.setTrnPortfolioId(TestData.MISSING_PORTFOLIO_ID);
            processor.setTransactionRecord(missingPortfolio);
            processor.processBuy();

            processor.setTransactionRecord(TestData.transferTransaction());
            processor.updatePositions();

            assertEquals(AuditStatus.FAILURE, auditProcessor.last().getAuditStatus());
        }

        @Test
        @DisplayName("AUD-MESSAGE strings the packed fields as sign plus unscaled digits (G8)")
        void auditMessageRendering() {
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());

            processor.updateAuditTrail();

            assertEquals(
                    "Transaction: BU Amount: +000000001250000 Units: +000000001000000",
                    auditProcessor.last().getAudMessageTrimmed());
            assertEquals(
                    AuditRecord.MESSAGE_LENGTH, auditProcessor.last().getAudMessage().length());
        }

        @Test
        @DisplayName("AUD-BEFORE-IMAGE is the record area truncated to its hundred bytes")
        void beforeImageIsTruncated() {
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());

            processor.updatePositions();

            String image = auditProcessor.last().getAudBeforeImage();
            assertEquals(AuditRecord.IMAGE_LENGTH, image.length());
            assertEquals(
                    CobolText.picX(
                            TestData.growthPortfolio().toRecordImage(), AuditRecord.IMAGE_LENGTH),
                    image);
        }

        @Test
        @DisplayName("the 'before' image is taken after the update, and is unchanged by it only "
                + "because the updated fields are the synthetic ones (G1, G12)")
        void beforeImageIsTakenAfterTheUpdate() {
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());
            processor.updatePositions();
            String afterBuy = auditProcessor.last().getAudBeforeImage();

            assertEquals(
                    CobolDecimal.quantity("1100.0000"),
                    repository.stored(TestData.GROWTH_PORTFOLIO_ID).get().getPortTotalUnits());
            assertEquals(
                    CobolText.picX(
                            TestData.growthPortfolio().toRecordImage(), AuditRecord.IMAGE_LENGTH),
                    afterBuy);
        }

        @Test
        @DisplayName("2310 reports a failed write")
        void auditWriteFailure() {
            auditProcessor.failing();
            PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());

            processor.updateAuditTrail();

            assertEquals(1, auditProcessor.count());
            assertEquals(
                    PortfolioTransactionProcessor.ERR_WRITING_AUDIT_RECORD,
                    errorProcessor.lastMessage());
            assertEquals(1, processor.getWsErrorCount());
        }
    }

    @Nested
    @DisplayName("0000-MAIN, 1000-INITIALIZE and 3000-TERMINATE")
    class ControlFlow {

        @Test
        @DisplayName("the loop stops once WS-ERROR-COUNT passes 100, which the test sees before "
                + "the next read (G14)")
        void errorCutoff() {
            PortfolioTransactionProcessor processor = processorOver(failingTransactions(200));

            processor.main();

            assertEquals(101, processor.getWsReadCount());
            assertEquals(101, processor.getWsErrorCount());
            assertEquals(0, processor.getWsProcessCount());
            assertFalse(processor.isEndOfFile());
        }

        @Test
        @DisplayName("exactly 100 errors do not stop the loop")
        void oneHundredErrorsIsUnderTheLimit() {
            List<TransactionRecord> transactions = new ArrayList<>(failing(100));
            transactions.add(TestData.buyTransaction());
            PortfolioTransactionProcessor processor =
                    processorOver(new ListTransactionSource(transactions));

            processor.main();

            assertTrue(processor.isEndOfFile());
            assertEquals(101, processor.getWsReadCount());
            assertEquals(100, processor.getWsErrorCount());
            assertEquals(1, processor.getWsProcessCount());
        }

        @Test
        @DisplayName("a transaction file that will not open stops the program before the loop")
        void transactionFileOpenFailure() {
            ListTransactionSource source =
                    new ListTransactionSource(TestData.buyTransaction()).failingOpen("35");
            PortfolioTransactionProcessor processor = processorOver(source);

            processor.main();

            assertEquals(
                    PortfolioTransactionProcessor.ERR_OPENING_TRANSACTION_FILE,
                    errorProcessor.messages().get(0));
            assertEquals(0, source.readCount());
            assertEquals(0, processor.getWsReadCount());
            assertEquals(1, processor.getWsErrorCount());
        }

        @Test
        @DisplayName("a portfolio file that will not open is logged but does not stop the loop "
                + "(G13)")
        void portfolioFileOpenFailure() {
            repository.failingOpen("39");
            PortfolioTransactionProcessor processor = processorOver(
                    new ListTransactionSource(TestData.buyTransaction()));

            processor.main();

            assertEquals(
                    PortfolioTransactionProcessor.ERR_OPENING_PORTFOLIO_FILE,
                    errorProcessor.messages().get(0));
            assertEquals(1, processor.getWsReadCount());
            assertEquals(1, processor.getWsProcessCount());
        }

        @Test
        @DisplayName("3000-TERMINATE closes both files and displays the three counters (G13)")
        void terminationReport() {
            ListTransactionSource source =
                    new ListTransactionSource(
                            TestData.buyTransaction(), TestData.transactionWithRawType("XX"));
            PortfolioTransactionProcessor processor = processorOver(source);

            processor.main();

            assertEquals(1, source.closeCount());
            assertEquals(1, repository.closeCount());
            assertEquals(
                    Arrays.asList(
                            "Transactions Read:    00000002",
                            "Transactions Process: 00000001",
                            "Errors Encountered:   00000001"),
                    processor.getDisplayLines());
        }

        @Test
        @DisplayName("the files are closed even when the transaction file never opened (G13)")
        void filesAreClosedAfterAFailedOpen() {
            ListTransactionSource source = new ListTransactionSource().failingOpen("35");
            PortfolioTransactionProcessor processor = processorOver(source);

            processor.main();

            assertEquals(1, source.closeCount());
            assertEquals(1, repository.closeCount());
        }

        @Test
        @DisplayName("an empty transaction file reads nothing and reports zeros")
        void emptyTransactionFile() {
            PortfolioTransactionProcessor processor = processorOver(new ListTransactionSource());

            processor.main();

            assertTrue(processor.isEndOfFile());
            assertEquals(0, processor.getWsReadCount());
            assertEquals(
                    "Transactions Read:    00000000", processor.getDisplayLines().get(0));
        }

        @Test
        @DisplayName("FUNCTION USER-ID has no IBM equivalent, so the user id is supplied and "
                + "stored in eight bytes (G10)")
        void userIdIsSupplied() {
            PortfolioTransactionProcessor processor =
                    new PortfolioTransactionProcessor(
                            repository,
                            new ListTransactionSource(),
                            auditProcessor,
                            errorProcessor,
                            FIXED_CLOCK,
                            "VERYLONGUSERNAME");
            processor.setTransactionRecord(TestData.buyTransaction());

            processor.updateAuditTrail();

            assertEquals("VERYLONG", auditProcessor.last().getAudUserId());
        }
    }

    private ListTransactionSource failingTransactions(int count) {
        return new ListTransactionSource(failing(count));
    }

    /** {@code count} transactions that all fail {@code 2110-CHECK-PORTFOLIO}. */
    private List<TransactionRecord> failing(int count) {
        List<TransactionRecord> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TransactionRecord transaction =
                    TestData.transaction(
                            TransactionType.BUY, "100.0000", "125.0000", "12500.00");
            transaction.setTrnPortfolioId(TestData.MISSING_PORTFOLIO_ID);
            transaction.setTrnSequenceNo(String.format("%06d", i));
            transactions.add(transaction);
        }
        return transactions;
    }

    @Test
    @DisplayName("no field in the translation is a binary floating point number")
    void decimalsAreBigDecimal() {
        PortfolioTransactionProcessor processor = processorFor(TestData.buyTransaction());
        processor.processBuy();

        BigDecimal units = repository.stored(TestData.GROWTH_PORTFOLIO_ID).get().getPortTotalUnits();
        assertEquals(CobolDecimal.QUANTITY_SCALE, units.scale());
        assertEquals(
                CobolDecimal.AMOUNT_SCALE,
                repository.stored(TestData.GROWTH_PORTFOLIO_ID).get().getPortTotalCost().scale());
    }
}
