package com.portfolio.domain.service;

import com.portfolio.domain.command.TransactionCommand;
import com.portfolio.domain.event.TransactionProcessedEvent;
import com.portfolio.domain.exception.InsufficientUnitsException;
import com.portfolio.domain.model.ClientType;
import com.portfolio.domain.model.Portfolio;
import com.portfolio.domain.model.TransactionType;
import com.portfolio.domain.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TransactionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TransactionProcessor(portfolioRepository, eventPublisher);
    }

    private Portfolio portfolioWithUnits(BigDecimal units, BigDecimal cost) {
        Portfolio p = new Portfolio("PORT0001", "1234567890", "Test Client", ClientType.INDIVIDUAL);
        if (units.compareTo(BigDecimal.ZERO) > 0) {
            p.applyBuy(units, cost);
        }
        return p;
    }

    private TransactionCommand command(TransactionType type, BigDecimal qty, BigDecimal amount) {
        return new TransactionCommand("20240320", "120000", "PORT0001", "000001",
                "INV001", type, qty, new BigDecimal("50.00"), amount, "USD", "USER01");
    }

    // --- Buy ---

    @Test
    void processBuyAddsUnitsAndCost() {
        Portfolio portfolio = portfolioWithUnits(BigDecimal.ZERO, BigDecimal.ZERO);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        processor.process(command(TransactionType.BUY, new BigDecimal("100"), new BigDecimal("5000.00")));

        assertEquals(new BigDecimal("100"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("5000.00"), portfolio.getTotalCost());
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void processBuyEmitsEvent() {
        Portfolio portfolio = portfolioWithUnits(BigDecimal.ZERO, BigDecimal.ZERO);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        processor.process(command(TransactionType.BUY, new BigDecimal("100"), new BigDecimal("5000.00")));

        ArgumentCaptor<TransactionProcessedEvent> captor = ArgumentCaptor.forClass(TransactionProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        TransactionProcessedEvent event = captor.getValue();
        assertEquals("PORT0001", event.portfolioId());
        assertEquals(TransactionType.BUY, event.transactionType());
        assertEquals(new BigDecimal("5000.00"), event.amount());
        assertEquals("USER01", event.userId());
    }

    // --- Sell ---

    @Test
    void processSellSubtractsUnitsAndCost() {
        Portfolio portfolio = portfolioWithUnits(new BigDecimal("100"), new BigDecimal("5000.00"));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        processor.process(command(TransactionType.SELL, new BigDecimal("30"), new BigDecimal("1500.00")));

        assertEquals(new BigDecimal("70"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("3500.00"), portfolio.getTotalCost());
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void processSellThrowsWhenInsufficientUnits() {
        Portfolio portfolio = portfolioWithUnits(new BigDecimal("10"), new BigDecimal("500.00"));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        assertThrows(InsufficientUnitsException.class,
                () -> processor.process(command(TransactionType.SELL, new BigDecimal("50"), new BigDecimal("2500.00"))));

        verify(portfolioRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- Fee ---

    @Test
    void processFeeReducesCostOnly() {
        Portfolio portfolio = portfolioWithUnits(new BigDecimal("100"), new BigDecimal("5000.00"));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        processor.process(command(TransactionType.FEE, null, new BigDecimal("25.00")));

        assertEquals(new BigDecimal("100"), portfolio.getTotalUnits());
        assertEquals(new BigDecimal("4975.00"), portfolio.getTotalCost());
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void processFeeOnZeroBalance() {
        Portfolio portfolio = portfolioWithUnits(BigDecimal.ZERO, BigDecimal.ZERO);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        processor.process(command(TransactionType.FEE, null, new BigDecimal("10.00")));

        assertEquals(BigDecimal.ZERO, portfolio.getTotalUnits());
        assertEquals(new BigDecimal("-10.00"), portfolio.getTotalCost());
    }

    // --- Transfer ---

    @Test
    void processTransferThrowsUnsupported() {
        Portfolio portfolio = portfolioWithUnits(new BigDecimal("100"), new BigDecimal("5000.00"));
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        assertThrows(UnsupportedOperationException.class,
                () -> processor.process(command(TransactionType.TRANSFER, null, null)));

        verify(portfolioRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- Portfolio not found ---

    @Test
    void processThrowsWhenPortfolioNotFound() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> processor.process(command(TransactionType.BUY, BigDecimal.TEN, BigDecimal.TEN)));
    }

    // --- Audit: markMaintenance is called ---

    @Test
    void processCallsMarkMaintenance() {
        Portfolio portfolio = portfolioWithUnits(BigDecimal.ZERO, BigDecimal.ZERO);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(portfolio));

        processor.process(command(TransactionType.BUY, new BigDecimal("10"), new BigDecimal("500.00")));

        assertEquals("USER01", portfolio.getLastUser());
        assertNotNull(portfolio.getLastMaintenance());
    }
}
