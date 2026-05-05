package com.portfolio.portmstr.service;

import com.portfolio.portmstr.dto.PortfolioRequest;
import com.portfolio.portmstr.dto.PortfolioResponse;
import com.portfolio.portmstr.exception.DuplicatePortfolioException;
import com.portfolio.portmstr.exception.PortfolioNotFoundException;
import com.portfolio.portmstr.model.PortfolioMaster;
import com.portfolio.portmstr.model.enums.ClientType;
import com.portfolio.portmstr.model.enums.PortfolioStatus;
import com.portfolio.portmstr.repository.PortfolioMasterRepository;
import com.portfolio.portmstr.validation.PortfolioValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for PortfolioMasterService.
 * Verifies all CRUD operations match COBOL PORTMSTR.cbl behavior:
 *   2000-CREATE-PORTFOLIO, 3000-READ-PORTFOLIO,
 *   4000-UPDATE-PORTFOLIO, 5000-DELETE-PORTFOLIO
 */
@ExtendWith(MockitoExtension.class)
class PortfolioMasterServiceTest {

    @Mock
    private PortfolioMasterRepository portfolioRepository;

    @Mock
    private PortfolioValidator validator;

    @Mock
    private AuditService auditService;

    @Mock
    private ErrorLoggingService errorLoggingService;

    @InjectMocks
    private PortfolioMasterService service;

    private PortfolioRequest validRequest;
    private PortfolioMaster existingPortfolio;

    @BeforeEach
    void setUp() {
        validRequest = new PortfolioRequest(
                "PORT0001", "1000000001", "John Doe", "I", "A",
                new BigDecimal("1000000.00"), new BigDecimal("100000.00"), "USD");

        existingPortfolio = new PortfolioMaster();
        existingPortfolio.setPortfolioId("PORT0001");
        existingPortfolio.setAccountNo("1000000001");
        existingPortfolio.setClientName("John Doe");
        existingPortfolio.setClientType(ClientType.INDIVIDUAL);
        existingPortfolio.setStatus(PortfolioStatus.ACTIVE);
        existingPortfolio.setTotalValue(new BigDecimal("1000000.00"));
        existingPortfolio.setCashBalance(new BigDecimal("100000.00"));
        existingPortfolio.setCurrencyCode("USD");
        existingPortfolio.setCreateDate(LocalDate.now());
        existingPortfolio.setLastMaintDate(LocalDate.now());
        existingPortfolio.setLastMaintTimestamp(LocalDateTime.now());
    }

    @Nested
    @DisplayName("2000-CREATE-PORTFOLIO Tests")
    class CreatePortfolioTests {

        @Test
        @DisplayName("Successfully creates a portfolio (VSAM WRITE, status 00)")
        void createPortfolio_success() {
            when(portfolioRepository.existsByPortfolioId("PORT0001")).thenReturn(false);
            when(portfolioRepository.save(any(PortfolioMaster.class))).thenAnswer(i -> i.getArgument(0));

            PortfolioResponse response = service.createPortfolio(validRequest);

            assertEquals("PORT0001", response.portfolioId());
            assertEquals("John Doe", response.clientName());
            assertEquals(0, response.returnCode());
            verify(auditService).logPortfolioCreate(any(), anyString());
        }

        @Test
        @DisplayName("Rejects duplicate portfolio ID (VSAM status 22)")
        void createPortfolio_duplicateKey() {
            when(portfolioRepository.existsByPortfolioId("PORT0001")).thenReturn(true);

            assertThrows(DuplicatePortfolioException.class,
                    () -> service.createPortfolio(validRequest));

            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sets creation date and maintenance timestamp")
        void createPortfolio_setsAuditFields() {
            when(portfolioRepository.existsByPortfolioId("PORT0001")).thenReturn(false);
            when(portfolioRepository.save(any(PortfolioMaster.class))).thenAnswer(i -> i.getArgument(0));

            service.createPortfolio(validRequest);

            ArgumentCaptor<PortfolioMaster> captor = ArgumentCaptor.forClass(PortfolioMaster.class);
            verify(portfolioRepository).save(captor.capture());

            PortfolioMaster saved = captor.getValue();
            assertNotNull(saved.getCreateDate());
            assertNotNull(saved.getLastMaintDate());
            assertNotNull(saved.getLastMaintTimestamp());
        }

        @Test
        @DisplayName("Maps client type I=Individual correctly")
        void createPortfolio_mapsClientTypeIndividual() {
            when(portfolioRepository.existsByPortfolioId("PORT0001")).thenReturn(false);
            when(portfolioRepository.save(any(PortfolioMaster.class))).thenAnswer(i -> i.getArgument(0));

            PortfolioResponse response = service.createPortfolio(validRequest);

            assertEquals("I", response.clientType());
        }

        @Test
        @DisplayName("Maps client type C=Corporate correctly")
        void createPortfolio_mapsClientTypeCorporate() {
            PortfolioRequest corpRequest = new PortfolioRequest(
                    "PORT0002", "2000000001", "Acme Corp", "C", "A",
                    BigDecimal.ZERO, BigDecimal.ZERO, "USD");
            when(portfolioRepository.existsByPortfolioId("PORT0002")).thenReturn(false);
            when(portfolioRepository.save(any(PortfolioMaster.class))).thenAnswer(i -> i.getArgument(0));

            PortfolioResponse response = service.createPortfolio(corpRequest);

            assertEquals("C", response.clientType());
        }

        @Test
        @DisplayName("Defaults cash balance to zero when null")
        void createPortfolio_defaultsCashBalance() {
            PortfolioRequest reqNoCash = new PortfolioRequest(
                    "PORT0003", "3000000001", "Test Client", "T", "A",
                    null, null, null);
            when(portfolioRepository.existsByPortfolioId("PORT0003")).thenReturn(false);
            when(portfolioRepository.save(any(PortfolioMaster.class))).thenAnswer(i -> i.getArgument(0));

            service.createPortfolio(reqNoCash);

            ArgumentCaptor<PortfolioMaster> captor = ArgumentCaptor.forClass(PortfolioMaster.class);
            verify(portfolioRepository).save(captor.capture());
            assertEquals(BigDecimal.ZERO, captor.getValue().getTotalValue());
            assertEquals(BigDecimal.ZERO, captor.getValue().getCashBalance());
            assertEquals("USD", captor.getValue().getCurrencyCode());
        }
    }

    @Nested
    @DisplayName("3000-READ-PORTFOLIO Tests")
    class ReadPortfolioTests {

        @Test
        @DisplayName("Successfully reads existing portfolio (VSAM READ, status 00)")
        void readPortfolio_success() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existingPortfolio));

            PortfolioResponse response = service.readPortfolio("PORT0001");

            assertEquals("PORT0001", response.portfolioId());
            assertEquals("John Doe", response.clientName());
            assertEquals(0, response.returnCode());
        }

        @Test
        @DisplayName("Throws PortfolioNotFoundException (VSAM status 23)")
        void readPortfolio_notFound() {
            when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> service.readPortfolio("PORT9999"));
        }

        @Test
        @DisplayName("Returns all financial data accurately")
        void readPortfolio_returnsFinancialData() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existingPortfolio));

            PortfolioResponse response = service.readPortfolio("PORT0001");

            assertEquals(new BigDecimal("1000000.00"), response.totalValue());
            assertEquals(new BigDecimal("100000.00"), response.cashBalance());
            assertEquals("USD", response.currencyCode());
        }
    }

    @Nested
    @DisplayName("4000-UPDATE-PORTFOLIO Tests")
    class UpdatePortfolioTests {

        @Test
        @DisplayName("Successfully updates portfolio (VSAM REWRITE, status 00)")
        void updatePortfolio_success() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existingPortfolio));
            when(portfolioRepository.save(any(PortfolioMaster.class))).thenAnswer(i -> i.getArgument(0));

            PortfolioRequest updateRequest = new PortfolioRequest(
                    "PORT0001", "1000000001", "Jane Doe", "I", "A",
                    new BigDecimal("2000000.00"), new BigDecimal("200000.00"), "EUR");

            PortfolioResponse response = service.updatePortfolio("PORT0001", updateRequest);

            assertEquals("Jane Doe", response.clientName());
            assertEquals(new BigDecimal("2000000.00"), response.totalValue());
            assertEquals("EUR", response.currencyCode());
            assertEquals(0, response.returnCode());
        }

        @Test
        @DisplayName("Throws PortfolioNotFoundException for non-existent ID")
        void updatePortfolio_notFound() {
            when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> service.updatePortfolio("PORT9999", validRequest));
        }

        @Test
        @DisplayName("Creates audit log with before and after images")
        void updatePortfolio_createsAuditTrail() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existingPortfolio));
            when(portfolioRepository.save(any(PortfolioMaster.class))).thenAnswer(i -> i.getArgument(0));

            service.updatePortfolio("PORT0001", validRequest);

            verify(auditService).logPortfolioUpdate(any(), any(), anyString());
        }

        @Test
        @DisplayName("Updates last maintenance date/timestamp")
        void updatePortfolio_updatesMaintenanceFields() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existingPortfolio));
            when(portfolioRepository.save(any(PortfolioMaster.class))).thenAnswer(i -> i.getArgument(0));

            service.updatePortfolio("PORT0001", validRequest);

            ArgumentCaptor<PortfolioMaster> captor = ArgumentCaptor.forClass(PortfolioMaster.class);
            verify(portfolioRepository).save(captor.capture());
            assertEquals(LocalDate.now(), captor.getValue().getLastMaintDate());
            assertNotNull(captor.getValue().getLastMaintTimestamp());
        }

        @Test
        @DisplayName("Status change from ACTIVE to SUSPENDED")
        void updatePortfolio_statusChange() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existingPortfolio));
            when(portfolioRepository.save(any(PortfolioMaster.class))).thenAnswer(i -> i.getArgument(0));

            PortfolioRequest suspendRequest = new PortfolioRequest(
                    "PORT0001", "1000000001", "John Doe", "I", "S",
                    new BigDecimal("1000000.00"), new BigDecimal("100000.00"), "USD");

            PortfolioResponse response = service.updatePortfolio("PORT0001", suspendRequest);

            assertEquals("S", response.status());
        }
    }

    @Nested
    @DisplayName("5000-DELETE-PORTFOLIO Tests")
    class DeletePortfolioTests {

        @Test
        @DisplayName("Successfully deletes portfolio (VSAM DELETE, status 00)")
        void deletePortfolio_success() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existingPortfolio));

            PortfolioResponse response = service.deletePortfolio("PORT0001");

            verify(portfolioRepository).delete(existingPortfolio);
            assertEquals("PORT0001", response.portfolioId());
            assertEquals(0, response.returnCode());
        }

        @Test
        @DisplayName("Throws PortfolioNotFoundException for missing portfolio")
        void deletePortfolio_notFound() {
            when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> service.deletePortfolio("PORT9999"));
        }

        @Test
        @DisplayName("Creates audit log with reason code")
        void deletePortfolio_createsAuditLog() {
            when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existingPortfolio));

            service.deletePortfolio("PORT0001");

            verify(auditService).logPortfolioDelete(any(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("List Operations Tests")
    class ListOperationsTests {

        @Test
        @DisplayName("List all portfolios (PORTREAD sequential read)")
        void listPortfolios_returnsAll() {
            when(portfolioRepository.findAll()).thenReturn(List.of(existingPortfolio));

            List<PortfolioResponse> results = service.listPortfolios();

            assertEquals(1, results.size());
            assertEquals("PORT0001", results.get(0).portfolioId());
        }

        @Test
        @DisplayName("List active portfolios (DB2 ACTIVE_PORTFOLIOS view)")
        void listActivePortfolios() {
            when(portfolioRepository.findActivePortfolios()).thenReturn(List.of(existingPortfolio));

            List<PortfolioResponse> results = service.listActivePortfolios();

            assertEquals(1, results.size());
        }
    }
}
