package com.clbs.portfolio.batch.processor;

import com.clbs.portfolio.entity.Portfolio;
import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.PortfolioRepository;
import com.clbs.portfolio.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionUpdateProcessorTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    private PositionUpdateProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PositionUpdateProcessor(positionRepository, portfolioRepository);
    }

    @Test
    void shouldCreateNewPositionOnBuy() throws Exception {
        TransactionRecord txn = createBuyTransaction();
        when(positionRepository.findByPortfolioIdAndInvestmentId("PORT1234", "AAPL000001"))
                .thenReturn(Optional.empty());
        when(positionRepository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionRecord result = processor.process(txn);

        assertThat(result.getStatus()).isEqualTo("APPLIED");

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(saved.getCostBasis()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(saved.getStatus()).isEqualTo("A");
    }

    @Test
    void shouldAddToExistingPositionOnBuy() throws Exception {
        TransactionRecord txn = createBuyTransaction();
        Position existing = Position.builder()
                .portfolioId("PORT1234")
                .investmentId("AAPL000001")
                .quantity(new BigDecimal("50.0000"))
                .costBasis(new BigDecimal("7500.00"))
                .marketValue(new BigDecimal("7500.00"))
                .status("A")
                .build();

        when(positionRepository.findByPortfolioIdAndInvestmentId("PORT1234", "AAPL000001"))
                .thenReturn(Optional.of(existing));
        when(positionRepository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(txn);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("150.0000"));
        assertThat(saved.getCostBasis()).isEqualByComparingTo(new BigDecimal("22500.00"));
    }

    @Test
    void shouldReducePositionOnSell() throws Exception {
        TransactionRecord txn = createSellTransaction();
        Position existing = Position.builder()
                .portfolioId("PORT1234")
                .investmentId("AAPL000001")
                .quantity(new BigDecimal("200.0000"))
                .costBasis(new BigDecimal("30000.00"))
                .marketValue(new BigDecimal("30000.00"))
                .status("A")
                .realizedGainLoss(BigDecimal.ZERO)
                .build();

        when(positionRepository.findByPortfolioIdAndInvestmentId("PORT1234", "AAPL000001"))
                .thenReturn(Optional.of(existing));
        when(positionRepository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(txn);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("100.0000"));
        // Cost basis: sold 100 of 200 -> 50% -> 15000 reduction
        assertThat(saved.getCostBasis()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(saved.getStatus()).isEqualTo("A");
    }

    @Test
    void shouldClosePositionOnFullSell() throws Exception {
        TransactionRecord txn = createSellTransaction();
        txn.setQuantity(new BigDecimal("-200.0000"));
        txn.setAmount(new BigDecimal("-30000.00"));

        Position existing = Position.builder()
                .portfolioId("PORT1234")
                .investmentId("AAPL000001")
                .quantity(new BigDecimal("200.0000"))
                .costBasis(new BigDecimal("30000.00"))
                .marketValue(new BigDecimal("30000.00"))
                .status("A")
                .realizedGainLoss(BigDecimal.ZERO)
                .build();

        when(positionRepository.findByPortfolioIdAndInvestmentId("PORT1234", "AAPL000001"))
                .thenReturn(Optional.of(existing));
        when(positionRepository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(txn);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getStatus()).isEqualTo("C");
    }

    @Test
    void shouldCalculateRealizedGainLossOnSell() throws Exception {
        TransactionRecord txn = createSellTransaction();
        txn.setQuantity(new BigDecimal("-100.0000"));
        txn.setPrice(new BigDecimal("200.0000"));
        txn.setAmount(new BigDecimal("-20000.00"));

        Position existing = Position.builder()
                .portfolioId("PORT1234")
                .investmentId("AAPL000001")
                .quantity(new BigDecimal("200.0000"))
                .costBasis(new BigDecimal("30000.00"))
                .marketValue(new BigDecimal("30000.00"))
                .status("A")
                .realizedGainLoss(BigDecimal.ZERO)
                .build();

        when(positionRepository.findByPortfolioIdAndInvestmentId("PORT1234", "AAPL000001"))
                .thenReturn(Optional.of(existing));
        when(positionRepository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(txn);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position saved = captor.getValue();
        // Sold 100 at 200 = 20000, cost basis for 100 shares = 15000
        // Realized gain = 20000 - 15000 = 5000
        assertThat(saved.getRealizedGainLoss()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void shouldReduceCashBalanceOnFee() throws Exception {
        TransactionRecord txn = TransactionRecord.builder()
                .id(1L)
                .portfolioId("PORT1234")
                .investmentId("AAPL000001")
                .trnType(TransactionType.FE)
                .quantity(BigDecimal.ZERO)
                .price(BigDecimal.ZERO)
                .amount(new BigDecimal("50.00"))
                .status("DONE")
                .build();

        Portfolio portfolio = Portfolio.builder()
                .portfolioId("PORT1234")
                .accountNo("1234567890")
                .status("A")
                .cashBalance(new BigDecimal("10000.00"))
                .build();

        when(portfolioRepository.findById("PORT1234")).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(txn);

        ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioRepository).save(captor.capture());
        assertThat(captor.getValue().getCashBalance()).isEqualByComparingTo(new BigDecimal("9950.00"));
    }

    @Test
    void shouldHandleBigDecimalPrecisionInPositionUpdate() throws Exception {
        TransactionRecord txn = createBuyTransaction();
        txn.setQuantity(new BigDecimal("33.3333"));
        txn.setPrice(new BigDecimal("99.9999"));
        txn.setAmount(new BigDecimal("3333.30"));

        when(positionRepository.findByPortfolioIdAndInvestmentId("PORT1234", "AAPL000001"))
                .thenReturn(Optional.empty());
        when(positionRepository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.process(txn);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("33.3333"));
        assertThat(saved.getCostBasis()).isEqualByComparingTo(new BigDecimal("3333.30"));
    }

    private TransactionRecord createBuyTransaction() {
        return TransactionRecord.builder()
                .id(1L)
                .trnDate("20240315")
                .trnTime("143025")
                .portfolioId("PORT1234")
                .sequenceNo("000001")
                .investmentId("AAPL000001")
                .trnType(TransactionType.BU)
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("150.0000"))
                .amount(new BigDecimal("15000.00"))
                .currency("USD")
                .status("DONE")
                .build();
    }

    private TransactionRecord createSellTransaction() {
        return TransactionRecord.builder()
                .id(1L)
                .trnDate("20240315")
                .trnTime("143025")
                .portfolioId("PORT1234")
                .sequenceNo("000001")
                .investmentId("AAPL000001")
                .trnType(TransactionType.SL)
                .quantity(new BigDecimal("-100.0000"))
                .price(new BigDecimal("150.0000"))
                .amount(new BigDecimal("-15000.00"))
                .currency("USD")
                .status("DONE")
                .build();
    }
}
