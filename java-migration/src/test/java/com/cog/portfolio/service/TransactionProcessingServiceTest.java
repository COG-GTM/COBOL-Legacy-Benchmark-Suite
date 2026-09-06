package com.cog.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cog.portfolio.TestData;
import com.cog.portfolio.common.PortfolioException;
import com.cog.portfolio.domain.AuditStatus;
import com.cog.portfolio.domain.Position;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionId;
import java.math.BigDecimal;
import java.time.LocalTime;
import com.cog.portfolio.domain.TransactionStatus;
import com.cog.portfolio.domain.TransactionType;
import com.cog.portfolio.repository.AuditLogRepository;
import com.cog.portfolio.repository.PortfolioRepository;
import com.cog.portfolio.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** PORTTRAN.cbl rules (PORTTEST.cbl moved here). */
@SpringBootTest
@Transactional
class TransactionProcessingServiceTest {

    @Autowired
    TransactionProcessingService service;
    @Autowired
    PortfolioRepository portfolioRepository;
    @Autowired
    PositionRepository positionRepository;
    @Autowired
    AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        portfolioRepository.save(TestData.portfolio());
    }

    @Test
    void buyOpensPositionAndAddsUnitsAndCost() {
        Position p = service.apply(TestData.transaction(TransactionType.BUY, "000001", "100", "10.5", "1050.00"));
        assertThat(p.getQuantity()).isEqualByComparingTo("100.0000");
        assertThat(p.getCostBasis()).isEqualByComparingTo("1050.00");
        assertThat(p.getQuantity().scale()).isEqualTo(4);
        assertThat(p.getCostBasis().scale()).isEqualTo(2);
    }

    @Test
    void buyOnExistingPositionAccumulates() {
        positionRepository.save(TestData.position("50", "500.00"));
        Position p = service.apply(TestData.transaction(TransactionType.BUY, "000002", "25", "12", "300.00"));
        assertThat(p.getQuantity()).isEqualByComparingTo("75");
        assertThat(p.getCostBasis()).isEqualByComparingTo("800.00");
    }

    @Test
    void sellSubtractsUnitsAndCost() {
        positionRepository.save(TestData.position("100", "1000.00"));
        Transaction trn = TestData.transaction(TransactionType.SELL, "000003", "40", "11", "440.00");
        Position p = service.apply(trn);
        assertThat(p.getQuantity()).isEqualByComparingTo("60");
        assertThat(p.getCostBasis()).isEqualByComparingTo("560.00");
        assertThat(trn.getStatus()).isEqualTo(TransactionStatus.DONE);
    }

    @Test
    void sellWithInsufficientUnitsIsRejectedBeforeAnyChange() {
        positionRepository.save(TestData.position("10", "100.00"));
        Transaction trn = TestData.transaction(TransactionType.SELL, "000004", "11", "10", "110.00");
        assertThatThrownBy(() -> service.apply(trn))
                .isInstanceOf(PortfolioException.class)
                .hasMessage(TransactionProcessingService.MSG_INSUFFICIENT_UNITS);
        assertThat(trn.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(positionRepository.findAll().get(0).getQuantity()).isEqualByComparingTo("10");
        assertThat(auditLogRepository.findAll()).anyMatch(a -> a.getStatus() == AuditStatus.FAILURE);
    }

    @Test
    void feeReducesCostBasisOnly() {
        positionRepository.save(TestData.position("10", "100.00"));
        Position p = service.apply(TestData.transaction(TransactionType.FEE, "000005", "1", "1", "2.50"));
        assertThat(p.getQuantity()).isEqualByComparingTo("10");
        assertThat(p.getCostBasis()).isEqualByComparingTo("97.50");
    }

    @Test
    void transferIsNotImplementedLikeLegacy() {
        positionRepository.save(TestData.position("10", "100.00"));
        assertThatThrownBy(() -> service.apply(
                TestData.transaction(TransactionType.TRANSFER, "000006", "1", "0", "0")))
                .isInstanceOf(PortfolioException.class)
                .hasMessage(TransactionProcessingService.MSG_TRANSFER_NOT_IMPLEMENTED);
    }

    @Test
    void validationRejectsZeroQuantityAndUnknownPortfolio() {
        assertThat(service.validate(TestData.transaction(TransactionType.BUY, "000007", "0", "1", "1")).message())
                .isEqualTo(TransactionProcessingService.MSG_QUANTITY);
        Transaction unknown = new Transaction(new TransactionId(TestData.DATE, LocalTime.NOON, "PORT9999", "000008"),
                TestData.INVESTMENT_ID, TransactionType.BUY);
        unknown.setQuantity(BigDecimal.ONE);
        assertThat(service.validate(unknown).message()).startsWith("Invalid Portfolio ID");
    }
}
