package com.portfolio.repository;

import com.portfolio.domain.TransactionHistoryFileRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository over the VSAM TRANHIST migration table (input of HISTLD00). */
public interface TransactionHistoryFileRepository
        extends JpaRepository<TransactionHistoryFileRecord, TransactionHistoryFileRecord.Key> {
}
