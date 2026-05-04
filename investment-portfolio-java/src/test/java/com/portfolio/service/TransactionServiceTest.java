package com.portfolio.service;

import com.portfolio.audit.AuditService;
import com.portfolio.dto.TransactionRequest;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.entity.PortfolioStatus;
import com.portfolio.entity.TransactionHistory;
import com.portfolio.entity.TransactionStatus;
import com.portfolio.exception.InsufficientUnitsException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.exception.ValidationException;
import com.portfolio.repository.PortfolioMasterRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private PortfolioMasterRepository portfolioRepository;

    @Mock
    private TransactionHistoryRepository transactionRepository;

    @Mock
    private AuditService auditService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                portfolioRepository, transactionRepository, auditService);
    }

    @Test
    void processBuy_validRequest_success() {
        PortfolioMaster portfolio = createTestPortfolio(BigDecimal.valueOf(100000));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(TransactionHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionRequest request = createRequest("BU", BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), BigDecimal.valueOf(5000));

        TransactionHistory result = transactionService.processTransaction(request);

        assertNotNull(result);
        assertEquals(TransactionStatus.DONE, result.getStatus());
        assertEquals(BigDecimal.valueOf(105000), portfolio.getTotalValue());
    }

    @Test
    void processSell_validRequest_success() {
        PortfolioMaster portfolio = createTestPortfolio(BigDecimal.valueOf(100000));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(TransactionHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionRequest request = createRequest("SL", BigDecimal.valueOf(50),
                BigDecimal.valueOf(100), BigDecimal.valueOf(5000));

        TransactionHistory result = transactionService.processTransaction(request);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(95000), portfolio.getTotalValue());
    }

    @Test
    void processSell_insufficientUnits_throwsException() {
        PortfolioMaster portfolio = createTestPortfolio(BigDecimal.valueOf(1000));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        TransactionRequest request = createRequest("SL", BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.valueOf(10000));

        assertThrows(InsufficientUnitsException.class,
                () -> transactionService.processTransaction(request));
    }

    @Test
    void processTransfer_throwsUnsupported() {
        PortfolioMaster portfolio = createTestPortfolio(BigDecimal.valueOf(100000));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        TransactionRequest request = createRequest("TR", BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), BigDecimal.valueOf(5000));

        assertThrows(UnsupportedOperationException.class,
                () -> transactionService.processTransaction(request));
    }

    @Test
    void processFee_validRequest_success() {
        PortfolioMaster portfolio = createTestPortfolio(BigDecimal.valueOf(100000));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(TransactionHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionRequest request = createRequest("FE", BigDecimal.valueOf(1),
                BigDecimal.valueOf(250), BigDecimal.valueOf(250));

        TransactionHistory result = transactionService.processTransaction(request);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(99750), portfolio.getTotalValue());
    }

    @Test
    void processTransaction_portfolioNotFound_throwsException() {
        when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());

        TransactionRequest request = createRequest("BU", BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), BigDecimal.valueOf(5000));
        request.setPortfolioId("PORT9999");

        assertThrows(PortfolioNotFoundException.class,
                () -> transactionService.processTransaction(request));
    }

    @Test
    void processTransaction_invalidType_throwsValidation() {
        PortfolioMaster portfolio = createTestPortfolio(BigDecimal.valueOf(100000));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        TransactionRequest request = createRequest("XX", BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), BigDecimal.valueOf(5000));

        assertThrows(ValidationException.class,
                () -> transactionService.processTransaction(request));
    }

    private TransactionRequest createRequest(String type, BigDecimal qty,
                                             BigDecimal price, BigDecimal amount) {
        TransactionRequest request = new TransactionRequest();
        request.setPortfolioId("PORT0001");
        request.setInvestmentId("AAPL");
        request.setTransactionType(type);
        request.setQuantity(qty);
        request.setPrice(price);
        request.setAmount(amount);
        request.setCurrency("USD");
        return request;
    }

    private PortfolioMaster createTestPortfolio(BigDecimal totalValue) {
        PortfolioMaster portfolio = new PortfolioMaster();
        portfolio.setPortfolioId("PORT0001");
        portfolio.setPortfolioName("Test Portfolio");
        portfolio.setStatus(PortfolioStatus.ACTIVE);
        portfolio.setTotalValue(totalValue);
        portfolio.setLastMaintDate(LocalDateTime.now());
        return portfolio;
    }
}
