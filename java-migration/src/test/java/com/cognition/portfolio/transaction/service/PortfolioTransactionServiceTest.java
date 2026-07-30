package com.cognition.portfolio.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cognition.portfolio.transaction.TestTransactions;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionKey;
import com.cognition.portfolio.transaction.domain.TransactionStatus;
import com.cognition.portfolio.transaction.domain.TransactionType;
import com.cognition.portfolio.transaction.exception.DuplicateTransactionException;
import com.cognition.portfolio.transaction.exception.TransactionNotFoundException;
import com.cognition.portfolio.transaction.exception.TransactionProcessingException;
import com.cognition.portfolio.transaction.exception.TransactionValidationException;
import com.cognition.portfolio.transaction.repository.PortfolioTransactionRepository;
import com.cognition.portfolio.transaction.validation.TransactionValidator;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;

/** End-to-end service behaviour against the migrated schema. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:svc-test;DB_CLOSE_DELAY=-1")
class PortfolioTransactionServiceTest {

  @Autowired private PortfolioTransactionService service;
  @Autowired private TransactionSequenceService sequenceService;
  @Autowired private PortfolioTransactionRepository repository;
  @Autowired private TransactionValidator validator;
  @Autowired private TransactionPostingService postingService;
  @Autowired private PlatformTransactionManager transactionManager;

  @BeforeEach
  void clean() {
    repository.deleteAll();
  }

  @Test
  @DisplayName("WRITE then keyed READ returns the record unchanged")
  void insertAndKeyedRead() {
    PortfolioTransaction saved = service.insert(TestTransactions.buy());

    PortfolioTransaction read = service.findByKey(saved.getTrnKey());

    assertThat(read.getTrnAmount()).isEqualByComparingTo("28117.50");
    assertThat(read.getTrnType()).isEqualTo(TransactionType.BUY);
  }

  @Test
  @DisplayName("A second WRITE on the same TRN-KEY is rejected, as VSAM status 22 is")
  void duplicateKeyRejected() {
    service.insert(TestTransactions.buy());

    assertThatThrownBy(() -> service.insert(TestTransactions.buy()))
        .isInstanceOf(DuplicateTransactionException.class);
  }

  @Test
  @DisplayName("BR-07: an invalid record is rejected on WRITE with the COBOL ERR-TEXT")
  void insertValidatesFirst() {
    PortfolioTransaction invalid =
        TestTransactions.builder("20240320", "093015", "PORT0001", "000001")
            .trnQuantity(BigDecimal.ZERO)
            .build();

    assertThatThrownBy(() -> service.insert(invalid))
        .isInstanceOf(TransactionValidationException.class)
        .hasMessage("Quantity must be greater than zero");
  }

  @Test
  @DisplayName("A keyed READ on a missing record behaves like INVALID KEY")
  void missingRecord() {
    assertThatThrownBy(
            () -> service.findByKey(new TransactionKey("20991231", "235959", "PORT0001", "000001")))
        .isInstanceOf(TransactionNotFoundException.class);
  }

  @Test
  @DisplayName("REWRITE replaces TRN-DATA and TRN-AUDIT but never TRN-KEY")
  void rewriteKeepsKey() {
    PortfolioTransaction saved = service.insert(TestTransactions.buy());
    PortfolioTransaction carrier =
        TestTransactions.builder("20991231", "235959", "PORT9999", "999999")
            .trnQuantity(new BigDecimal("175.0000"))
            .trnPrice(new BigDecimal("190.0000"))
            .trnAmount(new BigDecimal("33250.00"))
            .trnProcessUser("ONLINE01")
            .build();

    PortfolioTransaction updated = service.rewrite(saved.getTrnKey(), carrier);

    assertThat(updated.getTrnKey()).isEqualTo(saved.getTrnKey());
    assertThat(updated.getTrnQuantity()).isEqualByComparingTo("175.0000");
    assertThat(updated.getTrnProcessUser()).isEqualTo("ONLINE01");
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("BR-23: P -> D is allowed, D -> P is not")
  void statusTransitions() {
    PortfolioTransaction saved = service.insert(TestTransactions.buy());

    PortfolioTransaction done =
        service.transitionStatus(saved.getTrnKey(), TransactionStatus.DONE, "ONLINE01");
    assertThat(done.getTrnStatus()).isEqualTo(TransactionStatus.DONE);
    assertThat(done.getTrnProcessUser()).isEqualTo("ONLINE01");

    assertThatThrownBy(
            () -> service.transitionStatus(saved.getTrnKey(), TransactionStatus.PENDING, "ONLINE01"))
        .isInstanceOf(TransactionProcessingException.class)
        .hasMessage("Invalid status transition: D -> P");
  }

  @Test
  @DisplayName("Processing a BU record marks it done and returns the portfolio deltas")
  void processBuy() {
    PortfolioTransaction saved = service.insert(TestTransactions.buy());

    TransactionProcessingResult result = service.process(saved.getTrnKey(), null);

    assertThat(result.isProcessed()).isTrue();
    assertThat(result.effect().unitsDelta()).isEqualByComparingTo("150.0000");
    assertThat(repository.findById(saved.getTrnKey()).orElseThrow().getTrnStatus())
        .isEqualTo(TransactionStatus.DONE);
  }

  @Test
  @DisplayName("BR-10: processing a SL record without enough units fails and persists TRN-STATUS 'F'")
  void processSellWithoutUnits() {
    PortfolioTransaction saved = service.insert(TestTransactions.sell());

    TransactionProcessingResult result = service.process(saved.getTrnKey(), new BigDecimal("10.0000"));

    assertThat(result.isProcessed()).isFalse();
    assertThat(result.errorText()).isEqualTo("Insufficient units for sale");
    assertThat(repository.findById(saved.getTrnKey()).orElseThrow().getTrnStatus())
        .isEqualTo(TransactionStatus.FAILED);
  }

  @Test
  @DisplayName("BR-23: a record that is no longer pending cannot be processed again")
  void terminalRecordsCannotBeReprocessed() {
    PortfolioTransaction saved = service.insert(TestTransactions.buy());
    service.process(saved.getTrnKey(), null);

    assertThatThrownBy(() -> service.process(saved.getTrnKey(), null))
        .isInstanceOf(TransactionProcessingException.class)
        .hasMessage("Transaction is not pending: TRN-STATUS D");

    service.transitionStatus(saved.getTrnKey(), TransactionStatus.REVERSED, "ONLINE01");
    assertThatThrownBy(() -> service.process(saved.getTrnKey(), null))
        .isInstanceOf(TransactionProcessingException.class)
        .hasMessage("Transaction is not pending: TRN-STATUS R");
  }

  @Test
  @DisplayName("OQ-2: processing SL without PORT-TOTAL-UNITS is a caller error, not a failed record")
  void sellRequiresThePortfolioPosition() {
    PortfolioTransaction saved = service.insert(TestTransactions.sell());

    assertThatThrownBy(() -> service.process(saved.getTrnKey(), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("PORT-TOTAL-UNITS must be supplied");

    assertThat(repository.findById(saved.getTrnKey()).orElseThrow().getTrnStatus())
        .isEqualTo(TransactionStatus.PENDING);
  }

  @Test
  @DisplayName("BR-13: a failed posting still carries the AUD-ACTION, with AUD-STATUS 'FAIL'")
  void failedPostingKeepsAuditAction() {
    PortfolioTransaction sell = service.insert(TestTransactions.sell());
    PortfolioTransaction transfer = service.insert(TestTransactions.transfer());

    TransactionProcessingResult sellResult = service.process(sell.getTrnKey(), BigDecimal.ZERO);
    TransactionProcessingResult transferResult = service.process(transfer.getTrnKey(), null);

    assertThat(sellResult.auditAction()).isEqualTo("DELETE");
    assertThat(sellResult.auditStatus()).isEqualTo(TransactionProcessingResult.AUDIT_FAILURE);
    assertThat(transferResult.auditAction()).isEqualTo("UPDATE");
    assertThat(transferResult.auditStatus()).isEqualTo(TransactionProcessingResult.AUDIT_FAILURE);
  }

  @Test
  @DisplayName("BR-13: a validation failure never reaches 2300, so it carries no audit entry")
  void validationFailureHasNoAuditEntry() {
    PortfolioTransaction invalid =
        TestTransactions.builder("20240320", "093015", "XXXX0001", "000001").build();

    TransactionProcessingResult result = service.processDetached(invalid, null);

    assertThat(result.errorText()).isEqualTo("Invalid Portfolio ID: XXXX0001");
    assertThat(result.auditAction()).isNull();
    assertThat(result.auditStatus()).isNull();
  }

  @Test
  @DisplayName("BR-08: the batch counts valid records as processed and invalid ones as errors")
  void batchCounters() {
    PortfolioTransaction invalid =
        TestTransactions.builder("20240320", "120000", "PORT0001", "000004")
            .trnPrice(BigDecimal.ZERO)
            .build();

    BatchRunSummary summary =
        service.runBatch(
            List.of(TestTransactions.buy(), invalid, TestTransactions.fee()));

    assertThat(summary.readCount()).isEqualTo(3);
    assertThat(summary.processCount()).isEqualTo(2);
    assertThat(summary.errorCount()).isEqualTo(1);
    assertThat(summary.abortedOnErrorLimit()).isFalse();
    // BR-21: the run reads in VSAM key sequence, so the 12:00 record comes last.
    assertThat(summary.results()).extracting(TransactionProcessingResult::errorText)
        .containsExactly(null, null, "Price must be greater than zero");
  }

  @Test
  @DisplayName("OQ-6: the batch performs no position update, because PORTTRAN never performs 2200")
  void batchDoesNotUpdatePositions() {
    BatchRunSummary summary = service.runBatch(List.of(TestTransactions.buy()));

    assertThat(summary.processCount()).isEqualTo(1);
    assertThat(summary.results().get(0).effect()).isNull();
    assertThat(summary.results().get(0).transaction().getTrnStatus())
        .isEqualTo(TransactionStatus.PENDING);
  }

  @Test
  @DisplayName("BR-11: TR passes validation but fails the position update it never reaches in COBOL")
  void transferPassesValidationButFailsProcessing() {
    PortfolioTransaction saved = service.insert(TestTransactions.transfer());

    TransactionProcessingResult result = service.process(saved.getTrnKey(), new BigDecimal("1000"));

    assertThat(result.errorText()).isEqualTo("Transfer processing not implemented");
    assertThat(result.ruleId()).isEqualTo("BR-11");
  }

  @Test
  @DisplayName("BR-08: the run stops once WS-ERROR-COUNT exceeds 100")
  void batchStopsAfterHundredErrors() {
    List<PortfolioTransaction> failing =
        java.util.stream.IntStream.rangeClosed(1, 150)
            .mapToObj(
                i ->
                    TestTransactions.builder(
                            "20240320", "093015", "PORT0001", String.format("%06d", i))
                        .trnQuantity(BigDecimal.ZERO)
                        .build())
            .toList();

    BatchRunSummary summary = service.runBatch(failing);

    assertThat(summary.errorCount()).isEqualTo(101);
    assertThat(summary.readCount()).isEqualTo(101);
    assertThat(summary.abortedOnErrorLimit()).isTrue();
  }

  @Test
  @DisplayName("BR-20: TRN-SEQUENCE-NO increments per date and portfolio, zero filled to six digits")
  void sequenceAssignment() {
    assertThat(sequenceService.nextSequenceNo("20240320", "PORT0001")).isEqualTo("000001");

    service.insert(TestTransactions.buy());
    assertThat(sequenceService.nextSequenceNo("20240320", "PORT0001")).isEqualTo("000002");

    service.insert(TestTransactions.sell());
    assertThat(sequenceService.nextSequenceNo("20240320", "PORT0001")).isEqualTo("000003");
    assertThat(sequenceService.nextSequenceNo("20240321", "PORT0001")).isEqualTo("000001");
  }

  @Test
  @DisplayName("BR-20: the insert assigns TRN-SEQUENCE-NO itself, so no caller can read a stale max")
  void insertAssignsSequenceNumber() {
    PortfolioTransaction first =
        service.insertNextInSequence(
            TestTransactions.builder("20240321", "093015", "PORT0001", null).build());
    PortfolioTransaction second =
        service.insertNextInSequence(
            TestTransactions.builder("20240321", "101122", "PORT0001", null).build());

    assertThat(first.getTrnKey().getTrnSequenceNo()).isEqualTo("000001");
    assertThat(second.getTrnKey().getTrnSequenceNo()).isEqualTo("000002");
    assertThat(repository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("BR-20: a sequence number taken by a concurrent writer is retried, not rejected")
  void insertRetriesWhenTheSequenceNumberIsTaken() {
    service.insert(TestTransactions.builder("20240322", "093015", "PORT0001", "000001").build());
    // First derivation returns a number a concurrent writer already used, as a stale read would.
    TransactionSequenceService stale =
        new TransactionSequenceService(repository) {
          private int calls;

          @Override
          public String nextSequenceNo(String trnDate, String portfolioId) {
            return ++calls == 1 ? "000001" : super.nextSequenceNo(trnDate, portfolioId);
          }
        };
    PortfolioTransactionService racing =
        new PortfolioTransactionService(
            repository, validator, postingService, stale, transactionManager);

    PortfolioTransaction saved =
        racing.insertNextInSequence(
            TestTransactions.builder("20240322", "093015", "PORT0001", null).build());

    assertThat(saved.getTrnKey().getTrnSequenceNo()).isEqualTo("000002");
    assertThat(repository.count()).isEqualTo(2);
  }
}
