package com.portfolio.domain.service;

import com.portfolio.domain.command.TransactionCommand;
import com.portfolio.domain.exception.ValidationException;
import com.portfolio.domain.model.ClientType;
import com.portfolio.domain.model.Portfolio;
import com.portfolio.domain.model.PortfolioStatus;
import com.portfolio.domain.model.TransactionType;
import com.portfolio.domain.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionValidatorTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    private TransactionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator(portfolioRepository);
    }

    private Portfolio activePortfolio() {
        return new Portfolio("PORT0001", "1234567890", "Test Client", ClientType.INDIVIDUAL);
    }

    private TransactionCommand buyCommand(String portfolioId, BigDecimal qty, BigDecimal price, BigDecimal amount) {
        return new TransactionCommand("20240320", "120000", portfolioId, "000001",
                "INV001", TransactionType.BUY, qty, price, amount, "USD", "USER01");
    }

    // --- 2110-CHECK-PORTFOLIO tests ---

    @Test
    void rejectsNullPortfolioId() {
        TransactionCommand cmd = new TransactionCommand("20240320", "120000", null, "000001",
                "INV001", TransactionType.BUY, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, "USD", "USER01");

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(1, ex.getValidationCode());
        assertTrue(ex.getMessage().contains("required"));
    }

    @Test
    void rejectsBlankPortfolioId() {
        TransactionCommand cmd = buyCommand("        ", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(1, ex.getValidationCode());
    }

    @Test
    void rejectsPortfolioIdWithoutPrefix() {
        TransactionCommand cmd = buyCommand("ACCT0001", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(1, ex.getValidationCode());
        assertTrue(ex.getMessage().contains("format"));
    }

    @Test
    void rejectsNonExistentPortfolio() {
        when(portfolioRepository.findById("PORT9999")).thenReturn(Optional.empty());
        TransactionCommand cmd = buyCommand("PORT9999", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(1, ex.getValidationCode());
        assertTrue(ex.getMessage().contains("PORT9999"));
    }

    @Test
    void rejectsInactivePortfolio() {
        Portfolio closedPortfolio = activePortfolio();
        closedPortfolio.setStatus(PortfolioStatus.CLOSED);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(closedPortfolio));

        TransactionCommand cmd = buyCommand("PORT0001", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(1, ex.getValidationCode());
        assertTrue(ex.getMessage().contains("not active"));
    }

    @Test
    void rejectsSuspendedPortfolio() {
        Portfolio suspended = activePortfolio();
        suspended.setStatus(PortfolioStatus.SUSPENDED);
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(suspended));

        TransactionCommand cmd = buyCommand("PORT0001", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(1, ex.getValidationCode());
    }

    // --- 2120-CHECK-TRANSACTION-TYPE tests ---

    @Test
    void rejectsNullTransactionType() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));

        TransactionCommand cmd = new TransactionCommand("20240320", "120000", "PORT0001", "000001",
                "INV001", null, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, "USD", "USER01");

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(3, ex.getValidationCode());
    }

    // --- 2130-CHECK-AMOUNTS tests ---

    @Test
    void rejectsAmountAboveMax() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = buyCommand("PORT0001", BigDecimal.TEN, BigDecimal.TEN,
                new BigDecimal("10000000000000.00"));

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(4, ex.getValidationCode());
        assertTrue(ex.getMessage().contains("range"));
    }

    @Test
    void rejectsAmountBelowMin() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = buyCommand("PORT0001", BigDecimal.TEN, BigDecimal.TEN,
                new BigDecimal("-10000000000000.00"));

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(4, ex.getValidationCode());
    }

    @Test
    void rejectsZeroQuantityForBuy() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = buyCommand("PORT0001", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN);

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(4, ex.getValidationCode());
        assertTrue(ex.getMessage().contains("Quantity"));
    }

    @Test
    void rejectsNegativeQuantityForSell() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = new TransactionCommand("20240320", "120000", "PORT0001", "000001",
                "INV001", TransactionType.SELL, new BigDecimal("-5"), BigDecimal.TEN, BigDecimal.TEN, "USD", "USER01");

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(4, ex.getValidationCode());
    }

    @Test
    void rejectsNullQuantityForBuy() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = buyCommand("PORT0001", null, BigDecimal.TEN, BigDecimal.TEN);

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(4, ex.getValidationCode());
    }

    @Test
    void rejectsZeroPriceForBuy() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = buyCommand("PORT0001", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(4, ex.getValidationCode());
        assertTrue(ex.getMessage().contains("Price"));
    }

    @Test
    void rejectsNegativePriceForSell() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = new TransactionCommand("20240320", "120000", "PORT0001", "000001",
                "INV001", TransactionType.SELL, BigDecimal.TEN, new BigDecimal("-1"), BigDecimal.TEN, "USD", "USER01");

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(cmd));
        assertEquals(4, ex.getValidationCode());
    }

    // --- Valid commands pass ---

    @Test
    void validBuyCommandPasses() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = buyCommand("PORT0001", BigDecimal.TEN, new BigDecimal("50.00"),
                new BigDecimal("500.00"));

        assertDoesNotThrow(() -> validator.validate(cmd));
    }

    @Test
    void validSellCommandPasses() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = new TransactionCommand("20240320", "120000", "PORT0001", "000001",
                "INV001", TransactionType.SELL, BigDecimal.TEN, new BigDecimal("50.00"),
                new BigDecimal("500.00"), "USD", "USER01");

        assertDoesNotThrow(() -> validator.validate(cmd));
    }

    @Test
    void validFeeCommandPasses() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = new TransactionCommand("20240320", "120000", "PORT0001", "000001",
                "INV001", TransactionType.FEE, null, new BigDecimal("10.00"),
                new BigDecimal("25.00"), "USD", "USER01");

        assertDoesNotThrow(() -> validator.validate(cmd));
    }

    @Test
    void validTransferCommandSkipsPriceCheck() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = new TransactionCommand("20240320", "120000", "PORT0001", "000001",
                "INV001", TransactionType.TRANSFER, null, null, null, "USD", "USER01");

        assertDoesNotThrow(() -> validator.validate(cmd));
    }

    @Test
    void amountAtMaxBoundaryPasses() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = buyCommand("PORT0001", BigDecimal.TEN, BigDecimal.TEN,
                new BigDecimal("9999999999999.99"));

        assertDoesNotThrow(() -> validator.validate(cmd));
    }

    @Test
    void amountAtMinBoundaryPasses() {
        when(portfolioRepository.findById("PORT0001")).thenReturn(Optional.of(activePortfolio()));
        TransactionCommand cmd = buyCommand("PORT0001", BigDecimal.TEN, BigDecimal.TEN,
                new BigDecimal("-9999999999999.99"));

        assertDoesNotThrow(() -> validator.validate(cmd));
    }
}
