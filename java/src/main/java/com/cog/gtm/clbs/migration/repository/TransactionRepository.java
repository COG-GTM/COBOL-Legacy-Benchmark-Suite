package com.cog.gtm.clbs.migration.repository;

import com.cog.gtm.clbs.migration.domain.transaction.Transaction;
import com.cog.gtm.clbs.migration.domain.transaction.TransactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, TransactionId> {
}
