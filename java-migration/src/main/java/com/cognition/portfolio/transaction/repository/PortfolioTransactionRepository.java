package com.cognition.portfolio.transaction.repository;

import com.cognition.portfolio.traceability.CobolOrigin;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionKey;
import com.cognition.portfolio.transaction.domain.TransactionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  /** Paged sequential read, the online equivalent of a browse over the KSDS. */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2000-PROCESS-TRANSACTIONS", rules = {"BR-21"})
  @Query("""
      select t from PortfolioTransaction t
      where (:portfolioId is null or t.trnKey.trnPortfolioId = :portfolioId)
        and (:status is null or t.trnStatus = :status)
      order by t.trnKey.trnDate, t.trnKey.trnTime, t.trnKey.trnPortfolioId, t.trnKey.trnSequenceNo
      """)
  Page<PortfolioTransaction> browse(
      @Param("portfolioId") String portfolioId,
      @Param("status") TransactionStatus status,
      Pageable pageable);

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
