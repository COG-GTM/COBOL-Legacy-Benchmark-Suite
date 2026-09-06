package com.cog.portfolio.repository;

import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionId;
import com.cog.portfolio.domain.TransactionStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Replaces VSAM TRANFILE access. */
public interface TransactionRepository extends JpaRepository<Transaction, TransactionId> {

    List<Transaction> findByStatusOrderByIdTransactionDateAscIdTransactionTimeAscIdSequenceNoAsc(
            TransactionStatus status);

    Page<Transaction> findByStatusOrderByIdTransactionDateAscIdTransactionTimeAscIdSequenceNoAsc(
            TransactionStatus status, Pageable pageable);

    Page<Transaction> findAllByOrderByIdTransactionDateAscIdTransactionTimeAscIdSequenceNoAsc(Pageable pageable);

    long countByStatus(TransactionStatus status);
}
