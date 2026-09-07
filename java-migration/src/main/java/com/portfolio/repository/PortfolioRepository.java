package com.portfolio.repository;
import com.portfolio.domain.Portfolio; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface PortfolioRepository extends JpaRepository<Portfolio,String> { Optional<Portfolio> findByAccountNo(String accountNo); boolean existsByAccountNo(String accountNo); }
