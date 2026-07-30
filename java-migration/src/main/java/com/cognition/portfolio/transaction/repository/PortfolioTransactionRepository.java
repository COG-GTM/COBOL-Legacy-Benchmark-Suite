package com.cognition.portfolio.transaction.repository;

import com.cognition.portfolio.traceability.CobolOrigin;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionKey;
import com.cognition.portfolio.transaction.domain.TransactionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Data access for {@code TRANHIST}, replacing the VSAM file control operations of
 * {@code PORTTRAN.cbl}.
 */
@Repository
@CobolOrigin(program = "PORTTRAN", paragraph = "SELECT TRANSACTION-FILE")
public interface PortfolioTransactionRepository
    extends JpaRepository<PortfolioTransaction, TransactionKey> {

  /**
   * Sequential read in VSAM key order — the access path of
   * {@code PORTTRAN 2000-PROCESS-TRANSACTIONS}.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2000-PROCESS-TRANSACTIONS", rules = {"BR-21"})
  @Query("""
      select t from PortfolioTransaction t
      order by t.trnKey.trnDate, t.trnKey.trnTime, t.trnKey.trnPortfolioId, t.trnKey.trnSequenceNo
      """)
  List<PortfolioTransaction> findAllInKeySequence();

  /** BR-21 — the key sequence of the {@code TRANHIST} KSDS. */
  Sort KEY_SEQUENCE =
      Sort.by(
          "trnKey.trnDate", "trnKey.trnTime", "trnKey.trnPortfolioId", "trnKey.trnSequenceNo");

  /**
   * Paged sequential read, the online equivalent of a browse over the KSDS.
   *
   * <p>Each filter combination is a separate query rather than one query with
   * {@code :param is null} guards: an untyped null parameter compared against a converted enum
   * column is only resolvable on some databases, so the guarded form works on H2 but can fail on
   * PostgreSQL.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2000-PROCESS-TRANSACTIONS", rules = {"BR-21"})
  default Page<PortfolioTransaction> browse(
      String portfolioId, TransactionStatus status, Pageable pageable) {
    Pageable inKeySequence =
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), KEY_SEQUENCE);
    if (portfolioId == null && status == null) {
      return findAllBy(inKeySequence);
    }
    if (status == null) {
      return findByTrnKeyTrnPortfolioId(portfolioId, inKeySequence);
    }
    if (portfolioId == null) {
      return findByTrnStatus(status, inKeySequence);
    }
    return findByTrnKeyTrnPortfolioIdAndTrnStatus(portfolioId, status, inKeySequence);
  }

  /** Unfiltered browse. */
  Page<PortfolioTransaction> findAllBy(Pageable pageable);

  /** Browse restricted to one {@code TRN-PORTFOLIO-ID}. */
  Page<PortfolioTransaction> findByTrnKeyTrnPortfolioId(String portfolioId, Pageable pageable);

  /** Browse restricted to one {@code TRN-STATUS}. */
  Page<PortfolioTransaction> findByTrnStatus(TransactionStatus status, Pageable pageable);

  /** Browse restricted to one {@code TRN-PORTFOLIO-ID} and {@code TRN-STATUS}. */
  Page<PortfolioTransaction> findByTrnKeyTrnPortfolioIdAndTrnStatus(
      String portfolioId, TransactionStatus status, Pageable pageable);

  /**
   * Highest {@code TRN-SEQUENCE-NO} already used for a date and portfolio; the input to the
   * sequence assignment of {@code PRCSEQ00 1210-ADD-TO-SEQUENCE}.
   */
  @CobolOrigin(program = "PRCSEQ00", paragraph = "1210-ADD-TO-SEQUENCE", rules = {"BR-20"})
  @Query("""
      select max(t.trnKey.trnSequenceNo) from PortfolioTransaction t
      where t.trnKey.trnDate = :trnDate and t.trnKey.trnPortfolioId = :portfolioId
      """)
  Optional<String> findMaxSequenceNo(
      @Param("trnDate") String trnDate, @Param("portfolioId") String portfolioId);
}
