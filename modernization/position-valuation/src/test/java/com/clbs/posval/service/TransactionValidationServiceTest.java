package com.clbs.posval.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.posval.domain.PortfolioPosition;
import com.clbs.posval.domain.TransactionRecord;
import com.clbs.posval.repository.InMemoryPortfolioPositionStore;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Rules R-7.x of the spec: {@code PORTTRAN 2100-VALIDATE-TRANSACTION} and its three checks. */
class TransactionValidationServiceTest {

    private static final BigDecimal TEN = new BigDecimal("10.0000");
    private static final BigDecimal PRICE = new BigDecimal("125.0000");
    private static final BigDecimal AMOUNT = new BigDecimal("1250.00");

    private final InMemoryPortfolioPositionStore store = new InMemoryPortfolioPositionStore();
    private final TransactionValidationService service = new TransactionValidationService(store);

    @BeforeEach
    void loadPortfolio() {
        store.clear();
        store.load(PortfolioPosition.of("PORT0001", new BigDecimal("100.0000"), new BigDecimal("12500.00")));
    }

    @Test
    @DisplayName("R-7.0: a well formed buy passes every check")
    void validBuyPasses() {
        assertThat(service.validate(TransactionRecord.of("PORT0001", "BU", TEN, PRICE, AMOUNT)))
                .isEmpty();
    }

    @Test
    @DisplayName("R-7.1: a blank portfolio id is rejected before the file is read")
    void blankPortfolioIdIsRejected() {
        assertThat(service.validate(TransactionRecord.of("        ", "BU", TEN, PRICE, AMOUNT)))
                .contains(TransactionValidationService.ERR_PORTFOLIO_REQUIRED);
    }

    @Test
    @DisplayName("R-7.2: an unknown portfolio id is reported with the id at its full PIC X(8) width")
    void unknownPortfolioIdIsRejected() {
        assertThat(service.validate(TransactionRecord.of("PORT9999", "BU", TEN, PRICE, AMOUNT)))
                .contains("Invalid Portfolio ID: PORT9999");
    }

    @Test
    @DisplayName("R-7.3: only BU, SL, TR and FE are accepted, and the check is case sensitive")
    void transactionTypeIsChecked() {
        assertThat(service.validate(TransactionRecord.of("PORT0001", "XX", TEN, PRICE, AMOUNT)))
                .contains("Invalid Transaction Type: XX");
        assertThat(service.validate(TransactionRecord.of("PORT0001", "bu", TEN, PRICE, AMOUNT)))
                .contains("Invalid Transaction Type: bu");
    }

    @Test
    @DisplayName("R-7.4: quantity, price and amount must be strictly positive")
    void amountsMustBePositive() {
        assertThat(service.validate(TransactionRecord.of("PORT0001", "BU", BigDecimal.ZERO, PRICE, AMOUNT)))
                .contains(TransactionValidationService.ERR_QUANTITY);
        assertThat(service.validate(TransactionRecord.of("PORT0001", "BU", TEN, BigDecimal.ZERO, AMOUNT)))
                .contains(TransactionValidationService.ERR_PRICE);
        assertThat(service.validate(TransactionRecord.of("PORT0001", "BU", TEN, PRICE, BigDecimal.ZERO)))
                .contains(TransactionValidationService.ERR_AMOUNT);
        assertThat(service.validate(
                        TransactionRecord.of("PORT0001", "BU", TEN, PRICE, new BigDecimal("-1.00"))))
                .contains(TransactionValidationService.ERR_AMOUNT);
    }

    @Test
    @DisplayName("R-7.5: a transfer is exempt from the price and amount checks but not the quantity check")
    void transferExemptionIsAsymmetric() {
        assertThat(service.validate(
                        TransactionRecord.of("PORT0001", "TR", TEN, BigDecimal.ZERO, BigDecimal.ZERO)))
                .isEmpty();
        assertThat(service.validate(
                        TransactionRecord.of("PORT0001", "TR", BigDecimal.ZERO, PRICE, AMOUNT)))
                .contains(TransactionValidationService.ERR_QUANTITY);
    }

    @Test
    @DisplayName("R-7.6: checks short circuit, so only the first failure is reported")
    void checksShortCircuit() {
        assertThat(service.validate(
                        TransactionRecord.of("PORT9999", "XX", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)))
                .contains("Invalid Portfolio ID: PORT9999");
    }

    @Test
    @DisplayName("R-7.7: a quantity below the packed field's scale truncates to zero and is rejected")
    void subScaleQuantityTruncatesToZeroAndIsRejected() {
        assertThat(service.validate(
                        TransactionRecord.of("PORT0001", "BU", new BigDecimal("0.00001"), PRICE, AMOUNT)))
                .contains(TransactionValidationService.ERR_QUANTITY);
    }
}
