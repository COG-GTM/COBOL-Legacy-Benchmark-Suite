package com.cognition.portfolio.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionStatus;
import com.cognition.portfolio.transaction.domain.TransactionType;
import com.cognition.portfolio.transaction.repository.PortfolioTransactionRepository;
import com.cognition.portfolio.transaction.service.TransactionAmountCalculator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the representative records loaded by the {@code seed} profile.
 *
 * <p>These records are not a conversion of production data — the legacy repository contains no
 * ASCII extract of {@code TRANHIST}. They are derived from the {@code TRNREC.cpy} layout and the
 * COBOL generators {@code PORTTEST.cbl} / {@code TSTGEN00.cbl}.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:seed-test;DB_CLOSE_DELAY=-1")
@ActiveProfiles("seed")
class SeedDataTest {

  @Autowired private PortfolioTransactionRepository repository;
  @Autowired private TransactionAmountCalculator amountCalculator;

  @Test
  @DisplayName("The seed migration loads the representative records in key sequence")
  void seedRecordsLoaded() {
    List<PortfolioTransaction> transactions = repository.findAllInKeySequence();

    assertThat(transactions).hasSize(8);
    assertThat(transactions.get(0).getTrnKey().toKeyString()).isEqualTo("20240320093015PORT0001000001");
    assertThat(transactions).extracting(PortfolioTransaction::getTrnType)
        .contains(TransactionType.BUY, TransactionType.SELL, TransactionType.TRANSFER, TransactionType.FEE);
    assertThat(transactions).extracting(PortfolioTransaction::getTrnStatus)
        .contains(TransactionStatus.PENDING, TransactionStatus.DONE, TransactionStatus.FAILED,
            TransactionStatus.REVERSED);
  }

  @Test
  @DisplayName("The first seed record parses at every field with the copybook types")
  void firstSeedRecordFields() {
    PortfolioTransaction first = repository.findAllInKeySequence().get(0);

    assertThat(first.getTrnKey().getTrnDate()).isEqualTo("20240320");
    assertThat(first.getTrnKey().getTrnTime()).isEqualTo("093015");
    assertThat(first.getTrnKey().getTrnPortfolioId()).isEqualTo("PORT0001");
    assertThat(first.getTrnKey().getTrnSequenceNo()).isEqualTo("000001");
    assertThat(first.getTrnInvestmentId()).isEqualTo("AAPL000001");
    assertThat(first.getTrnType()).isEqualTo(TransactionType.BUY);
    assertThat(first.getTrnQuantity()).isEqualByComparingTo("150.0000");
    assertThat(first.getTrnPrice()).isEqualByComparingTo("187.4500");
    assertThat(first.getTrnAmount()).isEqualByComparingTo("28117.50");
    assertThat(first.getTrnCurrency()).isEqualTo("USD");
    assertThat(first.getTrnStatus()).isEqualTo(TransactionStatus.DONE);
    assertThat(first.getTrnProcessUser()).isEqualTo("BATCH001");
  }

  @Test
  @DisplayName("BR-22: every non-transfer seed record reconciles amount = quantity x price")
  void seedAmountsReconcile() {
    assertThat(repository.findAllInKeySequence())
        .filteredOn(transaction -> transaction.getTrnType() != TransactionType.TRANSFER)
        .allSatisfy(
            transaction ->
                assertThat(
                        amountCalculator.isConsistent(
                            transaction.getTrnQuantity(),
                            transaction.getTrnPrice(),
                            transaction.getTrnAmount()))
                    .isTrue());
  }
}
