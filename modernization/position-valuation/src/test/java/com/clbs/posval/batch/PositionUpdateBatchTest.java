package com.clbs.posval.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.posval.audit.AuditTrailWriter;
import com.clbs.posval.domain.PortfolioPosition;
import com.clbs.posval.domain.TransactionRecord;
import com.clbs.posval.error.ErrorProcessor;
import com.clbs.posval.repository.InMemoryPortfolioPositionStore;
import com.clbs.posval.service.PositionUpdateService;
import com.clbs.posval.service.TransactionValidationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Rules R-10.x of the spec: the {@code PORTTRAN} driver loop and its error limit. */
class PositionUpdateBatchTest {

    private static final BigDecimal QTY = new BigDecimal("1.0000");
    private static final BigDecimal PRICE = new BigDecimal("125.00");
    private static final BigDecimal AMOUNT = new BigDecimal("125.00");

    private final InMemoryPortfolioPositionStore store = new InMemoryPortfolioPositionStore();
    private final AuditTrailWriter audit = new AuditTrailWriter();
    private final ErrorProcessor errors = new ErrorProcessor();
    private final TransactionValidationService validation = new TransactionValidationService(store);
    private final PositionUpdateService updates = new PositionUpdateService(
            store, audit, Clock.fixed(Instant.parse("2024-03-20T15:30:45Z"), ZoneOffset.UTC));

    @BeforeEach
    void loadPortfolio() {
        store.clear();
        errors.clear();
        audit.clear();
        store.load(PortfolioPosition.of("PORT0001", new BigDecimal("100.0000"), new BigDecimal("12500.00")));
    }

    private PositionUpdateBatch batch(boolean applyUpdates) {
        return new PositionUpdateBatch(validation, updates, errors, applyUpdates);
    }

    @Test
    @DisplayName("R-10.1: as written, PORTTRAN validates and counts but never changes a balance")
    void defaultRunIsValidateOnly() {
        PositionUpdateBatch.BatchResult result = batch(false).run(List.of(
                TransactionRecord.of("PORT0001", "BU", QTY, PRICE, AMOUNT),
                TransactionRecord.of("PORT0001", "BU", QTY, PRICE, AMOUNT)));

        assertThat(result.read()).isEqualTo(2);
        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.errors()).isZero();
        assertThat(store.read("PORT0001").orElseThrow().totalCost()).isEqualByComparingTo("12500.00");
        assertThat(audit.records()).isEmpty();
    }

    @Test
    @DisplayName("R-10.2: with updates enabled the same file moves the balance and writes audit records")
    void enablingUpdatesAppliesTheDeadParagraphs() {
        PositionUpdateBatch.BatchResult result = batch(true).run(List.of(
                TransactionRecord.of("PORT0001", "BU", QTY, PRICE, AMOUNT),
                TransactionRecord.of("PORT0001", "BU", QTY, PRICE, AMOUNT)));

        assertThat(result.processed()).isEqualTo(2);
        assertThat(store.read("PORT0001").orElseThrow().totalCost()).isEqualByComparingTo("12750.00");
        assertThat(audit.records()).hasSize(2);
    }

    @Test
    @DisplayName("R-10.3: an invalid transaction is counted as an error and logged to ERRPROC")
    void invalidTransactionIsCountedAndLogged() {
        PositionUpdateBatch.BatchResult result = batch(false).run(List.of(
                TransactionRecord.of("PORT9999", "BU", QTY, PRICE, AMOUNT),
                TransactionRecord.of("PORT0001", "BU", QTY, PRICE, AMOUNT)));

        assertThat(result.read()).isEqualTo(2);
        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.errors()).isEqualTo(1);
        assertThat(errors.entries()).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.programName()).isEqualTo("PORTTRAN");
                    assertThat(entry.category()).isEqualTo("PR");
                    assertThat(entry.text()).isEqualTo("Invalid Portfolio ID: PORT9999");
                });
    }

    @Test
    @DisplayName("R-10.4: the run stops once the error count exceeds 100, leaving the file unread")
    void runStopsAfterOneHundredAndOneErrors() {
        List<TransactionRecord> bad = IntStream.range(0, 150)
                .mapToObj(i -> TransactionRecord.of("PORT9999", "BU", QTY, PRICE, AMOUNT))
                .toList();

        PositionUpdateBatch.BatchResult result = batch(false).run(bad);

        assertThat(result.errors()).isEqualTo(PositionUpdateBatch.ERROR_LIMIT + 1);
        assertThat(result.read()).isEqualTo(PositionUpdateBatch.ERROR_LIMIT + 1);
        assertThat(result.haltedOnErrorLimit()).isTrue();
    }

    @Test
    @DisplayName("R-10.5: exactly 100 errors do not stop the run")
    void oneHundredErrorsDoNotStopTheRun() {
        List<TransactionRecord> transactions = IntStream.range(0, 100)
                .mapToObj(i -> TransactionRecord.of("PORT9999", "BU", QTY, PRICE, AMOUNT))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        transactions.add(TransactionRecord.of("PORT0001", "BU", QTY, PRICE, AMOUNT));

        PositionUpdateBatch.BatchResult result = batch(false).run(transactions);

        assertThat(result.read()).isEqualTo(101);
        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.haltedOnErrorLimit()).isFalse();
    }
}
