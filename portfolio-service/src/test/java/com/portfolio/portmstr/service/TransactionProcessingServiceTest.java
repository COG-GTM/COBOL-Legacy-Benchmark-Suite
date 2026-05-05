package com.portfolio.portmstr.service;

import com.portfolio.portmstr.dto.TransactionRequest;
import com.portfolio.portmstr.exception.InsufficientUnitsException;
import com.portfolio.portmstr.exception.PortfolioNotFoundException;
import com.portfolio.portmstr.exception.PortfolioValidationException;
import com.portfolio.portmstr.model.PortfolioMaster;
import com.portfolio.portmstr.model.TransactionHistory;
import com.portfolio.portmstr.model.enums.ClientType;
import com.portfolio.portmstr.model.enums.PortfolioStatus;
import com.portfolio.portmstr.model.enums.TransactionStatus;
import com.portfolio.portmstr.repository.PortfolioMasterRepository;
import com.portfolio.portmstr.repository.TransactionHistoryRepository;
import com.portfolio.portmstr.validation.PortfolioValidator;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for TransactionProcessingService.
 * Verifies all transaction types match COBOL PORTTRAN.cbl behavior:
 *   2210-PROCESS-BUY, 2220-PROCESS-SELL,
 *   2230-PROCESS-TRANSFER, 2240-PROCESS-FEE
 */
@ExtendWith(MockitoExtension.class)
class TransactionProcessingServiceTest {

    @Mock
    private PortfolioMasterRepository portfolioRepository;

    @Mock
    private TransactionHistoryRepository transactionRepository;

    @Mock
    private PortfolioValidator validator;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TransactionProcessingService service;

    private PortfolioMaster portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new PortfolioMaster();
        portfolio.setPortfolioId("PORT0001");
        portfolio.setAccountNo("1000000001");
        portfolio.setClientName("John Doe");
        portfolio.setClientType(ClientType.INDIVIDUAL);
        portfolio.setStatus(PortfolioStatus.ACTIVE);
        portfolio.setTotalValue(new BigDecimal("100000.00"));
        portfolio.setCashBalance(new BigDecimal("50000.00"));
    }

    @Nested
    @DisplayName("2210-PROCESS-BUY Tests")
    class ProcessBuyTests {

        @Test
        @DisplayName("Buy transaction increases total value and decreases cash")
        void processBuy_updatesBalances() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "BU",
                    new BigDecimal("100"), new BigDecimal("50.00"),
                    new BigDecimal("5000.00"), "USD");

            TransactionHistory result = service.processTransaction(request);

            assertNotNull(result);
            assertEquals(TransactionStatus.DONE, result.getStatus());

            ArgumentCaptor<PortfolioMaster> captor = ArgumentCaptor.forClass(PortfolioMaster.class);
            verify(portfolioRepository).save(captor.capture());
            PortfolioMaster updated = captor.getValue();
            assertEquals(new BigDecimal("105000.00"), updated.getTotalValue());
            assertEquals(new BigDecimal("45000.00"), updated.getCashBalance());
        }

        @Test
        @DisplayName("Buy transaction generates unique transaction ID")
        void processBuy_generatesTransactionId() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "BU",
                    new BigDecimal("100"), new BigDecimal("50.00"),
                    new BigDecimal("5000.00"), "USD");

            TransactionHistory result = service.processTransaction(request);

            assertNotNull(result.getTransactionId());
            assertEquals(20, result.getTransactionId().length());
        }

        @Test
        @DisplayName("Buy transaction creates audit trail")
        void processBuy_createsAuditTrail() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "BU",
                    new BigDecimal("100"), new BigDecimal("50.00"),
                    new BigDecimal("5000.00"), "USD");

            service.processTransaction(request);

            verify(auditService).logTransaction(
                    anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("2220-PROCESS-SELL Tests")
    class ProcessSellTests {

        @Test
        @DisplayName("Sell transaction decreases total value and increases cash")
        void processSell_updatesBalances() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "SL",
                    new BigDecimal("50"), new BigDecimal("50.00"),
                    new BigDecimal("2500.00"), "USD");

            service.processTransaction(request);

            ArgumentCaptor<PortfolioMaster> captor = ArgumentCaptor.forClass(PortfolioMaster.class);
            verify(portfolioRepository).save(captor.capture());
            PortfolioMaster updated = captor.getValue();
            assertEquals(new BigDecimal("97500.00"), updated.getTotalValue());
            assertEquals(new BigDecimal("52500.00"), updated.getCashBalance());
        }

        @Test
        @DisplayName("Sell transaction fails when amount exceeds portfolio value")
        void processSell_insufficientUnits() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "SL",
                    new BigDecimal("5000"), new BigDecimal("50.00"),
                    new BigDecimal("250000.00"), "USD");

            assertThrows(InsufficientUnitsException.class,
                    () -> service.processTransaction(request));
        }

        @Test
        @DisplayName("Sell exact total value brings portfolio value to zero")
        void processSell_exactTotal() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "SL",
                    new BigDecimal("2000"), new BigDecimal("50.00"),
                    new BigDecimal("100000.00"), "USD");

            service.processTransaction(request);

            ArgumentCaptor<PortfolioMaster> captor = ArgumentCaptor.forClass(PortfolioMaster.class);
            verify(portfolioRepository).save(captor.capture());
            assertEquals(0, captor.getValue().getTotalValue().compareTo(BigDecimal.ZERO));
        }
    }

    @Nested
    @DisplayName("2230-PROCESS-TRANSFER Tests")
    class ProcessTransferTests {

        @Test
        @DisplayName("Transfer throws not-implemented exception")
        void processTransfer_notImplemented() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "TR",
                    new BigDecimal("100"), new BigDecimal("50.00"),
                    new BigDecimal("5000.00"), "USD");

            assertThrows(PortfolioValidationException.class,
                    () -> service.processTransaction(request));
        }
    }

    @Nested
    @DisplayName("2240-PROCESS-FEE Tests")
    class ProcessFeeTests {

        @Test
        @DisplayName("Fee transaction reduces cash balance")
        void processFee_reducesCash() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionRequest request = new TransactionRequest(
                    "PORT0001", "INV0000001", "FE",
                    new BigDecimal("1"), new BigDecimal("250.00"),
                    new BigDecimal("250.00"), "USD");

            service.processTransaction(request);

            ArgumentCaptor<PortfolioMaster> captor = ArgumentCaptor.forClass(PortfolioMaster.class);
            verify(portfolioRepository).save(captor.capture());
            assertEquals(new BigDecimal("49750.00"), captor.getValue().getCashBalance());
        }
    }

    @Nested
    @DisplayName("Portfolio Not Found Tests")
    class PortfolioNotFoundTests {

        @Test
        @DisplayName("Transaction fails when portfolio does not exist")
        void processTransaction_portfolioNotFound() {
            when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

            TransactionRequest request = new TransactionRequest(
                    "PORT9999", "INV0000001", "BU",
                    new BigDecimal("100"), new BigDecimal("50.00"),
                    new BigDecimal("5000.00"), "USD");

            assertThrows(PortfolioNotFoundException.class,
                    () -> service.processTransaction(request));
        }
    }
}
