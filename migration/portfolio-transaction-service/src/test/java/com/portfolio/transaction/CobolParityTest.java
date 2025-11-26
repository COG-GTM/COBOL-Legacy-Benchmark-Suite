package com.portfolio.transaction;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResponse;
import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.repository.PortfolioRepository;
import com.portfolio.transaction.service.TransactionOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("COBOL Parity Tests - Verifies Java service matches PORTTRAN.cbl behavior")
class CobolParityTest {

    @Autowired
    private TransactionOrchestrationService service;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @BeforeEach
    void setup() {
        Portfolio portfolio = new Portfolio();
        portfolio.setPortfolioId("PORT001");
        portfolio.setAccountNo("ACCT001");
        portfolio.setTotalUnits(new BigDecimal("100.0000"));
        portfolio.setTotalCost(new BigDecimal("10000.00"));
        portfolioRepository.save(portfolio);
    }

    @Nested
    @DisplayName("2110-CHECK-PORTFOLIO Parity Tests")
    class PortfolioValidationParityTests {

        @Test
        @DisplayName("COBOL: Portfolio ID is required - blank ID rejected")
        void shouldRejectBlankPortfolioId() {
            TransactionRequest request = new TransactionRequest(
                "   ", "BU", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

            TransactionResponse response = service.processTransaction(request);

            assertEquals("REJECTED", response.getStatus());
            assertTrue(response.getErrorMessage().contains("Portfolio ID is required"));
        }

        @Test
        @DisplayName("COBOL: Invalid Portfolio ID - non-existent portfolio rejected")
        void shouldRejectNonExistentPortfolio() {
            TransactionRequest request = new TransactionRequest(
                "INVALID", "BU", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

            TransactionResponse response = service.processTransaction(request);

            assertEquals("REJECTED", response.getStatus());
            assertTrue(response.getErrorMessage().contains("Invalid Portfolio ID"));
        }
    }

    @Nested
    @DisplayName("2120-CHECK-TRANSACTION-TYPE Parity Tests")
    class TransactionTypeValidationParityTests {

        @ParameterizedTest
        @CsvSource({"BU", "SL", "TR", "FE"})
        @DisplayName("COBOL: Valid transaction types accepted")
        void shouldAcceptValidTransactionTypes(String type) {
            TransactionRequest request = new TransactionRequest(
                "PORT001", type, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

            TransactionResponse response = service.processTransaction(request);

            if ("TR".equals(type)) {
                assertEquals("REJECTED", response.getStatus());
                assertTrue(response.getErrorMessage().contains("not implemented"));
            } else {
                assertEquals("PROCESSED", response.getStatus());
            }
        }

        @Test
        @DisplayName("COBOL: Invalid Transaction Type rejected")
        void shouldRejectInvalidTransactionType() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "XX", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

            TransactionResponse response = service.processTransaction(request);

            assertEquals("REJECTED", response.getStatus());
            assertTrue(response.getErrorMessage().contains("Invalid Transaction Type"));
        }
    }

    @Nested
    @DisplayName("2130-CHECK-AMOUNTS Parity Tests")
    class AmountValidationParityTests {

        @Test
        @DisplayName("COBOL: Quantity must be greater than zero")
        void shouldRejectZeroQuantity() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "BU", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN);

            TransactionResponse response = service.processTransaction(request);

            assertEquals("REJECTED", response.getStatus());
            assertTrue(response.getErrorMessage().contains("Quantity must be greater than zero"));
        }

        @Test
        @DisplayName("COBOL: Price must be greater than zero for non-TR")
        void shouldRejectZeroPriceForNonTransfer() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "BU", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);

            TransactionResponse response = service.processTransaction(request);

            assertEquals("REJECTED", response.getStatus());
            assertTrue(response.getErrorMessage().contains("Price must be greater than zero"));
        }

        @Test
        @DisplayName("COBOL: Amount must be greater than zero for non-TR")
        void shouldRejectZeroAmountForNonTransfer() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "BU", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO);

            TransactionResponse response = service.processTransaction(request);

            assertEquals("REJECTED", response.getStatus());
            assertTrue(response.getErrorMessage().contains("Amount must be greater than zero"));
        }
    }

    @Nested
    @DisplayName("2210-PROCESS-BUY Parity Tests")
    class BuyProcessingParityTests {

        @Test
        @DisplayName("COBOL: ADD TRN-QUANTITY TO PORT-TOTAL-UNITS")
        void shouldAddQuantityToTotalUnits() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "BU", new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("5000"));

            service.processTransaction(request);

            Portfolio updated = portfolioRepository.findById("PORT001").orElseThrow();
            assertEquals(new BigDecimal("150.0000"), updated.getTotalUnits());
        }

        @Test
        @DisplayName("COBOL: ADD TRN-AMOUNT TO PORT-TOTAL-COST")
        void shouldAddAmountToTotalCost() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "BU", new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("5000"));

            service.processTransaction(request);

            Portfolio updated = portfolioRepository.findById("PORT001").orElseThrow();
            assertEquals(new BigDecimal("15000.00"), updated.getTotalCost());
        }
    }

    @Nested
    @DisplayName("2220-PROCESS-SELL Parity Tests")
    class SellProcessingParityTests {

        @Test
        @DisplayName("COBOL: Insufficient units for sale - rejected")
        void shouldRejectSellWithInsufficientUnits() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "SL", new BigDecimal("200"), new BigDecimal("100"), new BigDecimal("20000"));

            TransactionResponse response = service.processTransaction(request);

            assertEquals("REJECTED", response.getStatus());
            assertTrue(response.getErrorMessage().contains("Insufficient units"));
        }

        @Test
        @DisplayName("COBOL: SUBTRACT TRN-QUANTITY FROM PORT-TOTAL-UNITS")
        void shouldSubtractQuantityFromTotalUnits() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "SL", new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("5000"));

            service.processTransaction(request);

            Portfolio updated = portfolioRepository.findById("PORT001").orElseThrow();
            assertEquals(new BigDecimal("50.0000"), updated.getTotalUnits());
        }

        @Test
        @DisplayName("COBOL: SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST")
        void shouldSubtractAmountFromTotalCost() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "SL", new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("5000"));

            service.processTransaction(request);

            Portfolio updated = portfolioRepository.findById("PORT001").orElseThrow();
            assertEquals(new BigDecimal("5000.00"), updated.getTotalCost());
        }
    }

    @Nested
    @DisplayName("2230-PROCESS-TRANSFER Parity Tests")
    class TransferProcessingParityTests {

        @Test
        @DisplayName("COBOL: Transfer processing not implemented")
        void shouldRejectTransferAsNotImplemented() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "TR", new BigDecimal("50"), null, null);

            TransactionResponse response = service.processTransaction(request);

            assertEquals("REJECTED", response.getStatus());
            assertTrue(response.getErrorMessage().contains("not implemented"));
        }
    }

    @Nested
    @DisplayName("2240-PROCESS-FEE Parity Tests")
    class FeeProcessingParityTests {

        @Test
        @DisplayName("COBOL: SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST (units unchanged)")
        void shouldSubtractAmountFromCostWithoutAffectingUnits() {
            TransactionRequest request = new TransactionRequest(
                "PORT001", "FE", new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"));

            service.processTransaction(request);

            Portfolio updated = portfolioRepository.findById("PORT001").orElseThrow();
            assertEquals(new BigDecimal("100.0000"), updated.getTotalUnits());
            assertEquals(new BigDecimal("9950.00"), updated.getTotalCost());
        }
    }
}
