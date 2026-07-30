package com.cognition.portfolio.transaction.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.portfolio.transaction.TestTransactions;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionKey;
import com.cognition.portfolio.transaction.domain.TransactionStatus;
import com.cognition.portfolio.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Persistence tests against the Flyway-created schema: the column types must hold the full COBOL
 * precision, and reads must come back in VSAM key sequence.
 */
@DataJpaTest
class PortfolioTransactionRepositoryTest {

  @Autowired private PortfolioTransactionRepository repository;

  @BeforeEach
  void insertRecords() {
    repository.saveAll(
        List.of(
            TestTransactions.buy(),
            TestTransactions.sell(),
            TestTransactions.fee(),
            TestTransactions.builder("20240321", "090500", "PORT0002", "000001")
                .trnInvestmentId("BND000ABCD")
                .trnCurrency("EUR")
                .trnStatus(TransactionStatus.DONE)
                .build()));
    repository.flush();
  }

  @Test
  @DisplayName("A record round trips with the full precision of PIC S9(11)V9(4) and S9(13)V9(2)")
  void roundTripKeepsCobolPrecision() {
    PortfolioTransaction stored =
        repository
            .findById(new TransactionKey("20240320", "093015", "PORT0001", "000001"))
            .orElseThrow();

    assertThat(stored.getTrnQuantity()).isEqualByComparingTo("150.0000");
    assertThat(stored.getTrnQuantity().scale()).isEqualTo(4);
    assertThat(stored.getTrnPrice().scale()).isEqualTo(4);
    assertThat(stored.getTrnAmount().scale()).isEqualTo(2);
    assertThat(stored.getTrnType()).isEqualTo(TransactionType.BUY);
    assertThat(stored.getTrnStatus()).isEqualTo(TransactionStatus.PENDING);
  }

  @Test
  @DisplayName("The 15 digit capacity of the packed decimal fields is preserved end to end")
  void storesFifteenDigitValues() {
    PortfolioTransaction large =
        TestTransactions.builder("20240322", "120000", "PORT0009", "000001")
            .trnQuantity(new BigDecimal("99999999999.9999"))
            .trnPrice(new BigDecimal("99999999999.9999"))
            .trnAmount(new BigDecimal("9999999999999.99"))
            .build();

    repository.saveAndFlush(large);

    PortfolioTransaction stored = repository.findById(large.getTrnKey()).orElseThrow();
    assertThat(stored.getTrnQuantity()).isEqualByComparingTo("99999999999.9999");
    assertThat(stored.getTrnAmount()).isEqualByComparingTo("9999999999999.99");
  }

  @Test
  @DisplayName("BR-21: sequential read returns records in VSAM key sequence")
  void sequentialReadIsInKeyOrder() {
    List<String> keys =
        repository.findAllInKeySequence().stream()
            .map(transaction -> transaction.getTrnKey().toKeyString())
            .toList();

    assertThat(keys)
        .containsExactly(
            "20240320093015PORT0001000001",
            "20240320101122PORT0001000002",
            "20240320104500PORT0001000003",
            "20240321090500PORT0002000001");
  }

  @Test
  @DisplayName("Browse filters on TRN-PORTFOLIO-ID and TRN-STATUS and pages in key order")
  void browseFiltersAndPages() {
    Page<PortfolioTransaction> firstPage =
        repository.browse("PORT0001", null, PageRequest.of(0, 2));

    assertThat(firstPage.getTotalElements()).isEqualTo(3);
    assertThat(firstPage.getContent()).hasSize(2);
    assertThat(firstPage.getContent().get(0).getTrnKey().getTrnSequenceNo()).isEqualTo("000001");

    assertThat(repository.browse(null, TransactionStatus.DONE, PageRequest.of(0, 10)).getTotalElements())
        .isEqualTo(1);
    assertThat(repository.browse(null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(4);
    assertThat(
            repository
                .browse("PORT0002", TransactionStatus.DONE, PageRequest.of(0, 10))
                .getTotalElements())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("BR-20: the highest TRN-SEQUENCE-NO per date and portfolio drives the next value")
  void maxSequenceNoPerDateAndPortfolio() {
    assertThat(repository.findMaxSequenceNo("20240320", "PORT0001")).contains("000003");
    assertThat(repository.findMaxSequenceNo("20240321", "PORT0002")).contains("000001");
    assertThat(repository.findMaxSequenceNo("20240325", "PORT0001")).isEmpty();
  }

  @Test
  @DisplayName("REWRITE and DELETE behave as the VSAM operations they replace")
  void rewriteAndDelete() {
    TransactionKey key = new TransactionKey("20240320", "104500", "PORT0001", "000003");
    PortfolioTransaction fee = repository.findById(key).orElseThrow();

    fee.setTrnStatus(TransactionStatus.DONE);
    repository.saveAndFlush(fee);
    assertThat(repository.findById(key).orElseThrow().getTrnStatus()).isEqualTo(TransactionStatus.DONE);

    repository.deleteById(key);
    repository.flush();
    assertThat(repository.findById(key)).isEmpty();
  }
}
