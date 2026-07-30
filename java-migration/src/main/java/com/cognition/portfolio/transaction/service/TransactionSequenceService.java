package com.cognition.portfolio.transaction.service;

import com.cognition.portfolio.traceability.CobolOrigin;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionKey;
import com.cognition.portfolio.transaction.repository.PortfolioTransactionRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sequence assignment and key ordering.
 *
 * <p>{@code PRCSEQ00 1200-BUILD-SEQUENCE} / {@code 1210-ADD-TO-SEQUENCE} number the items of a
 * processing run 1, 2, 3 ... in the order the file is read; the same convention fills
 * {@code TRN-SEQUENCE-NO PIC X(06)} for multiple transactions on the same date and portfolio
 * (TRNREC field description: "SEQUENCE NUMBER FOR MULTIPLE TRANS").
 */
@Service
@CobolOrigin(program = "PRCSEQ00", paragraph = "1200-BUILD-SEQUENCE")
public class TransactionSequenceService {

  /** Width of {@code TRN-SEQUENCE-NO PIC X(06)}. */
  public static final int SEQUENCE_WIDTH = 6;

  private static final int MAX_SEQUENCE = 999_999;

  private final PortfolioTransactionRepository repository;

  public TransactionSequenceService(PortfolioTransactionRepository repository) {
    this.repository = repository;
  }

  /**
   * BR-20 — next {@code TRN-SEQUENCE-NO} for a date and portfolio: highest existing value plus one,
   * starting at {@code 000001}, zero filled to six characters.
   */
  @Transactional(readOnly = true)
  @CobolOrigin(program = "PRCSEQ00", paragraph = "1210-ADD-TO-SEQUENCE", rules = {"BR-20"})
  public String nextSequenceNo(String trnDate, String portfolioId) {
    int next =
        repository
                .findMaxSequenceNo(trnDate, portfolioId)
                .map(Integer::parseInt)
                .orElse(0)
            + 1;
    return format(next);
  }

  /** Formats a sequence number into {@code TRN-SEQUENCE-NO PIC X(06)}. */
  @CobolOrigin(program = "TRNREC", paragraph = "TRN-SEQUENCE-NO", rules = {"BR-20"})
  public String format(int sequence) {
    if (sequence < 1 || sequence > MAX_SEQUENCE) {
      throw new IllegalArgumentException(
          "TRN-SEQUENCE-NO PIC X(06) holds 000001-999999, got " + sequence);
    }
    return String.format("%0" + SEQUENCE_WIDTH + "d", sequence);
  }

  /**
   * BR-21 — orders records the way the {@code TRANHIST} KSDS does: date, time, portfolio id,
   * sequence number.
   */
  @CobolOrigin(program = "TRNREC", paragraph = "05 TRN-KEY", rules = {"BR-21"})
  public List<PortfolioTransaction> inKeySequence(List<PortfolioTransaction> transactions) {
    return transactions.stream()
        .sorted(Comparator.comparing(PortfolioTransaction::getTrnKey, TransactionKey::compareTo))
        .toList();
  }
}
