package com.portfolio.repository;
import com.portfolio.domain.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface TransactionRepository extends JpaRepository<Transaction,String> {
 List<Transaction> findByStatusOrderByTransactionDateAscTransactionTimeAsc(TransactionStatus status);
 List<Transaction> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId);
}
