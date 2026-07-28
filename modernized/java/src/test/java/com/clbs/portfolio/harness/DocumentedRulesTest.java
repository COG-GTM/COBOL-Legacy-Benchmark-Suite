package com.clbs.portfolio.harness;

import com.clbs.portfolio.model.CobolDecimal;
import com.clbs.portfolio.model.ErrorCode;
import com.clbs.portfolio.model.ErrorSeverity;
import com.clbs.portfolio.model.PortfolioRecord;
import com.clbs.portfolio.model.PortfolioStatus;
import com.clbs.portfolio.model.TransactionRecord;
import com.clbs.portfolio.model.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The oracle for the translation. The COBOL was never compiled or run, so the documented rules in
 * {@code documentation/technical/data-dictionary.md} and
 * {@code documentation/operations/test-data-specs.md} are the only available statement of intent.
 *
 * <p>These tests do two things: they pin the seeded data to the documented ranges, and they pin the
 * places where the documentation contradicts the copybooks so that a later change to either is
 * caught rather than absorbed.
 */
class DocumentedRulesTest {

    @Nested
    @DisplayName("test-data-specs.md 3.5 - numeric ranges")
    class NumericRanges {

        @Test
        @DisplayName("the documented amount range survives storage in TRN-AMOUNT")
        void transactionAmountRange() {
            TransactionRecord transaction = TestData.buyTransaction();

            transaction.setTrnAmount(TestData.MIN_TRANSACTION_AMOUNT);
            assertEquals(new BigDecimal("0.01"), transaction.getTrnAmount());

            transaction.setTrnAmount(TestData.MAX_TRANSACTION_AMOUNT);
            assertEquals(TestData.MAX_TRANSACTION_AMOUNT, transaction.getTrnAmount());
        }

        @Test
        @DisplayName("the documented portfolio value range survives storage in PORT-TOTAL-VALUE")
        void portfolioValueRange() {
            PortfolioRecord portfolio = TestData.growthPortfolio();

            portfolio.setPortTotalValue(TestData.MIN_PORTFOLIO_VALUE);
            assertEquals(new BigDecimal("0.00"), portfolio.getPortTotalValue());

            portfolio.setPortTotalValue(TestData.MAX_PORTFOLIO_VALUE);
            assertEquals(TestData.MAX_PORTFOLIO_VALUE, portfolio.getPortTotalValue());
        }

        @Test
        @DisplayName("a value one digit past the documented maximum is silently truncated")
        void overflowIsSilent() {
            PortfolioRecord portfolio = TestData.growthPortfolio();
            portfolio.setPortTotalValue(TestData.MAX_PORTFOLIO_VALUE.add(new BigDecimal("0.01")));

            assertEquals(new BigDecimal("0.00"), portfolio.getPortTotalValue());
        }
    }

    @Nested
    @DisplayName("data-dictionary.md 5.1 - transaction validation")
    class ValidationRules {

        @Test
        @DisplayName("share quantity must not be zero for a buy or a sell")
        void quantityMustBePositive() {
            assertTrue(CobolDecimal.isNotPositive(CobolDecimal.quantity("0")));
            assertFalse(CobolDecimal.isNotPositive(TestData.buyTransaction().getTrnQuantity()));
            assertFalse(CobolDecimal.isNotPositive(TestData.sellTransaction().getTrnQuantity()));
        }

        @Test
        @DisplayName("price must be greater than zero for a buy or a sell")
        void priceMustBePositive() {
            assertFalse(CobolDecimal.isNotPositive(TestData.buyTransaction().getTrnPrice()));
            assertFalse(CobolDecimal.isNotPositive(TestData.sellTransaction().getTrnPrice()));
        }

        @Test
        @DisplayName("a fee carries a non-zero amount")
        void feeAmountIsNonZero() {
            assertEquals(new BigDecimal("45.50"), TestData.feeTransaction().getTrnAmount());
        }

        @Test
        @DisplayName("a transfer carries no price or amount, which PORTTRAN exempts from checking")
        void transferHasNoPriceOrAmount() {
            TransactionRecord transfer = TestData.transferTransaction();
            assertEquals(TransactionType.TRANSFER, transfer.getTransactionType());
            assertTrue(CobolDecimal.isNotPositive(transfer.getTrnPrice()));
            assertTrue(CobolDecimal.isNotPositive(transfer.getTrnAmount()));
            assertFalse(CobolDecimal.isNotPositive(transfer.getTrnQuantity()));
        }
    }

    @Nested
    @DisplayName("data-dictionary.md 6 - error catalogue")
    class ErrorCatalogue {

        @Test
        @DisplayName("the six documented codes carry their documented severity")
        void codesAndSeverities() {
            assertEquals(6, ErrorCode.values().length);
            assertEquals(ErrorSeverity.ERROR, ErrorCode.INVALID_ACCOUNT.severity());
            assertEquals(ErrorSeverity.ERROR, ErrorCode.INSUFFICIENT_BALANCE.severity());
            assertEquals(ErrorSeverity.WARNING, ErrorCode.ZERO_DOLLAR_TRANSACTION.severity());
            assertEquals("Invalid Transaction Type", ErrorCode.INVALID_TRANSACTION_TYPE.description());
        }

        @Test
        @DisplayName("the copybook return codes match the documented batch return codes")
        void returnCodes() {
            assertEquals(0, ErrorSeverity.SUCCESS.value());
            assertEquals(4, ErrorSeverity.WARNING.value());
            assertEquals(8, ErrorSeverity.ERROR.value());
            assertEquals(12, ErrorSeverity.SEVERE.value());
            assertEquals(16, ErrorSeverity.TERMINAL.value());
        }
    }

    @Nested
    @DisplayName("documentation that contradicts the copybooks")
    class DocumentationDiscrepancies {

        @Test
        @DisplayName("documented portfolio ids are one byte too long for PORT-ID")
        void portfolioIdWidth() {
            assertEquals(9, TestData.DOCUMENTED_GROWTH_PORTFOLIO_ID.length());
            assertEquals(8, PortfolioRecord.ID_LENGTH);

            PortfolioRecord portfolio = new PortfolioRecord();
            portfolio.setPortId(TestData.DOCUMENTED_GROWTH_PORTFOLIO_ID);
            assertEquals("PORT0000", portfolio.getPortId());
        }

        @Test
        @DisplayName("documented transaction type bytes B and S are not TRNREC codes")
        void transactionTypeCodes() {
            assertNull(TransactionType.fromCode("B"));
            assertNull(TransactionType.fromCode("S"));
            assertEquals(TransactionType.BUY, TransactionType.fromCode("BU"));
        }

        @Test
        @DisplayName("documented status I is not a PORTFLIO level-88 value")
        void portfolioStatusDomain() {
            assertNull(PortfolioStatus.fromCode("I"));
            assertEquals(PortfolioStatus.SUSPENDED,
                    TestData.balancedPortfolio().getPortfolioStatus());
        }
    }
}
