package com.portfolio.service;

import com.portfolio.audit.AuditService;
import com.portfolio.dto.PortfolioCreateRequest;
import com.portfolio.dto.PortfolioUpdateRequest;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.entity.PortfolioStatus;
import com.portfolio.exception.DuplicatePortfolioException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.exception.ValidationException;
import com.portfolio.repository.PortfolioMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioMasterRepository portfolioRepository;

    @Mock
    private AuditService auditService;

    private PortfolioValidator validator;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        validator = new PortfolioValidator();
        portfolioService = new PortfolioService(portfolioRepository, validator, auditService);
    }

    @Test
    void createPortfolio_validRequest_success() {
        PortfolioCreateRequest request = createValidRequest();
        when(portfolioRepository.existsById("PORT0001")).thenReturn(false);
        when(portfolioRepository.save(any(PortfolioMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioMaster result = portfolioService.createPortfolio(request);

        assertNotNull(result);
        assertEquals("PORT0001", result.getPortfolioId());
        assertEquals("Test Portfolio", result.getPortfolioName());
        assertEquals(PortfolioStatus.ACTIVE, result.getStatus());
        verify(portfolioRepository).save(any(PortfolioMaster.class));
    }

    @Test
    void createPortfolio_duplicateId_throwsException() {
        PortfolioCreateRequest request = createValidRequest();
        when(portfolioRepository.existsById("PORT0001")).thenReturn(true);

        assertThrows(DuplicatePortfolioException.class,
                () -> portfolioService.createPortfolio(request));
        verify(portfolioRepository, never()).save(any());
    }

    @Test
    void createPortfolio_invalidId_throwsValidationException() {
        PortfolioCreateRequest request = createValidRequest();
        request.setPortfolioId("INVALID");

        assertThrows(ValidationException.class,
                () -> portfolioService.createPortfolio(request));
    }

    @Test
    void createPortfolio_blankName_throwsValidationException() {
        PortfolioCreateRequest request = createValidRequest();
        request.setPortfolioName("");

        assertThrows(ValidationException.class,
                () -> portfolioService.createPortfolio(request));
    }

    @Test
    void readPortfolio_exists_returnsPortfolio() {
        PortfolioMaster portfolio = createTestPortfolio();
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        PortfolioMaster result = portfolioService.readPortfolio("PORT0001");

        assertNotNull(result);
        assertEquals("PORT0001", result.getPortfolioId());
    }

    @Test
    void readPortfolio_notExists_throwsException() {
        when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class,
                () -> portfolioService.readPortfolio("PORT9999"));
    }

    @Test
    void updatePortfolio_validRequest_success() {
        PortfolioMaster existing = createTestPortfolio();
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existing));
        when(portfolioRepository.save(any(PortfolioMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioUpdateRequest updateRequest = new PortfolioUpdateRequest();
        updateRequest.setPortfolioName("Updated Portfolio");
        updateRequest.setStatus("C");

        PortfolioMaster result = portfolioService.updatePortfolio("PORT0001", updateRequest);

        assertEquals("Updated Portfolio", result.getPortfolioName());
        assertEquals(PortfolioStatus.CLOSED, result.getStatus());
    }

    @Test
    void deletePortfolio_exists_success() {
        PortfolioMaster existing = createTestPortfolio();
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(existing));

        portfolioService.deletePortfolio("PORT0001");

        verify(portfolioRepository).delete(existing);
    }

    @Test
    void deletePortfolio_notExists_throwsException() {
        when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class,
                () -> portfolioService.deletePortfolio("PORT9999"));
    }

    private PortfolioCreateRequest createValidRequest() {
        PortfolioCreateRequest request = new PortfolioCreateRequest();
        request.setPortfolioId("PORT0001");
        request.setPortfolioName("Test Portfolio");
        request.setStatus("A");
        request.setAccountNo("ACC0000001");
        request.setClientName("Test Client");
        request.setClientType("I");
        request.setCurrencyCode("USD");
        return request;
    }

    private PortfolioMaster createTestPortfolio() {
        PortfolioMaster portfolio = new PortfolioMaster();
        portfolio.setPortfolioId("PORT0001");
        portfolio.setPortfolioName("Test Portfolio");
        portfolio.setStatus(PortfolioStatus.ACTIVE);
        portfolio.setAccountNo("ACC0000001");
        portfolio.setClientName("Test Client");
        portfolio.setCreateDate(LocalDate.now());
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolio.setTotalValue(BigDecimal.valueOf(100000));
        portfolio.setCashBalance(BigDecimal.valueOf(10000));
        return portfolio;
    }
}
