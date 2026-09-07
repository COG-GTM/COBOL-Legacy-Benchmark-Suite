package com.portfolio.repository;
import com.portfolio.domain.ReturnCodeRecord; import org.springframework.data.jpa.repository.JpaRepository;
public interface ReturnCodeRepository extends JpaRepository<ReturnCodeRecord,Long> {}
