package com.portfolio.repository;
import com.portfolio.domain.AuthEntry; import org.springframework.data.jpa.repository.JpaRepository;
public interface AuthEntryRepository extends JpaRepository<AuthEntry,Long> { long countByUserIdAndResourceAndAccessType(String userId,String resource,String accessType); }
