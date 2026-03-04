package com.cobolbenchmark.db;

import com.cobolbenchmark.model.PoshistKey;
import com.cobolbenchmark.model.PoshistRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Position History Repository - replaces DB2 POSHIST table operations.
 * Used by HISTLD00 batch job and INQHIST online inquiry.
 */
@Repository
public interface PoshistRepository extends JpaRepository<PoshistRecord, PoshistKey> {

    @Query("SELECT p FROM PoshistRecord p WHERE p.portfolioId = :portfolioId ORDER BY p.transDate DESC, p.transTime DESC")
    List<PoshistRecord> findByPortfolioIdOrderByDateDesc(@Param("portfolioId") String portfolioId);

    @Query("SELECT p FROM PoshistRecord p WHERE p.accountNo = :accountNo ORDER BY p.transDate DESC")
    List<PoshistRecord> findByAccountNo(@Param("accountNo") String accountNo);
}
