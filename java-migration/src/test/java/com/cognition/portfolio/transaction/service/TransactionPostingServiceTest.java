package com.cognition.portfolio.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cognition.portfolio.transaction.TestTransactions;
import com.cognition.portfolio.transaction.domain.PortfolioPostingEffect;
import com.cognition.portfolio.transaction.exception.TransactionProcessingException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Asserts the position update rules of {@code PORTTRAN 2200-UPDATE-POSITIONS}. */
class TransactionPostingServiceTest {

  private final TransactionPostingService postingService = new TransactionPostingService();

  @Test
  @DisplayName("BR-09 2210-PROCESS-BUY: units and cost both increase")
  void buyAddsUnitsAndCost() {
    PortfolioPostingEffect effect = postingService.processBuy(TestTransactions.buy());

    assertThat(effect.unitsDelta()).isEqualByComparingTo("150.0000");
    assertThat(effect.costDelta()).isEqualByComparingTo("28117.50");
    assertThat(effect.auditAction()).isEqualTo("CREATE");
  }

  @Test
  @DisplayName("BR-10 2220-PROCESS-SELL: units and cost decrease when enough units are held")
  void sellSubtractsUnitsAndCost() {
    PortfolioPostingEffect effect =
        postingService.processSell(TestTransactions.sell(), new BigDecimal("500.0000"));

    assertThat(effect.unitsDelta()).isEqualByComparingTo("-50.0000");
    assertThat(effect.costDelta()).isEqualByComparingTo("-9560.00");
    assertThat(effect.auditAction()).isEqualTo("DELETE");
  }

  @Test
  @DisplayName("BR-10 2220-PROCESS-SELL: 'Insufficient units for sale' when PORT-TOTAL-UNITS < TRN-QUANTITY")
  void sellRejectedWhenUnitsInsufficient() {
    assertThatThrownBy(() -> postingService.processSell(TestTransactions.sell(), new BigDecimal("49.9999")))
        .isInstanceOf(TransactionProcessingException.class)
        .hasMessage("Insufficient units for sale");
  }

  @Test
  @DisplayName("BR-10 2220-PROCESS-SELL: selling exactly the held units is allowed")
  void sellExactlyAvailableUnits() {
    assertThat(postingService.processSell(TestTransactions.sell(), new BigDecimal("50.0000")).unitsDelta())
        .isEqualByComparingTo("-50.0000");
  }

  @Test
  @DisplayName("BR-11 2230-PROCESS-TRANSFER: TR is not implemented in the legacy program and still fails")
  void transferNotImplemented() {
    assertThatThrownBy(() -> postingService.processTransfer(TestTransactions.transfer()))
        .isInstanceOf(TransactionProcessingException.class)
        .hasMessage("Transfer processing not implemented");
  }

  @Test
  @DisplayName("BR-12 2240-PROCESS-FEE: cost decreases, units unchanged")
  void feeSubtractsCostOnly() {
    PortfolioPostingEffect effect = postingService.processFee(TestTransactions.fee());

    assertThat(effect.unitsDelta()).isEqualByComparingTo("0");
    assertThat(effect.costDelta()).isEqualByComparingTo("-125.00");
    assertThat(effect.auditAction()).isEqualTo("UPDATE");
  }

  @Test
  @DisplayName("BR-13 2300-UPDATE-AUDIT-TRAIL: audit action per TRN-TYPE")
  void auditActionMapping() {
    assertThat(postingService.updatePositions(TestTransactions.buy(), null).auditAction()).isEqualTo("CREATE");
    assertThat(postingService.updatePositions(TestTransactions.sell(), new BigDecimal("100")).auditAction())
        .isEqualTo("DELETE");
    assertThat(postingService.updatePositions(TestTransactions.fee(), null).auditAction()).isEqualTo("UPDATE");
  }
}
