package com.clbs.portfolio.batch.rules;

import com.clbs.portfolio.entity.Portfolio;
import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.PortfolioRepository;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdjudicationRulesTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    @InjectMocks
    private ValidationRuleService validationRuleService;

    @InjectMocks
    private EligibilityRuleService eligibilityRuleService;

    @InjectMocks
    private DuplicateDetectionService duplicateDetectionService;

    @InjectMocks
    private CostSharingService costSharingService;

    @InjectMocks
    private CoordinationOfBenefitsService coordinationOfBenefitsService;

    // --- ValidationRuleService ---

    @Test
    void validationRule_shouldApproveValidTransaction() {
        TransactionRecord txn = createValidTransaction();
        txn.setStatus("DONE");
        assertThat(validationRuleService.apply(txn)).isEqualTo(AdjudicationResult.APPROVED);
    }

    @Test
    void validationRule_shouldDenyTransactionWithWrongStatus() {
        TransactionRecord txn = createValidTransaction();
        txn.setStatus("PENDING");
        assertThat(validationRuleService.apply(txn)).isEqualTo(AdjudicationResult.DENIED);
    }

    @Test
    void validationRule_shouldDenyTransactionWithNullPortfolioId() {
        TransactionRecord txn = createValidTransaction();
        txn.setStatus("DONE");
        txn.setPortfolioId(null);
        assertThat(validationRuleService.apply(txn)).isEqualTo(AdjudicationResult.DENIED);
    }

    // --- EligibilityRuleService ---

    @Test
    void eligibilityRule_shouldApproveActivePortfolio() {
        TransactionRecord txn = createValidTransaction();
        Portfolio portfolio = createActivePortfolio();
        when(portfolioRepository.findById("PORT1234")).thenReturn(Optional.of(portfolio));
        assertThat(eligibilityRuleService.apply(txn)).isEqualTo(AdjudicationResult.APPROVED);
    }

    @Test
    void eligibilityRule_shouldDenyClosedPortfolio() {
        TransactionRecord txn = createValidTransaction();
        Portfolio portfolio = createActivePortfolio();
        portfolio.setStatus("C");
        when(portfolioRepository.findById("PORT1234")).thenReturn(Optional.of(portfolio));
        assertThat(eligibilityRuleService.apply(txn)).isEqualTo(AdjudicationResult.DENIED);
    }

    @Test
    void eligibilityRule_shouldDenySuspendedPortfolio() {
        TransactionRecord txn = createValidTransaction();
        Portfolio portfolio = createActivePortfolio();
        portfolio.setStatus("S");
        when(portfolioRepository.findById("PORT1234")).thenReturn(Optional.of(portfolio));
        assertThat(eligibilityRuleService.apply(txn)).isEqualTo(AdjudicationResult.DENIED);
    }

    @Test
    void eligibilityRule_shouldDenyMissingPortfolio() {
        TransactionRecord txn = createValidTransaction();
        when(portfolioRepository.findById("PORT1234")).thenReturn(Optional.empty());
        assertThat(eligibilityRuleService.apply(txn)).isEqualTo(AdjudicationResult.DENIED);
    }

    // --- DuplicateDetectionService ---

    @Test
    void duplicateDetection_shouldApproveUniqueTransaction() {
        TransactionRecord txn = createValidTransaction();
        txn.setId(1L);
        when(transactionRecordRepository
                .findByPortfolioIdAndInvestmentIdAndTrnDateAndTrnTypeAndAmount(
                        any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(txn));
        assertThat(duplicateDetectionService.apply(txn)).isEqualTo(AdjudicationResult.APPROVED);
    }

    @Test
    void duplicateDetection_shouldDenyDuplicate() {
        TransactionRecord txn = createValidTransaction();
        txn.setId(1L);
        TransactionRecord dup = createValidTransaction();
        dup.setId(2L);
        when(transactionRecordRepository
                .findByPortfolioIdAndInvestmentIdAndTrnDateAndTrnTypeAndAmount(
                        any(), any(), any(), any(), any()))
                .thenReturn(List.of(txn, dup));
        assertThat(duplicateDetectionService.apply(txn)).isEqualTo(AdjudicationResult.DENIED);
    }

    // --- CostSharingService ---

    @Test
    void costSharing_shouldApproveNonSellTransaction() {
        TransactionRecord txn = createValidTransaction();
        txn.setTrnType(TransactionType.BU);
        assertThat(costSharingService.apply(txn)).isEqualTo(AdjudicationResult.APPROVED);
        assertThat(txn.getCostBasisAdjustment()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void costSharing_shouldCalculateWeightedAverageCostForSell() {
        TransactionRecord txn = createValidTransaction();
        txn.setTrnType(TransactionType.SL);
        txn.setQuantity(new BigDecimal("-50.0000"));

        Position position = Position.builder()
                .portfolioId("PORT1234")
                .investmentId("AAPL000001")
                .quantity(new BigDecimal("200.0000"))
                .costBasis(new BigDecimal("30000.00"))
                .build();

        when(positionRepository.findByPortfolioIdAndInvestmentId("PORT1234", "AAPL000001"))
                .thenReturn(Optional.of(position));

        assertThat(costSharingService.apply(txn)).isEqualTo(AdjudicationResult.APPROVED);
        // weighted avg cost = 30000/200 = 150, adjustment = 150*50 = 7500
        assertThat(txn.getCostBasisAdjustment()).isEqualByComparingTo(new BigDecimal("7500.00"));
    }

    @Test
    void costSharing_shouldDenyWhenSellExceedsPosition() {
        TransactionRecord txn = createValidTransaction();
        txn.setTrnType(TransactionType.SL);
        txn.setQuantity(new BigDecimal("-300.0000"));

        Position position = Position.builder()
                .portfolioId("PORT1234")
                .investmentId("AAPL000001")
                .quantity(new BigDecimal("200.0000"))
                .costBasis(new BigDecimal("30000.00"))
                .build();

        when(positionRepository.findByPortfolioIdAndInvestmentId("PORT1234", "AAPL000001"))
                .thenReturn(Optional.of(position));

        assertThat(costSharingService.apply(txn)).isEqualTo(AdjudicationResult.DENIED);
    }

    // --- CoordinationOfBenefitsService ---

    @Test
    void cob_shouldApproveNonTransferTransaction() {
        TransactionRecord txn = createValidTransaction();
        txn.setTrnType(TransactionType.BU);
        assertThat(coordinationOfBenefitsService.apply(txn)).isEqualTo(AdjudicationResult.APPROVED);
    }

    @Test
    void cob_shouldApproveValidTransfer() {
        TransactionRecord txn = createValidTransaction();
        txn.setTrnType(TransactionType.TR);
        txn.setInvestmentId("PORT5678AB");

        Portfolio source = createActivePortfolio();
        Portfolio dest = Portfolio.builder()
                .portfolioId("PORT5678")
                .accountNo("1234567890")
                .status("A")
                .build();

        when(portfolioRepository.findById("PORT1234")).thenReturn(Optional.of(source));
        when(portfolioRepository.findById("PORT5678")).thenReturn(Optional.of(dest));

        assertThat(coordinationOfBenefitsService.apply(txn)).isEqualTo(AdjudicationResult.APPROVED);
    }

    @Test
    void cob_shouldDenyTransferWithInactiveDestination() {
        TransactionRecord txn = createValidTransaction();
        txn.setTrnType(TransactionType.TR);
        txn.setInvestmentId("PORT5678AB");

        Portfolio source = createActivePortfolio();
        Portfolio dest = Portfolio.builder()
                .portfolioId("PORT5678")
                .accountNo("1234567890")
                .status("C")
                .build();

        when(portfolioRepository.findById("PORT1234")).thenReturn(Optional.of(source));
        when(portfolioRepository.findById("PORT5678")).thenReturn(Optional.of(dest));

        assertThat(coordinationOfBenefitsService.apply(txn)).isEqualTo(AdjudicationResult.DENIED);
    }

    // --- PaymentDeterminationService ---

    @Test
    void paymentDetermination_shouldCalculateSettlement() {
        PaymentDeterminationService pds = new PaymentDeterminationService();
        TransactionRecord txn = createValidTransaction();
        txn.setFeeAmount(new BigDecimal("50.00"));
        txn.setCostBasisAdjustment(new BigDecimal("0.00"));

        AdjudicationResult result = pds.apply(txn);
        assertThat(result).isEqualTo(AdjudicationResult.APPROVED);
        // settlement = 15000 + 50 - 0 = 15050
        assertThat(txn.getSettlementAmount()).isEqualByComparingTo(new BigDecimal("15050.00"));
    }

    // --- Helpers ---

    private TransactionRecord createValidTransaction() {
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
                .status("PENDING")
                .build();
    }

    private Portfolio createActivePortfolio() {
        return Portfolio.builder()
                .portfolioId("PORT1234")
                .accountNo("1234567890")
                .clientName("Test Client")
                .clientType("I")
                .status("A")
                .totalValue(new BigDecimal("100000.00"))
                .cashBalance(new BigDecimal("50000.00"))
                .build();
    }
}
