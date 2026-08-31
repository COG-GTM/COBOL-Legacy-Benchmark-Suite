package com.clbs.posval.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.posval.audit.AuditRecord;
import com.clbs.posval.audit.AuditTrailWriter;
import com.clbs.posval.domain.PortfolioPosition;
import com.clbs.posval.domain.TransactionRecord;
import com.clbs.posval.repository.InMemoryPortfolioPositionStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Rules R-8.x of the spec: {@code PORTTRAN 2200-UPDATE-POSITIONS} and its subordinate paragraphs. */
class PositionUpdateServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2024-03-20T15:30:45Z"), ZoneOffset.UTC);

    private final InMemoryPortfolioPositionStore store = new InMemoryPortfolioPositionStore();
    private final AuditTrailWriter audit = new AuditTrailWriter();
    private final PositionUpdateService service = new PositionUpdateService(store, audit, FIXED);

    @BeforeEach
    void loadPortfolio() {
        store.clear();
        audit.clear();
        store.load(PortfolioPosition.of("PORT0001", new BigDecimal("100.0000"), new BigDecimal("12500.00")));
    }

    private PortfolioPosition current() {
        return store.read("PORT0001").orElseThrow();
    }

    @Test
    @DisplayName("R-8.1: a buy adds quantity to units and amount to cost")
    void buyAccumulatesUnitsAndCost() {
        assertThat(service.apply(TransactionRecord.of(
                        "PORT0001", "BU", new BigDecimal("10.0000"), new BigDecimal("125.00"),
                        new BigDecimal("1250.00"))))
                .isEmpty();

        assertThat(current().totalUnits()).isEqualByComparingTo("110.0000");
        assertThat(current().totalCost()).isEqualByComparingTo("13750.00");
    }

    @Test
    @DisplayName("R-8.2: a sell subtracts the proceeds from cost basis, not the cost of the units sold")
    void sellSubtractsProceedsFromCostBasis() {
        assertThat(service.apply(TransactionRecord.of(
                        "PORT0001", "SL", new BigDecimal("10.0000"), new BigDecimal("200.00"),
                        new BigDecimal("2000.00"))))
                .isEmpty();

        assertThat(current().totalUnits()).isEqualByComparingTo("90.0000");
        assertThat(current().totalCost()).isEqualByComparingTo("10500.00");
    }

    @Test
    @DisplayName("R-8.3: a sell of more units than held is refused and changes nothing")
    void sellIsGuardedByUnitsHeld() {
        assertThat(service.apply(TransactionRecord.of(
                        "PORT0001", "SL", new BigDecimal("100.0001"), new BigDecimal("200.00"),
                        new BigDecimal("20000.00"))))
                .contains(PositionUpdateService.ERR_INSUFFICIENT_UNITS);

        assertThat(current().totalUnits()).isEqualByComparingTo("100.0000");
        assertThat(current().totalCost()).isEqualByComparingTo("12500.00");
    }

    @Test
    @DisplayName("R-8.4: nothing guards the cost basis, so a sell can drive it negative")
    void sellCanDriveCostBasisNegative() {
        assertThat(service.apply(TransactionRecord.of(
                        "PORT0001", "SL", new BigDecimal("1.0000"), new BigDecimal("20000.00"),
                        new BigDecimal("20000.00"))))
                .isEmpty();

        assertThat(current().totalUnits()).isEqualByComparingTo("99.0000");
        assertThat(current().totalCost()).isEqualByComparingTo("-7500.00");
    }

    @Test
    @DisplayName("R-8.5: a fee reduces cost basis and leaves units untouched")
    void feeReducesCostOnly() {
        assertThat(service.apply(TransactionRecord.of(
                        "PORT0001", "FE", new BigDecimal("1.0000"), new BigDecimal("9.99"),
                        new BigDecimal("9.99"))))
                .isEmpty();

        assertThat(current().totalUnits()).isEqualByComparingTo("100.0000");
        assertThat(current().totalCost()).isEqualByComparingTo("12490.01");
    }

    @Test
    @DisplayName("R-8.6: a transfer always fails and changes nothing")
    void transferIsNotImplemented() {
        assertThat(service.apply(TransactionRecord.of(
                        "PORT0001", "TR", new BigDecimal("10.0000"), new BigDecimal("125.00"),
                        new BigDecimal("1250.00"))))
                .contains(PositionUpdateService.ERR_TRANSFER_NOT_IMPLEMENTED);

        assertThat(current().totalCost()).isEqualByComparingTo("12500.00");
    }

    @Test
    @DisplayName("R-8.7: a missing portfolio record fails the update with a type specific message")
    void missingPortfolioIsReported() {
        assertThat(service.apply(TransactionRecord.of(
                        "PORT9999", "BU", new BigDecimal("1.0000"), new BigDecimal("1.00"),
                        new BigDecimal("1.00"))))
                .contains(PositionUpdateService.ERR_PORTFOLIO_NOT_FOUND);
        assertThat(service.apply(TransactionRecord.of(
                        "PORT9999", "FE", new BigDecimal("1.0000"), new BigDecimal("1.00"),
                        new BigDecimal("1.00"))))
                .contains(PositionUpdateService.ERR_PORTFOLIO_NOT_FOUND_FEE);
    }

    @Test
    @DisplayName("R-8.8: cost overflow wraps silently because no ADD carries ON SIZE ERROR")
    void costOverflowWrapsSilently() {
        store.load(PortfolioPosition.of(
                "PORT0002", new BigDecimal("1.0000"), new BigDecimal("9999999999999.99")));

        assertThat(service.apply(TransactionRecord.of(
                        "PORT0002", "BU", new BigDecimal("1.0000"), new BigDecimal("0.01"),
                        new BigDecimal("0.01"))))
                .isEmpty();

        assertThat(store.read("PORT0002").orElseThrow().totalCost()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("R-9.1: audit actions follow the COBOL mapping, buy CREATE and sell DELETE")
    void auditActionMapping() {
        service.apply(TransactionRecord.of("PORT0001", "BU", new BigDecimal("1.0000"),
                new BigDecimal("1.00"), new BigDecimal("1.00")));
        service.apply(TransactionRecord.of("PORT0001", "SL", new BigDecimal("1.0000"),
                new BigDecimal("1.00"), new BigDecimal("1.00")));
        service.apply(TransactionRecord.of("PORT0001", "FE", new BigDecimal("1.0000"),
                new BigDecimal("1.00"), new BigDecimal("1.00")));

        assertThat(audit.records()).extracting(AuditRecord::action)
                .containsExactly(AuditRecord.ACTION_CREATE, AuditRecord.ACTION_DELETE, AuditRecord.ACTION_UPDATE);
    }

    @Test
    @DisplayName("R-9.2: a failed transfer is audited as SUCC because AUD-STATUS follows the file status")
    void failedTransferIsAuditedAsSuccess() {
        service.apply(TransactionRecord.of("PORT0001", "TR", new BigDecimal("1.0000"),
                new BigDecimal("1.00"), new BigDecimal("1.00")));

        assertThat(audit.records()).singleElement()
                .extracting(AuditRecord::status).isEqualTo(AuditRecord.STATUS_SUCCESS);
    }

    @Test
    @DisplayName("R-9.3: an unknown transaction type updates nothing, raises nothing and still audits")
    void unknownTypeStillWritesAnAuditRecordWithABlankAction() {
        assertThat(service.apply(TransactionRecord.of("PORT0001", "ZZ", new BigDecimal("1.0000"),
                        new BigDecimal("1.00"), new BigDecimal("1.00"))))
                .isEmpty();

        assertThat(current().totalCost()).isEqualByComparingTo("12500.00");
        assertThat(audit.records()).singleElement()
                .extracting(AuditRecord::action).isEqualTo("        ");
    }
}
