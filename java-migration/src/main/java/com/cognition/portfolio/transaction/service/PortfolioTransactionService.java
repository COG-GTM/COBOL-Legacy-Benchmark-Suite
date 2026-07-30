package com.cognition.portfolio.transaction.service;

import com.cognition.portfolio.traceability.CobolOrigin;
import com.cognition.portfolio.transaction.domain.PortfolioPostingEffect;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionKey;
import com.cognition.portfolio.transaction.domain.TransactionStatus;
import com.cognition.portfolio.transaction.exception.DuplicateTransactionException;
import com.cognition.portfolio.transaction.exception.TransactionNotFoundException;
import com.cognition.portfolio.transaction.exception.TransactionProcessingException;
import com.cognition.portfolio.transaction.exception.TransactionValidationException;
import com.cognition.portfolio.transaction.repository.PortfolioTransactionRepository;
import com.cognition.portfolio.transaction.validation.TransactionValidator;
import com.cognition.portfolio.transaction.validation.ValidationOutcome;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the portfolio transaction entity. Each method states the COBOL file
 * operation or paragraph it replaces.
 */
@Service
@CobolOrigin(program = "PORTTRAN", paragraph = "0000-MAIN")
public class PortfolioTransactionService {

  /** {@code PERFORM 2000-PROCESS-TRANSACTIONS UNTIL ... WS-ERROR-COUNT > 100}. */
  public static final int MAX_ERRORS = 100;

  private final PortfolioTransactionRepository repository;
  private final TransactionValidator validator;
  private final TransactionPostingService postingService;
  private final TransactionSequenceService sequenceService;

  public PortfolioTransactionService(
      PortfolioTransactionRepository repository,
      TransactionValidator validator,
      TransactionPostingService postingService,
      TransactionSequenceService sequenceService) {
    this.repository = repository;
    this.validator = validator;
    this.postingService = postingService;
    this.sequenceService = sequenceService;
  }

  /** Keyed read: {@code READ TRANSACTION-FILE KEY IS TRN-KEY} / CICS {@code READ DATASET}. */
  @Transactional(readOnly = true)
  @CobolOrigin(program = "PORTTRAN", paragraph = "2110-CHECK-PORTFOLIO", rules = {"BR-21"})
  public PortfolioTransaction findByKey(TransactionKey key) {
    return repository.findById(key).orElseThrow(() -> new TransactionNotFoundException(key.toKeyString()));
  }

  /** Sequential/paged read in VSAM key order: {@code 2000-PROCESS-TRANSACTIONS}. */
  @Transactional(readOnly = true)
  @CobolOrigin(program = "PORTTRAN", paragraph = "2000-PROCESS-TRANSACTIONS", rules = {"BR-21"})
  public Page<PortfolioTransaction> browse(String portfolioId, TransactionStatus status, Pageable pageable) {
    return repository.browse(portfolioId, status, pageable);
  }

  /**
   * Insert: {@code WRITE TRANSACTION-RECORD}. The record is validated first
   * ({@code 2100-VALIDATE-TRANSACTION}) and a duplicate key is rejected the way VSAM file status
   * {@code 22} is handled.
   *
   * <p>When {@code TRN-SEQUENCE-NO} is absent the next value is assigned per BR-20.
   */
  @Transactional
  @CobolOrigin(program = "PORTTRAN", paragraph = "2100-VALIDATE-TRANSACTION", rules = {"BR-07", "BR-20"})
  public PortfolioTransaction insert(PortfolioTransaction transaction) {
    assertValid(transaction);
    if (repository.existsById(transaction.getTrnKey())) {
      throw new DuplicateTransactionException(transaction.getTrnKey().toKeyString());
    }
    return repository.save(transaction);
  }

  /**
   * Update: {@code REWRITE TRANSACTION-RECORD}. The key is immutable (it is the VSAM key), so only
   * the data and audit groups are replaced.
   */
  @Transactional
  @CobolOrigin(program = "PORTMSTR", paragraph = "4000-UPDATE-PORTFOLIO", rules = {"BR-07"})
  public PortfolioTransaction rewrite(TransactionKey key, PortfolioTransaction updated) {
    PortfolioTransaction existing = findByKey(key);
    existing.setTrnInvestmentId(updated.getTrnInvestmentId());
    existing.setTrnType(updated.getTrnType());
    existing.setTrnQuantity(updated.getTrnQuantity());
    existing.setTrnPrice(updated.getTrnPrice());
    existing.setTrnAmount(updated.getTrnAmount());
    existing.setTrnCurrency(updated.getTrnCurrency());
    existing.setTrnProcessDate(updated.getTrnProcessDate());
    existing.setTrnProcessUser(updated.getTrnProcessUser());
    assertValid(existing);
    return repository.save(existing);
  }

  /**
   * BR-23 — status transition. The legacy programs never assign {@code TRN-STATUS}; the permitted
   * transitions are derived from how {@code 2100-VALIDATE-TRANSACTION} classifies a record. See
   * open question OQ-5.
   */
  @Transactional
  @CobolOrigin(program = "TRNREC", paragraph = "TRN-STATUS 88-levels", rules = {"BR-23"}, derived = true)
  public PortfolioTransaction transitionStatus(
      TransactionKey key, TransactionStatus target, String processUser) {
    PortfolioTransaction transaction = findByKey(key);
    TransactionStatus current = transaction.getTrnStatus();
    if (!current.canTransitionTo(target)) {
      throw new TransactionProcessingException(
          "Invalid status transition: " + current.getCode() + " -> " + target.getCode(),
          "BR-23",
          "TRNREC TRN-STATUS 88-levels");
    }
    transaction.setTrnStatus(target);
    if (processUser != null) {
      transaction.setTrnProcessUser(processUser);
    }
    return repository.save(transaction);
  }

  /**
   * Processes one stored transaction through {@code 2100-VALIDATE-TRANSACTION} and then
   * {@code 2200-UPDATE-POSITIONS}: validate, apply the type-specific position update, then mark the
   * record {@code D} (done) or {@code F} (failed).
   *
   * <p>Note that in PORTTRAN {@code 2200-UPDATE-POSITIONS} is unreachable — no paragraph performs
   * it (open question OQ-6). This method wires validation to the position update, which is the
   * evident intent of the program; {@link #runBatch} keeps the literal, validation-only flow.
   *
   * @param availableUnits current {@code PORT-TOTAL-UNITS}, required for {@code SL} (BR-10)
   */
  @Transactional
  @CobolOrigin(program = "PORTTRAN", paragraph = "2200-UPDATE-POSITIONS",
      rules = {"BR-07", "BR-09", "BR-10", "BR-11", "BR-12", "BR-23"})
  public TransactionProcessingResult process(TransactionKey key, BigDecimal availableUnits) {
    PortfolioTransaction transaction = findByKey(key);
    TransactionProcessingResult result = processDetached(transaction, availableUnits);
    repository.save(result.transaction());
    return result;
  }

  /**
   * BR-08/BR-14 — literal port of the batch driver {@code 0000-MAIN}:
   * {@code PERFORM 2000-PROCESS-TRANSACTIONS UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100}, where each
   * record is validated by {@code 2100-VALIDATE-TRANSACTION} and only the counters are updated.
   *
   * <p>Two properties of the COBOL are reproduced deliberately:
   *
   * <ul>
   *   <li>no position update happens, because {@code 2200-UPDATE-POSITIONS} is never performed
   *       (OQ-6);
   *   <li>nothing is written back, because {@code TRANSACTION-FILE} is opened {@code INPUT}.
   * </ul>
   *
   * <p>The run stops as soon as the error counter exceeds 100, leaving the remaining records unread.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "0000-MAIN", rules = {"BR-07", "BR-08", "BR-14"})
  public BatchRunSummary runBatch(List<PortfolioTransaction> transactions) {
    List<TransactionProcessingResult> results = new ArrayList<>();
    int readCount = 0;
    int processCount = 0;
    int errorCount = 0;
    boolean aborted = false;

    for (PortfolioTransaction transaction : sequenceService.inKeySequence(transactions)) {
      if (errorCount > MAX_ERRORS) {
        aborted = true;
        break;
      }
      readCount++;
      ValidationOutcome outcome = validator.validate(transaction);
      if (outcome.isValid()) {
        processCount++;
        results.add(
            new TransactionProcessingResult(
                transaction, null, null, null, "PORTTRAN 2100-VALIDATE-TRANSACTION"));
      } else {
        errorCount++;
        results.add(
            new TransactionProcessingResult(
                transaction, null, outcome.message(), outcome.ruleId(), outcome.cobolParagraph()));
      }
    }
    return new BatchRunSummary(readCount, processCount, errorCount, aborted, results);
  }

  /** Same logic as {@link #process} but on an in-memory record that is not persisted. */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2200-UPDATE-POSITIONS",
      rules = {"BR-07", "BR-09", "BR-10", "BR-11", "BR-12"})
  public TransactionProcessingResult processDetached(
      PortfolioTransaction transaction, BigDecimal availableUnits) {
    ValidationOutcome outcome = validator.validate(transaction);
    if (!outcome.isValid()) {
      return fail(transaction, outcome.message(), outcome.ruleId(), outcome.cobolParagraph());
    }
    try {
      PortfolioPostingEffect effect = postingService.updatePositions(transaction, availableUnits);
      transaction.setTrnStatus(TransactionStatus.DONE);
      return new TransactionProcessingResult(transaction, effect, null, null, "PORTTRAN 2200-UPDATE-POSITIONS");
    } catch (TransactionProcessingException e) {
      return fail(transaction, e.getMessage(), e.getRuleId(), e.getCobolParagraph());
    }
  }

  private TransactionProcessingResult fail(
      PortfolioTransaction transaction, String errorText, String ruleId, String paragraph) {
    transaction.setTrnStatus(TransactionStatus.FAILED);
    return new TransactionProcessingResult(transaction, null, errorText, ruleId, paragraph);
  }

  private void assertValid(PortfolioTransaction transaction) {
    ValidationOutcome outcome = validator.validate(transaction);
    if (!outcome.isValid()) {
      throw new TransactionValidationException(outcome);
    }
  }
}
