package com.clbs.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.clbs.portfolio.domain.PortfolioPosition;
import com.clbs.portfolio.domain.PortfolioTransaction;
import com.clbs.portfolio.repository.PortfolioPositionRepository;
import com.clbs.portfolio.service.AuditRecord;
import com.clbs.portfolio.service.AuditService;
import com.clbs.portfolio.service.BatchResult;
import com.clbs.portfolio.service.PortfolioTransactionService;
import com.clbs.portfolio.service.TransactionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Behavioral-equivalence suite for the modernized PORTTRAN service.
 *
 * <p>Each test names the COBOL paragraph and rule it verifies, with the expected
 * output derived directly from {@code src/programs/portfolio/PORTTRAN.cbl} so that
 * a reviewer can confirm parity without running the mainframe program.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class PortfolioTransactionServiceTest {

    @Autowired
    private PortfolioTransactionService service;

    @Autowired
    private PortfolioPositionRepository repository;

    @MockBean
    private AuditService auditService;

    private static PortfolioTransaction.PortfolioTransactionBuilder tx() {
        return PortfolioTransaction.builder()
                .transactionDate("20240320")
                .transactionTime("101500")
                .sequenceNo("000001")
                .investmentId("STK0000001")
                .currency("USD")
                .status("P");
    }

    // ---------------------------------------------------------------- seed --

    @Test
    @DisplayName("Seed data: V2 migration loads 3 baseline positions")
    void seedDataLoaded() {
        assertThat(repository.count()).isEqualTo(3);
        PortfolioPosition p1 = repository.findById("PORT0001").orElseThrow();
        assertThat(p1.getTotalUnits()).isEqualByComparingTo("100.0000");
        assertThat(p1.getTotalCost()).isEqualByComparingTo("25000.00");
    }

    // -------------------------------------------------- 2110 CHECK-PORTFOLIO --

    @Test
    @DisplayName("2110: blank portfolio id -> 'Portfolio ID is required'")
    void blankPortfolioRejected() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("        ").type("BU")
                .quantity(bd("1")).price(bd("1")).amount(bd("1")).build());
        assertThat(r.success()).isFalse();
        assertThat(r.errorText()).isEqualTo("Portfolio ID is required");
    }

    @Test
    @DisplayName("2110: unknown portfolio id -> 'Invalid Portfolio ID: ...'")
    void unknownPortfolioRejected() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT9999").type("BU")
                .quantity(bd("1")).price(bd("1")).amount(bd("1")).build());
        assertThat(r.success()).isFalse();
        assertThat(r.errorText()).startsWith("Invalid Portfolio ID:");
    }

    // --------------------------------------------- 2120 CHECK-TRANSACTION-TYPE --

    @Test
    @DisplayName("2120: unrecognized type -> 'Invalid Transaction Type: ...'")
    void invalidTypeRejected() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("ZZ")
                .quantity(bd("1")).price(bd("1")).amount(bd("1")).build());
        assertThat(r.success()).isFalse();
        assertThat(r.errorText()).isEqualTo("Invalid Transaction Type: ZZ");
    }

    @Test
    @DisplayName("2100: portfolio is validated before transaction type (rule order)")
    void portfolioCheckedBeforeType() {
        // Both portfolio (unknown) and type (invalid) are wrong; COBOL reports portfolio first.
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT9999").type("ZZ")
                .quantity(bd("1")).price(bd("1")).amount(bd("1")).build());
        assertThat(r.errorText()).startsWith("Invalid Portfolio ID:");
    }

    // ------------------------------------------------------ 2130 CHECK-AMOUNTS --

    @Test
    @DisplayName("2130: quantity <= 0 -> 'Quantity must be greater than zero'")
    void nonPositiveQuantityRejected() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("BU")
                .quantity(bd("0")).price(bd("1")).amount(bd("1")).build());
        assertThat(r.errorText()).isEqualTo("Quantity must be greater than zero");
    }

    @Test
    @DisplayName("2130: price <= 0 (non-transfer) -> 'Price must be greater than zero'")
    void nonPositivePriceRejected() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("BU")
                .quantity(bd("1")).price(bd("0")).amount(bd("1")).build());
        assertThat(r.errorText()).isEqualTo("Price must be greater than zero");
    }

    @Test
    @DisplayName("2130: amount <= 0 (non-transfer) -> 'Amount must be greater than zero'")
    void nonPositiveAmountRejected() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("BU")
                .quantity(bd("1")).price(bd("1")).amount(bd("0")).build());
        assertThat(r.errorText()).isEqualTo("Amount must be greater than zero");
    }

    // ----------------------------------------------------------- 2210 BUY ----

    @Test
    @DisplayName("2210 BUY: adds units and cost at COBOL scales")
    void buyAddsUnitsAndCost() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("BU")
                .quantity(bd("10.0000")).price(bd("250.0000")).amount(bd("2500.00")).build());

        assertThat(r.success()).isTrue();
        PortfolioPosition p = repository.findById("PORT0001").orElseThrow();
        assertThat(p.getTotalUnits()).isEqualByComparingTo("110.0000"); // 100 + 10
        assertThat(p.getTotalCost()).isEqualByComparingTo("27500.00");  // 25000 + 2500
        assertThat(p.getTotalUnits().scale()).isEqualTo(4);
        assertThat(p.getTotalCost().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Decimal parity: quantity beyond 4 dp is truncated DOWN (COBOL has no ROUNDED)")
    void quantityTruncatedNotRounded() {
        // 10.00009 would round to 10.0001 but COBOL truncates to 10.0000.
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0002").type("BU")
                .quantity(bd("10.00009")).price(bd("1.0000")).amount(bd("10.00")).build());

        assertThat(r.success()).isTrue();
        PortfolioPosition p = repository.findById("PORT0002").orElseThrow();
        assertThat(p.getTotalUnits()).isEqualByComparingTo("10.0000");
    }

    // ---------------------------------------------------------- 2220 SELL ----

    @Test
    @DisplayName("2220 SELL: subtracts units and cost")
    void sellSubtractsUnitsAndCost() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("SL")
                .quantity(bd("40.0000")).price(bd("260.0000")).amount(bd("10400.00")).build());

        assertThat(r.success()).isTrue();
        PortfolioPosition p = repository.findById("PORT0001").orElseThrow();
        assertThat(p.getTotalUnits()).isEqualByComparingTo("60.0000");  // 100 - 40
        assertThat(p.getTotalCost()).isEqualByComparingTo("14600.00");  // 25000 - 10400
    }

    @Test
    @DisplayName("2220 SELL boundary: selling exactly the held units is allowed (< not <=)")
    void sellAllUnitsAllowed() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("SL")
                .quantity(bd("100.0000")).price(bd("250.0000")).amount(bd("25000.00")).build());

        assertThat(r.success()).isTrue();
        PortfolioPosition p = repository.findById("PORT0001").orElseThrow();
        assertThat(p.getTotalUnits()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("2220 SELL: insufficient units -> failure and no mutation")
    void sellInsufficientUnitsRejected() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("SL")
                .quantity(bd("101.0000")).price(bd("250.0000")).amount(bd("25250.00")).build());

        assertThat(r.success()).isFalse();
        assertThat(r.errorText()).isEqualTo("Insufficient units for sale");
        PortfolioPosition p = repository.findById("PORT0001").orElseThrow();
        assertThat(p.getTotalUnits()).isEqualByComparingTo("100.0000"); // unchanged
        assertThat(p.getTotalCost()).isEqualByComparingTo("25000.00");  // unchanged
    }

    // ----------------------------------------------------------- 2240 FEE ----

    @Test
    @DisplayName("2240 FEE: subtracts amount from cost only, units unchanged")
    void feeSubtractsCostOnly() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("FE")
                .quantity(bd("1.0000")).price(bd("1.0000")).amount(bd("125.00")).build());

        assertThat(r.success()).isTrue();
        PortfolioPosition p = repository.findById("PORT0001").orElseThrow();
        assertThat(p.getTotalUnits()).isEqualByComparingTo("100.0000"); // unchanged
        assertThat(p.getTotalCost()).isEqualByComparingTo("24875.00");  // 25000 - 125
    }

    // ------------------------------------------------------ 2230 TRANSFER ----

    @Test
    @DisplayName("2230 TRANSFER: returns 'Transfer processing not implemented'")
    void transferNotImplemented() {
        TransactionResult r = service.processTransaction(tx()
                .portfolioId("PORT0001").type("TR")
                .quantity(bd("1.0000")).price(bd("1.0000")).amount(bd("1.00")).build());
        assertThat(r.success()).isFalse();
        assertThat(r.errorText()).isEqualTo("Transfer processing not implemented");
    }

    @Test
    @DisplayName("2130 + 2230 TRANSFER: zero price/amount skip the positivity checks for type TR")
    void transferSkipsPriceAndAmountChecks() {
        // For TR, price<=0 and amount<=0 are allowed; validation passes and the
        // 'not implemented' error comes from the update step, not validation.
        String validationError = service.validateTransaction(tx()
                .portfolioId("PORT0001").type("TR")
                .quantity(bd("1.0000")).price(bd("0")).amount(bd("0")).build());
        assertThat(validationError).isNull();
    }

    // ------------------------------------------- 0000-MAIN batch circuit breaker --

    @Test
    @DisplayName("0000-MAIN: batch stops after error count exceeds 100")
    void batchHaltsAfterErrorThreshold() {
        List<PortfolioTransaction> txns = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            // Every record is invalid (unknown portfolio) -> guaranteed error.
            txns.add(tx().portfolioId("PORT9999").sequenceNo(String.format("%06d", i))
                    .type("BU").quantity(bd("1")).price(bd("1")).amount(bd("1")).build());
        }
        BatchResult result = service.processBatch(txns);

        assertThat(result.halted()).isTrue();
        assertThat(result.errors()).isEqualTo(101); // breaker trips once errors > 100
        assertThat(result.read()).isEqualTo(101);
        assertThat(result.processed()).isZero();
    }

    @Test
    @DisplayName("0000-MAIN: batch tallies read/processed/errors for a mixed file")
    void batchCountsMixedFile() {
        List<PortfolioTransaction> txns = List.of(
                tx().portfolioId("PORT0001").sequenceNo("000001").type("BU")
                        .quantity(bd("1.0000")).price(bd("1.0000")).amount(bd("1.00")).build(),
                tx().portfolioId("PORT9999").sequenceNo("000002").type("BU")
                        .quantity(bd("1.0000")).price(bd("1.0000")).amount(bd("1.00")).build(),
                tx().portfolioId("PORT0001").sequenceNo("000003").type("FE")
                        .quantity(bd("1.0000")).price(bd("1.0000")).amount(bd("5.00")).build());

        BatchResult result = service.processBatch(txns);
        assertThat(result.read()).isEqualTo(3);
        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.errors()).isEqualTo(1);
        assertThat(result.halted()).isFalse();
    }

    // ------------------------------------------------------- 2300 audit trail --

    @Test
    @DisplayName("2300: a processed BUY writes an audit record with action CREATE")
    void buyWritesAuditTrail() {
        service.processTransaction(tx()
                .portfolioId("PORT0001").type("BU")
                .quantity(bd("1.0000")).price(bd("1.0000")).amount(bd("1.00")).build());

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditService, atLeastOnce()).record(captor.capture());
        AuditRecord rec = captor.getValue();
        assertThat(rec.program()).isEqualTo("PORTTRAN");
        assertThat(rec.action()).isEqualTo("CREATE  ");
        assertThat(rec.status()).isEqualTo("SUCC");
    }

    @Test
    @DisplayName("2100: validation failures never reach the audit trail")
    void validationFailureSkipsAudit() {
        service.processTransaction(tx()
                .portfolioId("PORT9999").type("BU")
                .quantity(bd("1")).price(bd("1")).amount(bd("1")).build());
        verify(auditService, never()).record(any());
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
